/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
using System;
using System.Reflection;
using ActiveDs;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using Org.IdentityConnectors.Common.Pooling;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
using System.Diagnostics;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using System.DirectoryServices;
using System.DirectoryServices.ActiveDirectory;
using Org.IdentityConnectors.Framework.Common;
using System.Text;
using Org.IdentityConnectors.Common.Script;

namespace Org.IdentityConnectors.ActiveDirectory
{
    [ConnectorClass("connector_displayName",
                      typeof(ActiveDirectoryConfiguration),
                      MessageCatalogPath = "Org.IdentityConnectors.ActiveDirectory.Messages"
                      )]
    /// <summary>
	/// The Active Directory Connector
	/// </summary>
    public class ActiveDirectoryConnector : CreateOp, Connector, SchemaOp, DeleteOp,
        SearchOp<String>, TestOp, AdvancedUpdateOp, ScriptOnResourceOp, SyncOp, 
        AuthenticateOp, AttributeNormalizer, PoolableConnector
	{
        // This is the list of attributes returned by default if no attributes are
        // requested in the options field of ExecuteQuery
        public readonly static ICollection<string> AccountAttributesReturnedByDefault = 
            new HashSet<string>(StringComparer.CurrentCultureIgnoreCase) {
//            "userPassword",
            "sAMAccountName",
            "givenName",
            "sn",
            "displayName",
            "mail",
            "telephoneNumber",
            "employeeId",
            "division",
            "mobile",
            "middleName",
            "description",
            "department",
            "manager",
            "title",
            "initials",
            "co",
            "company",
            "facsimileTelephoneNumber",
            "homePhone",
            "streetAddress",
            "l",
            "st",
            "postalCode",
            "TerminalServicesInitialProgram",
            "TerminalServicesWorkDirectory",
            "AllowLogon",
            "MaxConnectionTime",
            "MaxDisconnectionTime",
            "MaxIdleTime",
            "ConnectClientDrivesAtLogon",
            "ConnectClientPrintersAtLogon",
            "DefaultToMainPrinter",
            "BrokenConnectionAction",
            "ReconnectionAction",
            "EnableRemoteControl",
            "TerminalServicesProfilePath",
            "TerminalServicesHomeDirectory",
            "TerminalServicesHomeDrive",
            "uSNChanged",
            "ad_container",
            "otherHomePhone",
            "distinguishedName",
            "objectClass",
            "homeDirectory",
       };

        public static IDictionary<ObjectClass, ICollection<string>> AttributesReturnedByDefault = null;
            

        // special attribute names
        public static readonly string ATT_CONTAINER = "ad_container";
        public static readonly string ATT_USER_PASSWORD = "userPassword";
        public static readonly string ATT_CN = "cn";
        public static readonly string ATT_OU = "ou";
        public static readonly string ATT_OBJECT_GUID = "objectGuid";
        public static readonly string ATT_IS_DELETED = "isDeleted";
        public static readonly string ATT_USN_CHANGED = "uSNChanged";
        public static readonly string ATT_DISTINGUISHED_NAME = "distinguishedName";
        public static readonly string ATT_SAMACCOUNT_NAME = "sAMAccountName";
        public static readonly string ATT_MEMBER = "member";
        public static readonly string ATT_MEMBEROF = "memberOf";
        public static readonly string ATT_HOME_DIRECTORY = "homeDirectory";
        public static readonly string ATT_OBJECT_SID = "objectSid";

        public static readonly string OBJECTCLASS_OU = "organizationalUnit";
        public static readonly ObjectClass ouObjectClass = new ObjectClass(OBJECTCLASS_OU);

        ActiveDirectoryConfiguration _configuration = null;
        ActiveDirectoryUtils _utils = null;
        private Schema _schema = null;

        static ActiveDirectoryConnector()
        {
            // populate default attributes
            AttributesReturnedByDefault = new Dictionary<ObjectClass, ICollection<string>>();
            AttributesReturnedByDefault.Add(ObjectClass.ACCOUNT, AccountAttributesReturnedByDefault);
        }

        #region CreateOp Members
        // implementation of CreateSpiOp
        public Uid Create(ObjectClass oclass, 
            ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            Uid uid = null;

            // I had lots of problems here.  Here are the things
            // that seemed to make everything work:
            // - Create the object with the minimum data and commit it,
            //   then update the object with the rest of the info.
            // - After updating an object and committing, be sure to 
            //   do a refresh cache before continuing to use it.  If
            //   not, it seems like multi-value attributes get hosed.
            // - Group membership cannot be change by memberOf, but must
            //   be changed by changing the members property of the group

            Trace.TraceInformation("Create method");
            if (_configuration == null)
            {
                throw new ConfigurationException("Connector has not been configured");
            }
            Name nameAttribute = ConnectorAttributeUtil.GetNameFromAttributes(attributes);
            if (nameAttribute == null)
            {
                throw new ConnectorException("The name operational attribute cannot be null");
            }

            String ldapContainerPath = ActiveDirectoryUtils.GetLDAPPath(_configuration.LDAPHostName,
                ActiveDirectoryUtils.GetParentDn(nameAttribute.GetNameValue()));
            String ldapEntryPath = ActiveDirectoryUtils.GetLDAPPath(_configuration.LDAPHostName, 
                nameAttribute.GetNameValue());
            
            try
            {
                if (!DirectoryEntry.Exists(ldapContainerPath))
                {
                    throw new ConnectorException("Container does not exist");
                }

                // Get the correct container, and put the new user in it
                DirectoryEntry containerDe = new DirectoryEntry(ldapContainerPath,
                    _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
                DirectoryEntry newDe = containerDe.Children.Add(
                    ActiveDirectoryUtils.GetRelativeName(nameAttribute),
                    ActiveDirectoryUtils.GetADObjectClass(oclass));

                newDe.CommitChanges();

                // default to creating users enabled
                if ((ObjectClass.ACCOUNT.Equals(oclass)) && 
                    (ConnectorAttributeUtil.Find(OperationalAttributes.ENABLE_NAME, attributes) == null))
                {
                    attributes.Add(ConnectorAttributeBuilder.Build(OperationalAttributes.ENABLE_NAME, true));
                }
                _utils.UpdateADObject(oclass, newDe, attributes,
                    UpdateType.REPLACE, _configuration);
                Object guidValue = newDe.Properties["objectGUID"].Value;
                if (guidValue != null)
                {
                    // format the uid in the special way required for searching
                    String guidString =
                        ActiveDirectoryUtils.ConvertUIDBytesToGUIDString(
                        (Byte[])guidValue);

                    Trace.TraceInformation("Created object with uid {0}", guidString);
                    uid = new Uid(guidString);
                }
                else
                {
                    Trace.TraceError("Unable to find uid attribute for newly created object");
                }


            }
            catch (DirectoryServicesCOMException exception)
            {
                // have to make sure the new thing gets deleted in 
                // the case of error
                Console.WriteLine("caught exception:" + exception);
                Trace.TraceError(exception.Message);
                throw;
            }

            return uid;
        }

        #endregion

        #region Connector Members

        // implementation of Connector
        public void Init(Configuration configuration)
        {
            Trace.TraceInformation("Init method");
            _configuration = (ActiveDirectoryConfiguration)configuration;
            _utils = new ActiveDirectoryUtils(_configuration);
        }

        #endregion

        #region IDisposable Members

        public void Dispose()
        {
        }

        #endregion

        #region SchemaOp Members
        // implementation of SchemaSpiOp
        public Schema Schema()
        {            
            Trace.TraceInformation("Schema method");

            if (_schema != null)
            {
                return _schema;
            }

            // could have blocked on the lock while someone else built the 
            // schema, so check one more time before building
            if (_schema == null)
            {
                String serverName = _configuration.LDAPHostName;
                Forest forest = null;

                if ((serverName == null) || (serverName.Length == 0))
                {
                    // get the active directory schema
                    DirectoryContext context = new DirectoryContext(
                            DirectoryContextType.Domain,
                            _configuration.DomainName,
                            _configuration.DirectoryAdminName,
                            _configuration.DirectoryAdminPassword);
                    DomainController dc = DomainController.FindOne(context);
                    forest = dc.Forest;
                }
                else
                {
                    DirectoryContext context = new DirectoryContext(
                            DirectoryContextType.DirectoryServer,
                            _configuration.LDAPHostName,
                            _configuration.DirectoryAdminName,
                            _configuration.DirectoryAdminPassword);
                    forest = Forest.GetForest(context);
                }

                ActiveDirectorySchema ADSchema = forest.Schema;


                // get the user attribute infos and operations
                ICollection<ConnectorAttributeInfo> userAttributeInfos =
                    GetUserAttributeInfos(ADSchema);
                ObjectClassInfoBuilder ociBuilder = new ObjectClassInfoBuilder();
                ociBuilder.ObjectType = ObjectClass.ACCOUNT_NAME;
                ociBuilder.AddAllAttributeInfo(userAttributeInfos);
                ObjectClassInfo userInfo = ociBuilder.Build();

                // get the group attribute infos and operations
                ICollection<ConnectorAttributeInfo> groupAttributeInfos =
                    GetGroupAttributeInfos(ADSchema);
                ociBuilder = new ObjectClassInfoBuilder();
                ociBuilder.ObjectType = ObjectClass.GROUP_NAME;
                ociBuilder.AddAllAttributeInfo(groupAttributeInfos);
                ObjectClassInfo groupInfo = ociBuilder.Build();

                // get the organizationalUnit attribute infos and operations
                ICollection<ConnectorAttributeInfo> ouAttributeInfos =
                    GetOuAttributeInfos(ADSchema);
                ociBuilder = new ObjectClassInfoBuilder();
                ociBuilder.ObjectType = OBJECTCLASS_OU;
                ociBuilder.AddAllAttributeInfo(ouAttributeInfos);
                ObjectClassInfo ouInfo = ociBuilder.Build();

                SchemaBuilder schemaBuilder = new SchemaBuilder(this.GetType());
                
                schemaBuilder.DefineObjectClass(userInfo);
                schemaBuilder.DefineObjectClass(groupInfo);
                schemaBuilder.RemoveSupportedObjectClass(typeof(AuthenticateOp), groupInfo);
                schemaBuilder.DefineObjectClass(ouInfo);
                schemaBuilder.RemoveSupportedObjectClass(typeof(AuthenticateOp), ouInfo);
                schemaBuilder.RemoveSupportedObjectClass(typeof(CreateOp), ouInfo);
                schemaBuilder.RemoveSupportedObjectClass(typeof(DeleteOp), ouInfo);
                schemaBuilder.RemoveSupportedObjectClass(typeof(SearchOp<String>), ouInfo);

                _schema = schemaBuilder.Build();
            }

            return _schema;
        }

        public ICollection<ConnectorAttributeInfo> GetUserAttributeInfos(
            ActiveDirectorySchema ADSchema)
        {
            ICollection<ConnectorAttributeInfo> attributeInfos = new Collection<ConnectorAttributeInfo>();
            // put in operational attributes
            attributeInfos.Add(OperationalAttributeInfos.ENABLE);
            /*
            attributeInfos.Add(OperationalAttributeInfos.ENABLE_DATE);
            attributeInfos.Add(OperationalAttributeInfos.DISABLE_DATE);
            attributeInfos.Add(OperationalAttributeInfos.LOCK_OUT);
             */
            attributeInfos.Add(OperationalAttributeInfos.CURRENT_PASSWORD);
            // dont think I need this
            // attributeInfos.Add(OperationalAttributeInfos.RESET_PASSWORD);
            attributeInfos.Add(PredefinedAttributeInfos.GROUPS);
            attributeInfos.Add(OperationalAttributeInfos.PASSWORD);            

            ICollection<String> attributesToIgnore = new List<String>();
            attributesToIgnore.Add("CN");
            attributesToIgnore.Add(ATT_USER_PASSWORD);

            // get everything else from the schema
            PopulateSchemaFromAD(_configuration.ObjectClass, ADSchema, attributeInfos, 
                attributesToIgnore, ObjectClass.ACCOUNT);

            // now add in container ... 
            attributeInfos.Add(GetConnectorAttributeInfo(ATT_CONTAINER, 
                typeof(string), true, true, false, false, ObjectClass.ACCOUNT));

            // add in the userPassword
            // attributeInfos.Add(GetConnectorAttributeInfo(ATT_USER_PASSWORD,
            // typeof(string), false, true, true, false, ObjectClass.ACCOUNT));

            // add in terminal services attributes
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_INITIAL_PROGRAM, typeof(string), 
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_INITIAL_PROGRAM_DIR, typeof(string), 
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_ALLOW_LOGON, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_MAX_CONNECTION_TIME, typeof(int), 
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_MAX_DISCONNECTION_TIME, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_MAX_IDLE_TIME, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_CONNECT_CLIENT_DRIVES_AT_LOGON, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_CONNECT_CLIENT_PRINTERS_AT_LOGON, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_DEFAULT_TO_MAIN_PRINTER, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_BROKEN_CONNECTION_ACTION, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_RECONNECTION_ACTION, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_ENABLE_REMOTE_CONTROL, typeof(int),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_PROFILE_PATH, typeof(string),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_HOME_DIRECTORY, typeof(string),
                false, true, false, false, ObjectClass.ACCOUNT));
            attributeInfos.Add(GetConnectorAttributeInfo(
                TerminalServicesUtils.TS_HOME_DRIVE, typeof(string),
                false, true, false, false, ObjectClass.ACCOUNT));

            return attributeInfos;
        }

        public ICollection<ConnectorAttributeInfo> GetGroupAttributeInfos(
            ActiveDirectorySchema ADSchema)
        {
            ICollection<ConnectorAttributeInfo> attributeInfos = new Collection<ConnectorAttributeInfo>();
            // put in operational attributes
            //attributeInfos.Add(OperationalAttributeInfos.EXPIRE_PASSWORD);

            // now add in container ... 
            attributeInfos.Add(GetConnectorAttributeInfo(ATT_CONTAINER,
                typeof(string), true, true, false, false, ObjectClass.GROUP));

            attributeInfos.Add(PredefinedAttributeInfos.ACCOUNTS);

            // get everything else from the schema
            PopulateSchemaFromAD("Group", ADSchema, attributeInfos, null, ObjectClass.GROUP);
            return attributeInfos;
        }

        public ICollection<ConnectorAttributeInfo> GetOuAttributeInfos(
            ActiveDirectorySchema ADSchema)
        {
            ICollection<ConnectorAttributeInfo> attributeInfos = new Collection<ConnectorAttributeInfo>();

            // add in container ... 
            attributeInfos.Add(GetConnectorAttributeInfo(ATT_CONTAINER,
                typeof(string), true, true, false, false, ouObjectClass));

            // add in name ... 
            attributeInfos.Add(GetConnectorAttributeInfo(Name.NAME,
                typeof(string), true, true, true, false, ouObjectClass));

            return attributeInfos;
        }

        protected void PopulateSchemaFromAD(String className, 
            ActiveDirectorySchema ADSchema,
            ICollection<ConnectorAttributeInfo> attributeInfos,
            ICollection<String> attributesToIgnore, ObjectClass oclass)
        {
            if(attributesToIgnore == null) {
                attributesToIgnore = new List<String>();
            }
            ActiveDirectorySchemaClass schemaClass = ADSchema.FindClass(className);
            AddPropertyCollectionToSchema(schemaClass.MandatoryProperties,
                attributeInfos, false, oclass);

            AddPropertyCollectionToSchema(schemaClass.OptionalProperties,
                attributeInfos, false, oclass);
        }

        protected void AddPropertyCollectionToSchema(
            ActiveDirectorySchemaPropertyCollection schemaProperties,
            ICollection<ConnectorAttributeInfo> attributeInfos,
            Boolean required, ObjectClass oclass)
        {
            foreach (ActiveDirectorySchemaProperty schemaProperty in
                                schemaProperties)
            {
                DirectoryEntry sde = schemaProperty.GetDirectoryEntry();
                PropertyValueCollection systemOnlyPvc = sde.Properties["systemOnly"];
                Boolean writable = true;
                if (systemOnlyPvc != null)
                {
                    Object value = systemOnlyPvc.Value;
                    if ((value != null) && (value.Equals(true)))
                    {
                        writable = false;
                    }
                }

                String syntax = schemaProperty.Syntax.ToString();
                syntax = syntax.ToUpper();
                Type connectorType = typeof(string);

                // if this gets larger, break it out
                // into a special method.
                if ("BOOLEAN".Equals(syntax, StringComparison.CurrentCultureIgnoreCase))
                {
                    connectorType = typeof(bool);
                }
                else if ("INTEGER".Equals(syntax, StringComparison.CurrentCultureIgnoreCase))
                {
                    connectorType = typeof(int);
                }
                else if ("INT64".Equals(
                    syntax, StringComparison.CurrentCultureIgnoreCase))
                {
                    connectorType = typeof(long);
                }
                
                attributeInfos.Add(GetConnectorAttributeInfo(schemaProperty.Name,
                    connectorType, writable, true, required, 
                    schemaProperty.IsSingleValued ? false : true, oclass));

/*
                Console.WriteLine("***->" + schemaProperty.Name + "<-***");
                foreach (String pName in sde.Properties.PropertyNames)
                {
                    Console.WriteLine("***->" + pName + " = " + sde.Properties[pName].Value);
                }
*/
            }
        }

        private ConnectorAttributeInfo GetConnectorAttributeInfo(string name,
            Type type, bool writable, bool readable, bool required,
            bool multivalue, ObjectClass oclass)
        {
            ConnectorAttributeInfoBuilder builder = new ConnectorAttributeInfoBuilder();
            builder.Name = name;
            builder.ValueType = type;
            builder.Writeable = writable;
            builder.Readable = readable;
            builder.Required = required;
            builder.MultiValue = multivalue;

            // if there is a set of attributes to return by default
            // for this object class use it.  If not, just use the
            // the builder's default value
            if(AttributesReturnedByDefault.Keys.Contains(oclass)) {               
                if (AttributesReturnedByDefault[oclass].Contains(name))
                {
                    builder.ReturnedByDefault = true;
                }
                else
                {
                    builder.ReturnedByDefault = false;
                }
            }

            return builder.Build();
        }

        #endregion

        #region SearchOp<string> Members

        // implementation of SearchSpiOp
        public Org.IdentityConnectors.Framework.Common.Objects.Filters.FilterTranslator<string> CreateFilterTranslator(ObjectClass oclass, OperationOptions options)
        {
            return new ActiveDirectoryFilterTranslator();
        }

        // implementation of SearchSpiOp
        public void ExecuteQuery(ObjectClass oclass, string query, 
            ResultsHandler handler, OperationOptions options)
        {
            try
            {
                bool useGC = false;
                if (_configuration.SearchChildDomains)
                {
                    useGC = true;
                }

                ExecuteQuery(oclass, query, handler, options,
                    false, null, _configuration.LDAPHostName, useGC, _configuration.SearchContext);
            }
            catch (Exception e)
            {
                Trace.TraceError(String.Format("Caught Exception: {0}", e));
                throw;
            }
        }

        // this is used by the ExecuteQuere method of SearchSpiOp, and
        // by the SyncSpiOp 
        private void ExecuteQuery(ObjectClass oclass, string query,
            ResultsHandler handler, OperationOptions options, bool includeDeleted,
            SortOption sortOption, string serverName, bool useGlobalCatalog, string searchRoot)
        {
            StringBuilder fullQueryBuilder = new StringBuilder();
            if (query == null)
            {
                fullQueryBuilder.Append("(objectclass=");
                fullQueryBuilder.Append(ActiveDirectoryUtils.GetADObjectClass(oclass));
                fullQueryBuilder.Append(")");
            }
            else
            {
                fullQueryBuilder.Append("(&(objectclass=");
                fullQueryBuilder.Append(ActiveDirectoryUtils.GetADObjectClass(oclass));
                fullQueryBuilder.Append(")");
                fullQueryBuilder.Append(query);
                fullQueryBuilder.Append(")");
            }
            query = fullQueryBuilder.ToString();

            if (query == null)
            {
                Trace.TraceInformation("query is null");
            }
            else
            {
                Trace.TraceInformation("Setting search string to \'{0}\'", query);
            }

            string path;

            if (useGlobalCatalog)
            {
                path = ActiveDirectoryUtils.GetGCPath(serverName, searchRoot);
            }
            else
            {
                path = ActiveDirectoryUtils.GetLDAPPath(serverName, searchRoot);
            }

            DirectoryEntry searchRootEntry = new DirectoryEntry(path,
                _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
            DirectorySearcher searcher = new DirectorySearcher(searchRootEntry, query);
            searcher.PageSize = 1000;
            if (includeDeleted)
            {
                searcher.Tombstone = true;
            }

            if (sortOption != null)
            {
                searcher.Sort = sortOption;
            }

            SearchResultCollection resultSet = searcher.FindAll();
            ICollection<string> attributesToReturn = null;
            if (resultSet.Count > 0)
            {
                attributesToReturn = GetAttributesToReturn(oclass, options);
            }

            foreach (SearchResult result in resultSet)
            {
                Trace.TraceInformation("Found object {0}", result.Path);
                ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

                bool isDeleted = false;
                if(result.Properties.Contains(ATT_IS_DELETED)) {
                    ResultPropertyValueCollection pvc = result.Properties[ATT_IS_DELETED];
                    if(pvc.Count > 0) {
                        isDeleted = (bool)pvc[0];
                    }
                }
                
                if (isDeleted.Equals(false))
                {
                    // if we were using the global catalog (gc), we have to 
                    // now retrieve the object from a domain controller (dc) 
                    // because the gc may not have have all of the attributes,
                    // depending on which attributes are replicated to the gc.                    
                    SearchResult savedGcResult = null;
                    SearchResult savedDcResult = result;
                    if (useGlobalCatalog)
                    {
                        savedGcResult = result;

                        String dcSearchRootPath = ActiveDirectoryUtils.GetLDAPPath(
                            _configuration.LDAPHostName, searchRoot);

                        DirectoryEntry dcSearchRoot = new DirectoryEntry(dcSearchRootPath,
                            _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);

                        string dcSearchQuery = String.Format("(" + ATT_DISTINGUISHED_NAME + "={0})",
                            ActiveDirectoryUtils.GetDnFromPath(savedGcResult.Path));
                        DirectorySearcher dcSearrcher = 
                            new DirectorySearcher(dcSearchRoot, dcSearchQuery);
                        savedDcResult = dcSearrcher.FindOne();
                        if (savedDcResult == null)
                        {
                            // in this case, we found it in the gc, but not in the
                            // domain controller.  We cant return a result.  The could
                            // be a case where the account was deleted, but the global
                            // catalog doesn't know it yet
                            Trace.TraceWarning(String.Format("Found result in global catalog " + 
                                "for ''{0}'', but could not retrieve the entry from the domain " + 
                                "controller", savedGcResult.Path));
                            continue;
                        }
                    }

                    foreach (string attributeName in attributesToReturn)
                    {
                        SearchResult savedResults = savedDcResult;
                        // if we are using the global catalog, we had to get the
                        // dc's version of the directory entry, but for usnchanged, 
                        // we need the gc version of it
                        if (useGlobalCatalog && attributeName.Equals(ATT_USN_CHANGED, 
                            StringComparison.CurrentCultureIgnoreCase))
                        {
                            savedResults = savedGcResult;
                        }

                        AddAttributeIfNotNull(builder,
                            _utils.GetConnectorAttributeFromADEntry(
                            oclass, attributeName, savedResults));
                    }
                }
                else
                {
                    // get uid
                    AddAttributeIfNotNull(builder,
                        _utils.GetConnectorAttributeFromADEntry(
                        oclass, Uid.NAME, result));

                    // get uid
                    AddAttributeIfNotNull(builder,
                        _utils.GetConnectorAttributeFromADEntry(
                        oclass, Name.NAME, result));

                    // get usnchanged
                    AddAttributeIfNotNull(builder,
                        _utils.GetConnectorAttributeFromADEntry(
                        oclass, ATT_USN_CHANGED, result));

                    // add isDeleted
                    builder.AddAttribute(ATT_IS_DELETED, true);
                }

                String msg = String.Format("Returning ''{0}''",
                    (result.Path != null) ? result.Path : "<path is null>");
                Trace.TraceInformation(msg);
                handler(builder.Build());
            }
        }

        private ICollection<string> GetAttributesToReturn(ObjectClass oclass, OperationOptions options)
        {
            ICollection<string> attributeNames = new HashSet<string>();

            if ((options.AttributesToGet != null) && (options.AttributesToGet.Length > 0))
            {
                foreach (string name in options.AttributesToGet)
                {
                    attributeNames.Add(name);
                }
                // now add in operational attributes ... they are always returned
                ObjectClassInfo ocInfo = Schema().FindObjectClassInfo(oclass.GetObjectClassValue());
                foreach (ConnectorAttributeInfo info in ocInfo.ConnectorAttributeInfos)
                {
                    Trace.TraceInformation(String.Format(
                        "Adding {0} to list of returned attributes", info.Name));
                    if ((info.IsReturnedByDefault) && (ConnectorAttributeUtil.IsSpecial(info)))
                    {
                        if (!attributeNames.Contains(info.Name))
                        {
                            attributeNames.Add(info.Name);
                        }
                    }
                }
            }
            else
            {
                ObjectClassInfo ocInfo = Schema().FindObjectClassInfo(oclass.GetObjectClassValue());
                foreach (ConnectorAttributeInfo info in ocInfo.ConnectorAttributeInfos)
                {
                    Trace.TraceInformation(String.Format(
                        "Adding {0} to list of returned attributes", info.Name));
                    if (info.IsReturnedByDefault)
                    {
                        attributeNames.Add(info.Name);
                    }
                }
            }

            // Uid is always returned
            attributeNames.Add(Uid.NAME);
            return attributeNames;
        }

        private void AddAttributeIfNotNull(ConnectorObjectBuilder builder, 
            ConnectorAttribute attribute)
        {
            if (attribute != null)
            {
                builder.AddAttribute(attribute);
            }
        }

        #endregion

        #region TestOp Members

        public void Test()
        {
            _configuration.Validate();
        }

        #endregion

        #region AdvancedUpdateOp Members

        // implementation of AdvancedUpdateSpiOp
        public Uid Update(UpdateType type, ObjectClass oclass, 
            ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            Uid updatedUid = null;

            Trace.TraceInformation("Update method");
            if (_configuration == null)
            {
                throw new ConfigurationException("Connector has not been configured");
            }

            updatedUid = ConnectorAttributeUtil.GetUidAttribute(attributes);
            if (updatedUid == null)
            {
                throw new ConnectorException("Uid was not present in attributes to be modified");
            }
            
            DirectoryEntry updateEntry =
                ActiveDirectoryUtils.GetDirectoryEntryFromUid(_configuration.LDAPHostName, updatedUid,
                _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
            
            _utils.UpdateADObject(oclass, updateEntry,
                attributes, type, _configuration);

            try
            {
                Object guidValue = updateEntry.Properties["objectGUID"].Value;
                if (guidValue != null)
                {
                    // format the uid in the special way required for searching
                    String searchGuid =
                        ActiveDirectoryUtils.ConvertUIDBytesToGUIDString(
                        (Byte[])guidValue);

                    Trace.TraceInformation("Created user with uid {0}", searchGuid);
                    updatedUid = new Uid(searchGuid);
                }
                else
                {
                    Trace.TraceError("Unable to find uid attribute for newly created object");
                }
            }
            catch (DirectoryServicesCOMException exception)
            {
                // have to make sure the new thing gets deleted in 
                // the case of error
                Console.WriteLine("caught exception:" + exception);
                return null;
            }
            return updatedUid;
        }

        #endregion

        #region DeleteOp Members

        // implementation of DeleteSpiOp
        public void Delete(ObjectClass objClass, Uid uid, OperationOptions options)
        {
            DirectoryEntry de = null;

            de = ActiveDirectoryUtils.GetDirectoryEntryFromUid(_configuration.LDAPHostName, uid, 
                _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);

            if (objClass.Equals(ObjectClass.ACCOUNT))
            {
                // if it's a user account, get the parent's child list
                // and remove this entry
                DirectoryEntry parent = de.Parent;
                parent.Children.Remove(de);
            }
            else if (objClass.Equals(ObjectClass.GROUP))
            {
                // if it's a group (container), delete this
                // entry and all it's children
                de.DeleteTree();
            }
            else
            {
                throw new ConnectorException(String.Format("Delete is not supported for ObjectClass {0}", objClass.GetObjectClassValue()));
            }
        }

        #endregion


        #region ScriptOnResourceOp Members

        public object RunScriptOnResource(ScriptContext request, OperationOptions options)
        {
            IDictionary<string, object> arguments = new Dictionary<string, object>(request.ScriptArguments);
            // per Will D.  batch scripts need special parameters set, but other scripts 
            // don't.  He doesn't feel that this can be changed at present, so setting 
            // the parameters here.

            // Cant find a constant for the string to represent the shell script executor,
            // replace embedded string constant if one turns up.
            if (request.ScriptLanguage.Equals("Shell", StringComparison.CurrentCultureIgnoreCase))
            {
                if (options.RunAsUser != null)
                {
                    arguments.Add("USERNAME", options.RunAsUser);
                    arguments.Add("PASSWORD", 
                        ActiveDirectoryUtils.GetSecureString(options.RunWithPassword));
                }
            }

            
            ScriptExecutorFactory factory = ScriptExecutorFactory.NewInstance(request.ScriptLanguage);
            ScriptExecutor executor = factory.NewScriptExecutor(new Assembly[0],request.ScriptText, true);
            return executor.Execute(arguments);
        }

        #endregion

        #region SyncOp Members

        // implementation of SyncSpiOp
        public class SyncResults
        {
            SyncResultsHandler _syncResultsHandler;
            string _serverName;
            bool _useGlobalCatalog;
            long _lastModifiedUsn;
            long _lastDeletedUsn;

            public SyncResults(SyncResultsHandler syncResultsHandler, 
                bool useGlobalCatalog, string serverName, 
                long lastModifiedUsn, long lastDeletedUsn) {
                _syncResultsHandler = syncResultsHandler;
                _serverName = serverName;
                _useGlobalCatalog = false;
                _lastModifiedUsn = lastModifiedUsn;
                _lastDeletedUsn = lastDeletedUsn;
            }

            public bool SyncHandler(ConnectorObject obj)
            {
                SyncDeltaBuilder builder = new SyncDeltaBuilder();
                builder.Object = obj;
                ConnectorAttribute tokenAttr = 
                    ConnectorAttributeUtil.Find(ATT_USN_CHANGED, obj.GetAttributes());
                if(tokenAttr == null) {
                    string msg = String.Format("Attribute {0} is not present " +
                        "in connector object.  Cannot proceed with synchronization", ATT_USN_CHANGED);
                    Trace.TraceError(msg);
                    throw new ConnectorException(msg);
                }
                long tokenUsnValue = (long)ConnectorAttributeUtil.GetSingleValue(tokenAttr);
                string tokenServerName = _serverName;
                
                bool? isDeleted = false;
                ConnectorAttribute isDeletedAttr =
                    ConnectorAttributeUtil.Find(ATT_IS_DELETED, obj.GetAttributes());
                if (isDeletedAttr != null)
                {
                    isDeleted = (bool?)ConnectorAttributeUtil.GetSingleValue(isDeletedAttr);
                    _lastDeletedUsn = tokenUsnValue;
                }
                else
                {
                    _lastModifiedUsn = tokenUsnValue;
                }

                SyncToken token = new SyncToken(String.Format("{0}|{1}|{2}|{3}",
                    _lastModifiedUsn, _lastDeletedUsn, _useGlobalCatalog, tokenServerName));
                builder.Token = token;

                if ((isDeleted != null) && (isDeleted.Equals(true)))
                {
                    builder.DeltaType = SyncDeltaType.DELETE;
                }
                else
                {
                    builder.DeltaType = SyncDeltaType.UPDATE;
                }

                builder.Uid = obj.Uid;
                _syncResultsHandler(builder.Build());
                return true;
            }
        }

        public void Sync(ObjectClass objClass, SyncToken token, 
            SyncResultsHandler handler, OperationOptions options)
        {
            string modifiedQuery = null;
            string deletedQuery = null;
            string serverName = null;
            bool useGlobalCatalog = false;

            if (_configuration.SearchChildDomains)
            {
                serverName = _configuration.SyncGlobalCatalogServer;
                useGlobalCatalog = true;
            }
            else
            {
                serverName = _configuration.SyncDomainController;
            }

            if ((serverName == null) || (serverName.Length == 0))
            {
                Trace.TraceWarning("No server was configured for synchronization, so picking one.  You should configure a server for best performance.");
                // we have to know which server we are working against,
                // so find one.
                if (useGlobalCatalog)
                {
                    DirectoryContext context = new DirectoryContext(
                        DirectoryContextType.Forest, _configuration.DomainName,
                        _configuration.DirectoryAdminName,
                        _configuration.DirectoryAdminPassword);
                    GlobalCatalog gc = GlobalCatalog.FindOne(context);
                    _configuration.SyncGlobalCatalogServer = gc.ToString();
                    serverName = _configuration.SyncGlobalCatalogServer;
                }
                else
                {
                    DirectoryContext context = new DirectoryContext(
                        DirectoryContextType.Domain, _configuration.DomainName,
                        _configuration.DirectoryAdminName,
                        _configuration.DirectoryAdminPassword);
                    DomainController controller = DomainController.FindOne(context);
                    _configuration.SyncDomainController = controller.ToString();
                    serverName = _configuration.SyncDomainController;
                }
            }


            long lastModToken = 0;
            long lastDelToken = 0;
            // if the token is not null, we may be able to start from 
            // the usn contained there
            if (token != null)
            {
                string[] tokenParts = ((string)(token.Value)).Split('|');
                bool tokenUseGlobalCatalog = bool.Parse(tokenParts[2]);
                string tokenServerName = tokenParts[3];

                // If the token server is the same as the configured server,
                // use the token value (usn) to limit the query.  The token is
                // server specific though, so we cant use the usn if it didn't come
                // from this server.
                // If no server is configured, just try to use what we used last time.
                if (tokenServerName != null)
                {
                    if ((serverName == null) ||
                        (tokenServerName.Equals(serverName)) || (serverName.Length == 0))
                    {
                        lastModToken = long.Parse(tokenParts[0]);
                        lastDelToken = long.Parse(tokenParts[1]);
                        modifiedQuery = string.Format("(!({0}<={1}))", ATT_USN_CHANGED, tokenParts[0]);
                        deletedQuery = string.Format("(&(!({0}<={1}))(isDeleted=TRUE))", ATT_USN_CHANGED, tokenParts[1]);
                    }
                }
            }
            else
            {
                deletedQuery = string.Format("(isDeleted=TRUE)");
            }

            OperationOptionsBuilder builder = new OperationOptionsBuilder();
            SyncResults syncResults = new SyncResults(handler, useGlobalCatalog, 
                serverName, lastModToken, lastDelToken);

            // find modified usn's
            ExecuteQuery(objClass, modifiedQuery, syncResults.SyncHandler, builder.Build(),
                false, new SortOption(ATT_USN_CHANGED, SortDirection.Ascending),
                serverName, useGlobalCatalog, _configuration.SyncSearchContext);

            // find deleted usn's
            DirectoryContext domainContext = new DirectoryContext(DirectoryContextType.DirectoryServer, 
                        serverName,
                        _configuration.DirectoryAdminName,
                        _configuration.DirectoryAdminPassword);
            Domain domain = Domain.GetDomain(domainContext);
            String deleteObjectsSearchRoot = null;
            if (domain != null)
            {
                DirectoryEntry domainDe = domain.GetDirectoryEntry();
                deleteObjectsSearchRoot = ActiveDirectoryUtils.GetDnFromPath(domainDe.Path);
            }
            ExecuteQuery(objClass, deletedQuery, syncResults.SyncHandler, builder.Build(),
                true, new SortOption(ATT_USN_CHANGED, SortDirection.Ascending),
                serverName, useGlobalCatalog, deleteObjectsSearchRoot);

        }

        #endregion

        #region AuthenticateOp Members

        public void Authenticate(string username, 
            Org.IdentityConnectors.Common.Security.GuardedString password, 
            OperationOptions options)
        {
            PasswordChangeHandler handler = new PasswordChangeHandler();
            String ldapEntryPath = ActiveDirectoryUtils.GetLDAPPath(_configuration.LDAPHostName, 
                username);

            DirectoryEntry directoryEntry = new DirectoryEntry(ldapEntryPath,
                _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);

            handler.Authenticate(directoryEntry, username, password);
        }

        #endregion

        #region AttributeNormalizer Members

        public ConnectorAttribute NormalizeAttribute(ObjectClass oclass, ConnectorAttribute attribute)
        {
            // if this gets big, use deleagates, but for now, just
            // handle individual attirbutes;
            if (attribute is Uid)
            {
                StringBuilder normalizedUidValue = new StringBuilder();
                String uidValue = ((Uid)attribute).GetUidValue();
                // convert to upper case
                if (uidValue != null)
                {
                    uidValue = uidValue.ToUpper();
                    
                    // now remove spaces
                    foreach(Char nextChar in uidValue) {
                        if(!nextChar.Equals(" ")) {
                            normalizedUidValue.Append(nextChar);
                        }
                    }

                    return new Uid(normalizedUidValue.ToString());
                }
                else 
                { 
                    return attribute;               
                }
            }
            else if (attribute is Name)
            {
                String nameValue = ((Name)attribute).GetNameValue();
                return ConnectorAttributeBuilder.Build(attribute.Name,
                    ActiveDirectoryUtils.NormalizeLdapString(nameValue));
            }

            return attribute;
        }

        #endregion

        #region PoolableConnector Members

        public void CheckAlive()
        {
            return;
        }

        #endregion
    }
}
