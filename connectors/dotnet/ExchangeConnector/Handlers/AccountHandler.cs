using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Spi;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Text;

namespace Org.IdentityConnectors.Exchange {
    class AccountHandler : ObjectClassHandler {

        // tracing
        private static TraceSource LOGGER = new TraceSource(TraceNames.ACCOUNT_HANDLER);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        private MiscHelper _helper = new MiscHelper();

        public void Create(CreateOpContext context) {

            context.Attributes = DeduplicateEmailAddresses(context, context.Attributes);

            // get recipient type
            string rcptType = ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttRecipientType, context.Attributes) as string;

            if (rcptType == null || rcptType.Equals("")) {
                rcptType = ExchangeConnectorAttributes.RcptTypeUser;
            }

            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;
            
            PSExchangeConnector.CommandInfo cmdInfoEnable = null;
            PSExchangeConnector.CommandInfo cmdInfoSet = null;
            switch (rcptType) {
                case ExchangeConnectorAttributes.RcptTypeMailBox:
                    cmdInfoEnable = PSExchangeConnector.CommandInfo.EnableMailbox;
                    cmdInfoSet = PSExchangeConnector.CommandInfo.SetMailbox;
                    break;
                case ExchangeConnectorAttributes.RcptTypeMailUser:
                    cmdInfoEnable = PSExchangeConnector.CommandInfo.EnableMailUser;
                    cmdInfoSet = PSExchangeConnector.CommandInfo.SetMailUser;
                    break;
                case ExchangeConnectorAttributes.RcptTypeUser:
                    break;
                default:
                    throw new ArgumentException(
                              context.ConnectorConfiguration.ConnectorMessages.Format(
                              "ex_bad_rcpt", "Recipient type [{0}] is not supported", rcptType));
            }

            // first create the object in AD
            ICollection<ConnectorAttribute> adAttributes = ExchangeUtility.FilterOut(context.Attributes,
                PSExchangeConnector.CommandInfo.EnableMailbox, 
                PSExchangeConnector.CommandInfo.SetMailbox,
                PSExchangeConnector.CommandInfo.EnableMailUser,
                PSExchangeConnector.CommandInfo.SetMailUser);
            Uid uid = adconn.Create(context.ObjectClass, adAttributes, context.Options);

            if (rcptType == ExchangeConnectorAttributes.RcptTypeUser) {
                // AD account only, we do nothing
                context.Uid = uid;
                return;
            }

            // add a empty "EmailAddresses" attribute if needed (address policy is disabled and no addresses are provided)
            ICollection<ConnectorAttribute> enhancedAttributes;
            ConnectorAttribute policyEnabledAttribute = ConnectorAttributeUtil.Find(ExchangeConnectorAttributes.AttEmailAddressPolicyEnabled, context.Attributes);
            if (policyEnabledAttribute != null &&
                ConnectorAttributeUtil.GetBooleanValue(policyEnabledAttribute).HasValue &&
                ConnectorAttributeUtil.GetBooleanValue(policyEnabledAttribute).Value == false &&
                ConnectorAttributeUtil.Find(ExchangeConnectorAttributes.AttPrimarySmtpAddress, context.Attributes) == null &&
                ConnectorAttributeUtil.Find(ExchangeConnectorAttributes.AttEmailAddresses, context.Attributes) == null) {

                enhancedAttributes = new HashSet<ConnectorAttribute>(context.Attributes);
                enhancedAttributes.Add(ConnectorAttributeBuilder.Build(ExchangeConnectorAttributes.AttEmailAddresses));
                LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Added empty EmailAddresses attribute because address policy use is disabled and no addresses were provided");
            } else {
                enhancedAttributes = context.Attributes;        // no change
            }

            // prepare the command            
            Command cmdEnable = ExchangeUtility.GetCommand(cmdInfoEnable, enhancedAttributes, uid, (ExchangeConfiguration) context.ConnectorConfiguration);
            Command cmdSet = ExchangeUtility.GetCommand(cmdInfoSet, enhancedAttributes, uid, (ExchangeConfiguration)context.ConnectorConfiguration);

            try {
                _helper.InvokePipeline(exconn, cmdEnable);
                _helper.InvokePipeline(exconn, cmdSet);
            }
            catch {
                LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Rolling back AD create for UID: " + uid.GetUidValue());

                // rollback AD create
                try {
                    adconn.Delete(context.ObjectClass, uid, context.Options);
                } catch {
                    LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "Not able to rollback AD create for UID: " + uid.GetUidValue());
                    // note: this is not perfect, we hide the original exception
                    throw;
                }

                // rethrow original exception
                throw;
            }

            context.Uid = uid;
        }

        public void Update(UpdateOpContext context) 
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            // update in AD first
            var filtered = ExchangeUtility.FilterOut(
                context.Attributes,
                PSExchangeConnector.CommandInfo.EnableMailbox,
                PSExchangeConnector.CommandInfo.EnableMailUser,
                PSExchangeConnector.CommandInfo.SetMailbox,
                PSExchangeConnector.CommandInfo.SetMailUser);
            adconn.Update(context.UpdateType, context.ObjectClass, context.Uid, filtered, context.Options);

            // retrieve Exchange-related information about the user
            string query = "(objectGUID=" + ActiveDirectoryUtils.ConvertUIDToSearchString(context.Uid) + ")";
            ConnectorObject currentObject = _helper.GetCurrentObject(context, query);
            ICollection<ConnectorAttribute> attributesForReplace = _helper.DetermineNewAttributeValues(context, currentObject);

            attributesForReplace = DeduplicateEmailAddresses(context, attributesForReplace);

            string origRcptType;
            var newRcptType = _helper.DetermineOrigAndNewAttributeValue(context, currentObject, attributesForReplace, ExchangeConnectorAttributes.AttRecipientType, out origRcptType);
            if (newRcptType == null) 
            {
                newRcptType = ExchangeConnectorAttributes.RcptTypeUser;
            }

            string origDatabase;
            var newDatabase = _helper.DetermineOrigAndNewAttributeValue(context, currentObject, attributesForReplace, ExchangeConnectorAttributes.AttDatabase, out origDatabase);

            // PART 1 - DEALING WITH MailUser CASE

            if (ExchangeConnectorAttributes.RcptTypeMailUser.Equals(newRcptType)) 
            {
                // disabling Mailbox if needed
                if (ExchangeConnectorAttributes.RcptTypeMailBox.Equals(origRcptType)) 
                {
                    Command cmdDisable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.DisableMailbox, attributesForReplace, context.Uid, exconn.Configuration);
                    cmdDisable.Parameters.Add("Confirm", false);
                    _helper.InvokePipeline(exconn, cmdDisable);
                }

                // enabling MailUser if needed
                if (!ExchangeConnectorAttributes.RcptTypeMailUser.Equals(origRcptType)) 
                {
                    // Enable-MailUser needs the value of ExternalEmailAddress, so we have to get it
                    string origExternalEmailAddress;
                    var newExternalEmailAddress = _helper.DetermineOrigAndNewAttributeValue(context, currentObject, attributesForReplace, ExchangeConnectorAttributes.AttExternalEmailAddress, out origExternalEmailAddress);

                    if (String.IsNullOrEmpty(newExternalEmailAddress)) 
                    {
                        throw new InvalidOperationException("Missing ExternalEmailAddress value, which is required for a MailUser");
                    }
                    ExchangeUtility.SetAttValue(ExchangeConnectorAttributes.AttExternalEmailAddress, newExternalEmailAddress, attributesForReplace);

                    // now execute the Enable-MailUser command
                    Command cmdEnable = ExchangeUtility.GetCommand(
                                PSExchangeConnector.CommandInfo.EnableMailUser, attributesForReplace, context.Uid, exconn.Configuration);
                    _helper.InvokePipeline(exconn, cmdEnable);
                }

                // setting MailUser attributes
                Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetMailUser, attributesForReplace, context.Uid, exconn.Configuration);
                _helper.InvokePipeline(exconn, cmdSet);
            }

            // PART 2 - DEALING WITH UserMailbox CASE

            else if (ExchangeConnectorAttributes.RcptTypeMailBox.Equals(newRcptType))
            {
                // enable mailbox if necessary

                // we should execute something like this here:
                // get-user -identity id|?{$_.RecipientType -eq "User"}|enable-mailbox -database "db"
                // unfortunately I was not able to get it working with the pipeline... that's why there are two commands
                // executed :-(
                // alternatively there can be something like:
                // get-user -identity id -RecipientTypeDetails User|enable-mailbox -database "db", but we have then trouble
                // with detecting attempt to change the database attribute               
                if (!ExchangeConnectorAttributes.RcptTypeMailBox.Equals(origRcptType))
                {
                    Command cmdEnable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.EnableMailbox, attributesForReplace, context.Uid, exconn.Configuration);
                    _helper.InvokePipeline(exconn, cmdEnable);
                }
                else
                {
                    // are we trying to update the database?
                    if (newDatabase != null && origDatabase != null && !newDatabase.Equals(origDatabase))
                    {
                        throw new ArgumentException(
                            context.ConnectorConfiguration.ConnectorMessages.Format(
                            "ex_not_updatable", "Update of [{0}] attribute is not supported", ExchangeConnectorAttributes.AttDatabase));
                    }
                }

                Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetMailbox, attributesForReplace, context.Uid, exconn.Configuration);
                _helper.InvokePipeline(exconn, cmdSet);
            }

            // PART 3 - DEALING WITH User CASE

            else if (ExchangeConnectorAttributes.RcptTypeUser.Equals(newRcptType))
            {
                if (ExchangeConnectorAttributes.RcptTypeMailBox.Equals(origRcptType))
                {
                    Command cmdDisable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.DisableMailbox, attributesForReplace, context.Uid, exconn.Configuration);
                    cmdDisable.Parameters.Add("Confirm", false);
                    _helper.InvokePipeline(exconn, cmdDisable);
                }
                else if (ExchangeConnectorAttributes.RcptTypeMailUser.Equals(origRcptType))
                {
                    Command cmdDisable = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.DisableMailUser, attributesForReplace, context.Uid, exconn.Configuration);
                    cmdDisable.Parameters.Add("Confirm", false);
                    _helper.InvokePipeline(exconn, cmdDisable);
                }
                else if (ExchangeConnectorAttributes.RcptTypeUser.Equals(origRcptType))
                {
                    // if orig is User, there is no need to disable anything
                }
                else
                {
                    throw new InvalidOperationException("Invalid original recipient type: " + origRcptType);
                }

                Command cmdSet = ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.SetUser, attributesForReplace, context.Uid, exconn.Configuration);
                _helper.InvokePipeline(exconn, cmdSet);
            }
            else
            {
                // unsupported rcpt type
                throw new ArgumentException(
                            context.ConnectorConfiguration.ConnectorMessages.Format(
                            "ex_bad_rcpt", "Recipient type [{0}] is not supported", newRcptType));
            }
        }

        /// <summary>
        /// DeduplicatesEmailAddresses.
        ///  - on REPLACE (i.e. update/replace or create) the situation is easy: we just remove duplicate entries (SMTP:x & smtp:x result in SMTP:x)
        ///  - on DELETE we currently do nothing
        ///  - on ADD we have one additional rule:
        ///     "If we are adding SMTP:x, we first convert all SMTP:y in existing records to smtp:y because we see the intent of having x to be a new primary"
        ///  
        /// </summary>
        /// <param name="context"></param>
        /// <param name="attributes">these are attributes to be set (already resolved if we have update of type ADD or DELETE)</param>
        /// <returns></returns>
        private ICollection<ConnectorAttribute> DeduplicateEmailAddresses(CreateUpdateOpContext context, ICollection<ConnectorAttribute> attributes)
        {
            // trivial cases

            if (context is UpdateOpContext && ((UpdateOpContext)context).UpdateType == UpdateType.DELETE)
            {
                return attributes;
            }

            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(ExchangeConnectorAttributes.AttEmailAddresses, attributes);
            if (attribute == null || attribute.Value == null)
            {
                return attributes;      // missing or empty EmailAddresses - nothing to deduplicate
            }
            ConnectorAttribute attributeDelta = ConnectorAttributeUtil.Find(ExchangeConnectorAttributes.AttEmailAddresses, context.Attributes);
            if (attributeDelta == null || attributeDelta.Value == null)
            {
                return attributes;      // missing or empty changed EmailAddresses - nothing to deduplicate
            }

            // now the main part

            IList<string> valuesToDeduplicate = new List<string>();
            foreach (object o in attribute.Value)
            {
                if (o != null)
                {
                    valuesToDeduplicate.Add(o.ToString());
                }
            }
            bool changed = false;

            // special rule: if ADD with SMTP:, let us change all other "SMTP:x" to "smtp:x"
            Boolean isUpdateAdd = context is UpdateOpContext && ((UpdateOpContext)context).UpdateType == UpdateType.ADD;
            if (isUpdateAdd)
            {
                string newPrimary = null;
                foreach (object o in attributeDelta.Value)
                {
                    if (((string)o).StartsWith("SMTP:"))
                    {
                        newPrimary = (string)o;
                        break;
                    }
                }
                if (newPrimary != null)
                {
                    foreach (string address in new List<string>(valuesToDeduplicate))      // to eliminate concurrent access
                    {
                        if (address.StartsWith("SMTP:") && !address.Equals(newPrimary))
                        {
                            string replacement = "smtp:" + address.Substring(5);
                            LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Changing duplicate primary-candidate address {0} to {1}", address, replacement);
                            valuesToDeduplicate.Remove(address);
                            valuesToDeduplicate.Add(replacement);
                            changed = true;
                        }
                    }
                }
            }

            IDictionary<string, string> values = new Dictionary<string, string>();       // normalized->most-recent-original e.g. SMTP:XYZ@AAA.EDU -> SMTP:xyz@aaa.edu (if primary is present)
            foreach (object v in valuesToDeduplicate) 
            {
                string address = (string)v;
                string normalized = address.ToUpper();
                if (values.ContainsKey(normalized)) 
                {
                    changed = true;
                    string existing = values[normalized];
                    if (address.StartsWith("SMTP:") && existing.StartsWith("smtp:")) 
                    {
                        LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Removing redundant address {0}, keeping {1}", existing, address);
                        values[normalized] = address;
                    }
                    else 
                    {
                        LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Removing redundant address {0}, keeping {1}", address, existing);
                    }
                } 
                else 
                {
                    values.Add(normalized, address);
                }
            }
            if (changed)
            {
                ConnectorAttributeBuilder cab = new ConnectorAttributeBuilder();
                cab.Name = ExchangeConnectorAttributes.AttEmailAddresses;
                foreach (string value in values.Values)
                {
                    cab.AddValue(value);
                }
                ICollection<ConnectorAttribute> rv = new List<ConnectorAttribute>(attributes);          // the original is (sometimes) a read-only collection
                rv.Remove(attribute);
                rv.Add(cab.Build());
                return rv;
            }
            else
            {
                return attributes;
            }
        }

        public void Delete(DeleteOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            adconn.Delete(context.ObjectClass, context.Uid, context.Options);
        }

        public void ExecuteQuery(ExecuteQueryContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            ICollection<string> attsToGet = null;
            if (context.Options != null && context.Options.AttributesToGet != null)
            {
                attsToGet = CollectionUtil.NewList(context.Options.AttributesToGet);
            }

            // delegate to get the exchange attributes if requested            
            ResultsHandler filter = new SearchResultsHandler()
            {
                Handle = cobject =>
                {
                    LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Object returned from AD connector: {0}", CommonUtils.DumpConnectorAttributes(cobject.GetAttributes()));
                    ConnectorObject filtered = ExchangeUtility.ConvertAdAttributesToExchange(cobject);
                    //filtered = AddExchangeAttributes(exconn, context.ObjectClass, filtered, attsToGet);
                    LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Object as passed from Exchange connector: {0}", CommonUtils.DumpConnectorAttributes(filtered.GetAttributes()));
                    return context.ResultsHandler.Handle(filtered);
                },

                HandleResult = result =>
                    {
                        if (context.ResultsHandler is SearchResultsHandler)
                        {
                            ((SearchResultsHandler)context.ResultsHandler).HandleResult(result);
                        }
                    }

            };

            ResultsHandler handler2use = filter;
            OperationOptions options2use = context.Options;

            // mapping AttributesToGet from Exchange to AD "language"
            // actually, we don't need this code any more, because there are no attribute that are not retrieved by default 
            // Uncomment this code if necessary in the future.
            if (context.Options != null && context.Options.AttributesToGet != null)
            {
                /*
                ISet<string> mappedExchangeAttributesToGet = new HashSet<string>(AttMap2AD.Keys);
                mappedExchangeAttributesToGet.IntersectWith(options.AttributesToGet);
                if (mappedExchangeAttributesToGet.Count > 0 || attsToGet.Contains(AttRecipientType))
                {
                    // replace Exchange attributes with AD names
                    var newAttsToGet = ExchangeUtility.FilterReplace(attsToGet, AttMap2AD);

                    // we have to remove recipient type, as it is unknown to AD
                    newAttsToGet.Remove(AttRecipientType);

                    // build new op options
                    var builder = new OperationOptionsBuilder(options);
                    string[] attributesToGet = new string[newAttsToGet.Count];
                    newAttsToGet.CopyTo(attributesToGet, 0);
                    builder.AttributesToGet = attributesToGet;
                    options2use = builder.Build();
                } */

                /*
                if (attsToGet.Contains(ExchangeConnectorAttributes.AttDatabase))
                {
                    attsToGet.Remove(ExchangeConnectorAttributes.AttDatabase);

                    // build new op options
                    var builder = new OperationOptionsBuilder(context.Options);
                    string[] attributesToGet = new string[attsToGet.Count];
                    attsToGet.CopyTo(attributesToGet, 0);
                    builder.AttributesToGet = attributesToGet;
                    options2use = builder.Build();
                }
                 */
            }

            adconn.ExecuteQueryInternal(context.ObjectClass, 
                context.Query, handler2use, options2use, 
                GetAdAttributesToReturn(adconn, context.ObjectClass, context.Options));
        }

        public void Sync(SyncOpContext context)
        {
            ExchangeConnector exconn = (ExchangeConnector)context.Connector;
            ActiveDirectoryConnector adconn = exconn.ActiveDirectoryConnector;

            ICollection<string> attsToGet = null;
            if (context.Options != null && context.Options.AttributesToGet != null)
            {
                attsToGet = CollectionUtil.NewSet(context.Options.AttributesToGet);
            }

            // delegate to get the exchange attributes if requested            
            SyncResultsHandler xchangeHandler = new SyncResultsHandler()
            {
                Handle = delta =>
                {
                    if (delta.DeltaType == SyncDeltaType.DELETE)
                    {
                        return context.SyncResultsHandler.Handle(delta);
                    }

                    // replace the ad attributes with exchange ones and add recipient type and database (if requested)
                    ConnectorObject updated = ExchangeUtility.ConvertAdAttributesToExchange(delta.Object);
                    //updated = this.AddExchangeAttributes(exconn, context.ObjectClass, updated, attsToGet);
                    if (updated != delta.Object)
                    {
                        // build new sync delta, cause we changed the object
                        SyncDeltaBuilder deltaBuilder = new SyncDeltaBuilder
                        {
                            DeltaType = delta.DeltaType,
                            Token = delta.Token,
                            Uid = delta.Uid,
                            Object = updated
                        };
                        delta = deltaBuilder.Build();
                    }

                    return context.SyncResultsHandler.Handle(delta);
                }
            };

            // call AD sync, use xchangeHandler
            adconn.SyncInternal(context.ObjectClass, 
                context.SyncToken, xchangeHandler, context.Options, 
                GetAdAttributesToReturn(adconn, context.ObjectClass, context.Options));
        }

        
        public ObjectClassInfo GetObjectClassInfo(ExchangeConnector connector, ObjectClass oc)
        {
            return connector.GetObjectClassInfoGeneric(oc);
            /*
            // get the original object class info
            ObjectClassInfo oinfo = connector.GetObjectClassInfoGeneric(oc);
            Trace.TraceInformation("ExchangeConnector.GetObjectClassInfo: oinfo for {0} as retrieved has {1} entries", oc, oinfo.ConnectorAttributeInfos.Count);

            // add additional attributes for ACCOUNT
            var classInfoBuilder = new ObjectClassInfoBuilder { IsContainer = oinfo.IsContainer, ObjectType = oinfo.ObjectType };
            classInfoBuilder.AddAllAttributeInfo(oinfo.ConnectorAttributeInfos);
            //classInfoBuilder.AddAllAttributeInfo(ExchangeConnectorAttributes.ManualExchangeAttInfosForSchema);
            //classInfoBuilder.AddAllAttributeInfo(ExchangeConnectorAttributes.AttInfoCustomAttributesForSchema);
            //classInfoBuilder.AddAllAttributeInfo(ExchangeConnectorAttributes.ExchangeRelatedADAttInfosForSchema);
            oinfo = classInfoBuilder.Build();
            Trace.TraceInformation("ExchangeConnector.GetObjectClassInfo: newly created oinfo has {0} entries", oinfo.ConnectorAttributeInfos.Count);

            // return
            return oinfo;
             */
        }




        /// <summary>
        /// Gets the Exchange user using powershell Get-User command
        /// </summary>
        /// <param name="cmdInfo">command info to get the user</param>
        /// <param name="attributes">attributes containing the Name</param>
        /// <returns><see cref="PSObject"/> with user info</returns>
        private PSObject GetUser(ExchangeConnector connector, PSExchangeConnector.CommandInfo cmdInfo, Name nameAttribute)
        {
            ExchangeConfiguration configuration = connector.Configuration;
            // assert we have user name
            string name = nameAttribute.GetNameValue();
            ExchangeUtility.NullCheck(name, "User name", configuration);

            ICollection<ConnectorAttribute> attributes = new List<ConnectorAttribute>();
            attributes.Add(nameAttribute);
            Command cmdUser = ExchangeUtility.GetCommand(cmdInfo, attributes, configuration);
            ICollection<PSObject> users = _helper.InvokePipeline(connector, cmdUser);
            if (users.Count == 1)
            {
                foreach (PSObject obj in users)
                {
                    return obj;
                }
            }

            throw new ArgumentException(
                configuration.ConnectorMessages.Format(
                "ex_bad_username", "Provided User name is not unique or not existing"));
        }

        /// <summary>
        /// Gets Recipient Type/Database from Exchange database, this method can be more general, but it is ok
        /// for our needs
        /// </summary>
        /// <param name="oc">object class, currently the method works for <see cref="ObjectClass.ACCOUNT"/> only</param>
        /// <param name="cobject">connector object to get the recipient type/database for</param>
        /// <param name="attToGet">attributes to get</param>
        /// <returns>Connector Object with recipient type added</returns>
        /// <exception cref="ConnectorException">In case of some troubles in powershell (if the 
        /// user is not found we get this exception too)</exception>
        private ConnectorObject AddExchangeAttributes(ExchangeConnector exchangeConnector, ObjectClass oc, ConnectorObject cobject, IEnumerable<string> attToGet)
        {
            // NOTHING TO DO. Everything has been read from AD!
            return cobject;

            /*
            ExchangeConfiguration configuration = exchangeConnector.Configuration;
            ExchangeUtility.NullCheck(oc, "name", configuration);
            ExchangeUtility.NullCheck(oc, "cobject", configuration);

            // we support ACCOUNT only or there is nothing to add
            if (!oc.Is(ObjectClass.ACCOUNT_NAME) || attToGet == null)
            {
                return cobject;
            }

            // check it is not deleted object
            bool? deleted = ExchangeUtility.GetAttValue(ExchangeConnectorAttributes.AttIsDeleted, cobject.GetAttributes()) as bool?;
            if (deleted != null && deleted == true)
            {
                // do nothing, it is deleted object
                return cobject;
            }

            ICollection<string> lattToGet = CollectionUtil.NewCaseInsensitiveSet();
            CollectionUtil.AddAll(lattToGet, attToGet);
            foreach (string att in attToGet)
            {
                if (cobject.GetAttributeByName(att) != null && att != ExchangeConnectorAttributes.AttDatabase)
                {
                    lattToGet.Remove(att);
                }
            }

            if (lattToGet.Count == 0)
            {
                return cobject;
            }

            ConnectorObjectBuilder cobjBuilder = new ConnectorObjectBuilder();
            cobjBuilder.AddAttributes(cobject.GetAttributes());

            PSExchangeConnector.CommandInfo cmdInfo = PSExchangeConnector.CommandInfo.GetUser;

            // prepare the connector attribute list to get the command
            ICollection<ConnectorAttribute> attributes = new Collection<ConnectorAttribute> { cobject.Name };

            // get the command
            Command cmd = ExchangeUtility.GetCommand(cmdInfo, attributes, configuration);
            ICollection<PSObject> foundObjects = _helper.InvokePipeline(exchangeConnector, cmd);
            PSObject user = null;
            if (foundObjects != null && foundObjects.Count == 1)
            {
                user = GetFirstElement(foundObjects);
                foreach (var info in user.Properties)
                {
                    ConnectorAttribute att = ExchangeUtility.GetAsAttribute(info);
                    if (att != null && lattToGet.Contains(att.Name))
                    {
                        cobjBuilder.AddAttribute(att);
                        lattToGet.Remove(att.Name);
                    }
                }

                if (lattToGet.Count == 0)
                {
                    return cobjBuilder.Build();
                }
            }

            if (user == null)
            {
                // nothing to do
                return cobject;
            }

            string rcptType = user.Members[ExchangeConnectorAttributes.AttRecipientType].Value.ToString();
            foundObjects = null;

            // get detailed information            
            if (rcptType == ExchangeConnectorAttributes.RcptTypeMailBox)
            {
                foundObjects = _helper.InvokePipeline(exchangeConnector, ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.GetMailbox, attributes, configuration));
            }
            else if (rcptType == ExchangeConnectorAttributes.RcptTypeMailUser)
            {
                foundObjects = _helper.InvokePipeline(exchangeConnector, ExchangeUtility.GetCommand(PSExchangeConnector.CommandInfo.GetMailUser, attributes, configuration));
            }

            if (foundObjects != null && foundObjects.Count == 1)
            {
                PSObject userDetails = GetFirstElement(foundObjects);
                foreach (var info in userDetails.Properties)
                {
                    ConnectorAttribute att = ExchangeUtility.GetAsAttribute(info);
                    if (att != null && lattToGet.Contains(att.Name))
                    {
                        cobjBuilder.AddAttribute(att);
                        lattToGet.Remove(att.Name);
                    }
                }
            }

            return cobjBuilder.Build();
            */
        }

        private ICollection<string> GetAdAttributesToReturn(ActiveDirectoryConnector adConnector, ObjectClass oclass, OperationOptions options)
        {
            ICollection<string> attNames = adConnector.GetAdAttributesToReturn(oclass, options);

            // In attNames there is a mix of attributes - some AD-only ones (from ObjectClasses.xml),
            // and some Exchange ones (from the schema) and some AD-only-for-Exchange ones (from the schema).
            // We should convert Exchange ones to their AD counterparts and add "hidden useful" AD attributes.

            if (oclass.Is(ObjectClass.ACCOUNT_NAME))
            {
                ICollection<string> newAttNames = new HashSet<string>(attNames);

                // IMPORTANT: we assume that "options" do not imply any additional AD attributes
                CollectionUtil.AddAll(newAttNames, ExchangeConnectorAttributes.AttMapFromAD.Keys);
                CollectionUtil.AddAll(newAttNames, ExchangeConnectorAttributes.HiddenAdAttributesToRetrieve);
                CollectionUtil.AddAll(newAttNames, ExchangeConnectorAttributes.VisibleAdAttributesToRetrieve);
                attNames = newAttNames;
            }
            return attNames;
        }


        /// <summary>
        /// Returns first element of the collection
        /// </summary>
        /// <typeparam name="T">Object Type stored in collection</typeparam>
        /// <param name="collection">Collection to get the first element from</param>
        /// <returns>First element in the collection, null if the collection is empty</returns>
        private static T GetFirstElement<T>(IEnumerable<T> collection) where T : class
        {
            foreach (T o in collection)
            {
                return o;
            }

            return null;
        }


        // TODO move to appropriate place
        /// <summary>
        /// helper method for searching object in AD by UID
        /// </summary>
        /// <param name="uid">Uid of the searched </param>
        /// <param name="oclass">Object class</param>
        /// <param name="options">Operation options</param>
        /// <returns>Connector object found by the Uid</returns>
        internal ConnectorObject ADSearchByUid(ExchangeConnector connector, Uid uid, ObjectClass oclass, OperationOptions options)
        {
            ExchangeConfiguration configuration = connector.Configuration;
            ExchangeUtility.NullCheck(uid, "uid", configuration);
            ExchangeUtility.NullCheck(oclass, "oclass", configuration);
            if (options == null)
            {
                options = new OperationOptionsBuilder().Build();
            }

            ConnectorObject ret = null;
            Filter filter = FilterBuilder.EqualTo(uid);
            var translator = connector.ActiveDirectoryConnector.CreateFilterTranslator(oclass, options);
            IList<string> queries = translator.Translate(filter);

            if (queries.Count == 1)
            {
                ResultsHandler handler = new ResultsHandler()
                {
                    Handle = cobject =>
                    {
                        ret = cobject;
                        return false;
                    }
                };
                connector.ActiveDirectoryConnector.ExecuteQuery(oclass, queries[0], handler, options);
            }

            return ret;
        }

        public FilterTranslator<string> CreateFilterTranslator(ExchangeConnector connector, ObjectClass oclass, OperationOptions options)
        {
            return new LegacyExchangeConnectorFilterTranslator();
        }

        /// <summary>
        /// Filter translator which does MS Exchange specific translation
        /// </summary>
        private class LegacyExchangeConnectorFilterTranslator : ActiveDirectoryFilterTranslator
        {
            /// <summary>
            /// Translates the connector attribute name to LDAP name            
            /// </summary>
            /// <param name="attr">Connector attribute name</param>
            /// <returns>Translated string array</returns>
            protected override string[] GetLdapNamesForAttribute(ConnectorAttribute attr)
            {
                // Exchange attributes with known mappings to AD
                foreach (string attrNameInExchange in ExchangeConnectorAttributes.AttMap2AD.Keys)
                {
                    if (attr.Is(attrNameInExchange))
                    {
                        return new[] { ExchangeConnectorAttributes.AttMap2AD[attrNameInExchange] };
                    }
                }

                // Other Exchange attributes have no mapping to AD ones.
                // This means that some attributes with more complicated mappings,
                // like RecipientType or EmailAddressPolicyEnabled, cannot be
                // used in search queries.
                if (ExchangeConnectorAttributes.IsExchangeAttribute(attr))
                {
                    return null;
                }
                else
                {
                    return base.GetLdapNamesForAttribute(attr);
                }
            }
        }


    }
}
