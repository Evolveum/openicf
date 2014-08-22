using Org.IdentityConnectors.ActiveDirectory;
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
            Trace.TraceInformation("GUID value = " + guidPropertyInfo.Value);
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

            Trace.TraceInformation("Creating object with UID = {0} and Name = {1}", guid, name);
            foreach (ConnectorAttributeInfo cai in ocinfo.ConnectorAttributeInfos) {
                if (cai.IsReadable && properties.ContainsKey(cai.Name)) {
                    object value = properties[cai.Name].Value;
                    Trace.TraceInformation(" - attribute {0} = {1}", cai.Name, value);

                    if (value is PSObject) {
                        var ps = value as PSObject;
                        value = ps.BaseObject;
                        Trace.TraceInformation(" - attribute {0} UNWRAPPED = {1} ({2})", cai.Name, value, value.GetType());
                    }
                    builder.AddAttribute(cai.Name, CommonUtils.ConvertToSupportedForm(cai, value));
                }
            }
            return builder.Build();
        }
    }
}
