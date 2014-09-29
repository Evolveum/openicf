using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Text;

namespace Org.IdentityConnectors.Exchange
{
    class MiscHelper
    {
        // tracing (using ExchangeConnector trace source)
        private static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private const int CAT_DEFAULT = 1;      // default tracing event category


        /// <summary>
        /// Invokes command in PowerShell runspace, this method is just helper
        /// method to do the exception localization
        /// </summary>
        /// <param name="cmd">Command to execute</param>
        /// <returns>Collection of <see cref="PSObject"/> returned from runspace</returns>
        /// <exception cref="ConnectorException">If some troubles with command execution, 
        /// the exception will be partially localized</exception>
        internal ICollection<PSObject> InvokePipeline(ExchangeConnector exchangeConnector, Command cmd) {
            return exchangeConnector.PowerShellSupport.InvokePipeline(cmd);
        }

        internal Uid InvokePipelineAndGetGuid(ExchangeConnector exchangeConnector, Command cmd) {
            ICollection<PSObject> objects = InvokePipeline(exchangeConnector, cmd);
            if (objects.Count() != 1) {
                throw new ConnectorException("Expected one object returned from 'create' operation, got " + objects.Count() + " instead");
            }
            PSObject psobject = objects.ElementAt(0);
            if (psobject == null) {
                throw new ConnectorException("Got 'null' object from 'create' operation");
            }
            PSPropertyInfo guidPropertyInfo = psobject.Properties["guid"];          // TODO catch exception
            if (guidPropertyInfo == null || guidPropertyInfo.Value == null) {
                throw new ConnectorException("No 'guid' property on object from 'create' operation");
            }
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "GUID value = {0}", guidPropertyInfo.Value);
            return new Uid(guidPropertyInfo.Value.ToString());
        }

        internal ConnectorObject CreateConnectorObject(ExchangeConnector connector, PSObject psobject, ObjectClass objectClass) {
            ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
            
            string guid = (string)psobject.Properties["guid"].Value.ToString();
            string name = (string)psobject.Properties["name"].Value;

            builder.SetUid(new Uid(guid));
            builder.SetName(new Name(name));

            ObjectClassInfo ocinfo = connector.GetSchema().FindObjectClassInfo(objectClass.GetObjectClassValue());

            IDictionary<string,PSPropertyInfo> properties = psobject.Properties.ToDictionary(psinfo => psinfo.Name);

            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Building connector object with UID = {0} and Name = {1}", guid, name);
            foreach (ConnectorAttributeInfo cai in ocinfo.ConnectorAttributeInfos) {
                if (cai.IsReadable && properties.ContainsKey(cai.Name)) {
                    object value = properties[cai.Name].Value;
                    LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, " - attribute {0} = {1}", cai.Name, value);

                    if (value is PSObject) {
                        var ps = value as PSObject;
                        value = ps.BaseObject;
                        LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, " - attribute {0} UNWRAPPED = {1} ({2})", cai.Name, value, value.GetType());
                    }
                    builder.AddAttribute(cai.Name, CommonUtils.ConvertToSupportedForm(cai, value));
                }
            }
            return builder.Build();
        }

        public ICollection<ConnectorAttribute> DetermineNewAttributeValues(UpdateOpContext context, string query)
        {
            ConnectorObject originalObject;
            if (context.UpdateType != UpdateType.REPLACE)
            {
                originalObject = GetCurrentObject(context, query);
            }
            else
            {
                originalObject = null;          // not necessary here
            }
            return DetermineNewAttributeValues(context, originalObject);
        }

        // creates a collection of attributes that correspond to the original ones, but resolves ADD/DELETE using existing values of psuser
        public ICollection<ConnectorAttribute> DetermineNewAttributeValues(UpdateOpContext context, ConnectorObject originalObject)
        {
            if (context.UpdateType == UpdateType.REPLACE)
            {
                // TODO check multivaluedness and updateability (as below)
                return new List<ConnectorAttribute>(context.Attributes);
            }
            else
            {
                Boolean add;
                if (context.UpdateType == UpdateType.ADD)
                {
                    add = true;
                }
                else if (context.UpdateType == UpdateType.DELETE)
                {
                    add = false;
                }
                else
                {
                    throw new ArgumentException("Unsupported update type: " + context.UpdateType);
                }

                Schema schema = null;
                ICollection<ConnectorAttribute> rv = new List<ConnectorAttribute>(context.Attributes.Count);
                foreach (ConnectorAttribute attribute in context.Attributes)
                {
                    ConnectorAttribute originalAttribute = originalObject.GetAttributeByName(attribute.Name);
                    IList<object> newValues = originalAttribute != null && originalAttribute.Value != null ? new List<object>(originalAttribute.Value) : new List<object>();
                    Boolean changed = false;
                    if (attribute.Value != null)
                    {
                        foreach (object item in attribute.Value)
                        {
                            if (add)
                            {
                                if (newValues.Contains(item))
                                {
                                    LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "Trying to add value from " + attribute.Name + " that is already there: " + item);
                                }
                                else
                                {
                                    newValues.Add(item);
                                    changed = true;
                                }
                            }
                            else
                            {
                                if (!newValues.Contains(item))
                                {
                                    LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "Trying to remove value from " + attribute.Name + " that is not there: " + item);
                                }
                                else
                                {
                                    newValues.Remove(item);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (changed)
                    {
                        ConnectorAttributeBuilder b = new ConnectorAttributeBuilder();
                        b.Name = attribute.Name;
                        b.AddValue(newValues);
                        ConnectorAttribute modified = b.Build();

                        if (schema == null)
                        {
                            ExchangeConnector connector = (ExchangeConnector)context.Connector;
                            schema = connector.Schema();
                        }
                        ObjectClassInfo oci = schema.FindObjectClassInfo(context.ObjectClass.Type);
                        if (oci == null)
                        {
                            throw new InvalidOperationException("No object class info for " + context.ObjectClass.Type + " in the schema");
                        }
                        var cai = ConnectorAttributeInfoUtil.Find(attribute.Name, oci.ConnectorAttributeInfos);
                        if (cai == null)
                        {
                            throw new InvalidOperationException("No connector attribute info for " + context.ObjectClass.Type + " in the schema");
                        }

                        if (!cai.IsUpdateable)
                        {
                            throw new ConnectorSecurityException("Attempt to update a non-updateable attribute (" + attribute.Name + "): " +
                                CollectionUtil.Dump(newValues));
                        }

                        if (newValues.Count > 1 && !cai.IsMultiValued)
                        {
                            throw new InvalidAttributeValueException("More than one value in a single-valued attribute (" + attribute.Name + "): " +
                                    CollectionUtil.Dump(newValues));
                        }
                        rv.Add(modified);
                    }
                }
                return rv;
            }
        }

        public string DetermineOrigAndNewAttributeValue(UpdateOpContext context, ConnectorObject origObject, ICollection<ConnectorAttribute> attributesForReplace, string attributeName, out string origAttributeValue)
        {
            ConnectorAttribute originalAttribute = origObject.GetAttributeByName(attributeName);
            if (originalAttribute != null)
            {
                origAttributeValue = ConnectorAttributeUtil.GetAsStringValue(originalAttribute);
            }
            else
            {
                origAttributeValue = null;
            }

            ConnectorAttribute newAttribute = ConnectorAttributeUtil.Find(attributeName, attributesForReplace);
            if (newAttribute != null)
            {
                return ConnectorAttributeUtil.GetAsStringValue(newAttribute);
            }
            else
            {
                return origAttributeValue;
            }

            /*
            string deltaValue = ConnectorAttributeUtil.GetAsStringValue(attribute);
            if (attribute == null) {
                return origAttributeValue;
            }
            switch (context.UpdateType) {
                case UpdateType.ADD:
                    if (deltaValue == null) {
                        return origAttributeValue;
                    }
                    if (origAttributeValue != null && !origAttributeValue.Equals(deltaValue)) {
                        throw new ArgumentException("Multiple values for " + attribute.Name + " are not allowed: existing = " + origAttributeValue + ", one being added = " + deltaValue);
                    } else {
                        return deltaValue;
                    }
                case UpdateType.REPLACE:
                    return deltaValue;
                case UpdateType.DELETE:
                    if (deltaValue == null) {
                        return origAttributeValue;
                    }
                    if (origAttributeValue == null || !origAttributeValue.Equals(deltaValue)) {
                        LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "Trying to remove value from " + attribute.Name + " that is not there: " + deltaValue);
                        return origAttributeValue;
                    } else {
                        return null;
                    }
                default:
                    throw new ArgumentException("Invalid update type: " + context.UpdateType);
            } */
        }


        internal ConnectorObject GetCurrentObject(UpdateOpContext context, string query)
        {
            ConnectorObject currentObject = null;
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Fetching object using query {0}", query);

            ResultsHandler handler = new ResultsHandler()
            {
                Handle = cobject =>
                {
                    //LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Object-to-be-modified: {0}", CommonUtils.DumpConnectorAttributes(cobject.GetAttributes()));
                    if (currentObject != null)
                    {
                        throw new InvalidOperationException("More than one object complying with " + query + " was found");
                    }
                    currentObject = cobject;
                    return true;
                }
            };

            ((ExchangeConnector) context.Connector).ExecuteQuery(context.ObjectClass, query, handler, null);
            if (currentObject == null)
            {
                throw new ObjectNotFoundException("Object with UID " + context.Uid.GetUidValue() + " was not found");
            }

            return currentObject;
        }
    }
}
