/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
using System;
using System.Reflection;
using ActiveDs;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using Org.IdentityConnectors.Common;
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
    public enum UpdateType {
        ADD,
        DELETE,
        REPLACE
    }
    
    
    /// <summary>
    /// The Active Directory Connector
    /// </summary>
    [ConnectorClass("connector_displayName",
                      typeof(ActiveDirectoryConfiguration),
                      MessageCatalogPaths = new String[]{"Org.IdentityConnectors.ActiveDirectory.Messages"}
                      )]    
    public class ActiveDirectoryConnector : CreateOp, Connector, SchemaOp, DeleteOp,
        SearchOp<String>, TestOp, UpdateAttributeValuesOp, ScriptOnResourceOp, SyncOp, 
        AuthenticateOp, AttributeNormalizer, PoolableConnector
	{
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
        public static readonly string ATT_PWD_LAST_SET = "pwdLastSet";
        public static readonly string ATT_ACCOUNT_EXPIRES = "accountExpires";
        public static readonly string ATT_LOCKOUT_TIME = "lockoutTime";
        public static readonly string ATT_GROUP_TYPE = "groupType";
        public static readonly string ATT_DESCRIPTION = "description";
        public static readonly string ATT_SHORT_NAME = "name";
        public static readonly string ATT_DISPLAY_NAME = "displayName";
        public static readonly string ATT_USER_ACOUNT_CONTROL = "userAccountControl";
        public static readonly string ATT_PASSWORD_NEVER_EXPIRES = "PasswordNeverExpires";
        public static readonly string ATT_ACCOUNTS = ConnectorAttributeUtil.CreateSpecialName("ACCOUNTS");
        public static readonly string OBJECTCLASS_OU = "organizationalUnit";
        public static readonly string OBJECTCLASS_GROUP = "Group";
        public static readonly string OPTION_DOMAIN = "w2k_domain";
        public static readonly string OPTION_RETURN_UID_ONLY = "returnUidOnly";

        public static readonly ObjectClass ouObjectClass = new ObjectClass(OBJECTCLASS_OU);
        public static readonly ObjectClass groupObjectClass = new ObjectClass(OBJECTCLASS_GROUP);

        private static readonly string OLD_SEARCH_FILTER_STRING = "Search Filter String";
        private static readonly string OLD_SEARCH_FILTER = "searchFilter";

        ActiveDirectoryConfiguration _configuration = null;
        ActiveDirectoryUtils _utils = null;
        private static Schema _schema = null;
        public ActiveDirectoryConnector()
        {
            // populate default attributes and Schema
            Schema();
        }

        #region CreateOp Members
        // implementation of CreateSpiOp
        public virtual Uid Create(ObjectClass oclass, 
            ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            Uid uid = null;
            bool created = false;
            DirectoryEntry newDe = null;

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
                throw new ConfigurationException(_configuration.ConnectorMessages.Format(
                    "ex_ConnectorNotConfigured", "Connector has not been configured"));
            }
            Name nameAttribute = ConnectorAttributeUtil.GetNameFromAttributes(attributes);
            if (nameAttribute == null)
            {
                throw new ConnectorException(
                    _configuration.ConnectorMessages.Format("ex_OperationalAttributeNull", 
                        "The name operational attribute cannot be null"));
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
                newDe = containerDe.Children.Add(
                    ActiveDirectoryUtils.GetRelativeName(nameAttribute),
                    _utils.GetADObjectClass(oclass));

                if (oclass.Equals(ActiveDirectoryConnector.groupObjectClass))
                {
                    ConnectorAttribute groupAttribute = 
                        ConnectorAttributeUtil.Find(ActiveDirectoryConnector.ATT_GROUP_TYPE, attributes);
                    if (groupAttribute != null)
                    {
                        int? groupType = ConnectorAttributeUtil.GetIntegerValue(groupAttribute);
                        if (groupType.HasValue)
                        {
                            newDe.Properties[ActiveDirectoryConnector.ATT_GROUP_TYPE].Value = groupType;
                        }
                    }
                }

                newDe.CommitChanges();
                created = true;
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
                if (created)
                {
                    // In the case of an exception, make sure we
                    // don't leave any partial objects around
                    newDe.DeleteTree();
                }
                throw;
            }
            catch(Exception exception)
            {
                Console.WriteLine("caught exception:" + exception);
                Trace.TraceError(exception.Message);
                if (created)
                {
                    // In the case of an exception, make sure we
                    // don't leave any partial objects around
                    newDe.DeleteTree();
                }
                throw;
            }

            if (!oclass.Equals(ObjectClass.ACCOUNT))
            {
                // uid will be the dn for non account objects
                String dnUid = nameAttribute.GetNameValue();
                if((dnUid != null) && (dnUid.Length > 0))
                {
                    dnUid = ActiveDirectoryUtils.NormalizeLdapString(dnUid);
                }
                return new Uid(dnUid);
            }
            return uid;
        }

        #endregion

        #region Connector Members

        // implementation of Connector
        public virtual void Init(Configuration configuration)
        {
            Trace.TraceInformation("Active Directory Init method");
            _configuration = (ActiveDirectoryConfiguration)configuration;
            _utils = new ActiveDirectoryUtils(_configuration);
        }

        #endregion

        #region IDisposable Members

        public virtual void Dispose()
        {
        }

        #endregion

        protected ICollection<string> GetDefaultAttributeListForObjectClass(
            ObjectClass oclass, ObjectClassInfo oclassInfo)
        {
            ICollection<string> defaultAttributeList = new List<string>();

            foreach (ConnectorAttributeInfo attInfo in oclassInfo.ConnectorAttributeInfos)
            {
                if (attInfo.IsReturnedByDefault)
                {
                    defaultAttributeList.Add(attInfo.Name);
                }
            }

            return defaultAttributeList;
        }

        #region SchemaOp Members
        // implementation of SchemaSpiOp
        public Schema Schema()
        {            
            Trace.TraceInformation("Schema method");
            if (_schema != null)
            {
                Trace.TraceInformation("Returning cached schema");
                return _schema;
            }

            SchemaBuilder schemaBuilder = 
                new SchemaBuilder(SafeType<Connector>.Get(this));
            AttributesReturnedByDefault = new Dictionary<ObjectClass, ICollection<string>>();
            
            //iterate through supported object classes
            foreach(ObjectClass oc in GetSupportedObjectClasses())
            {
                ObjectClassInfo ocInfo = GetObjectClassInfo(oc);
                Assertions.NullCheck(ocInfo, "ocInfo");

                //populate the list of default attributes to get
                AttributesReturnedByDefault.Add(oc, new HashSet<string>());
                foreach (ConnectorAttributeInfo caInfo in ocInfo.ConnectorAttributeInfos)
                {
                    if( caInfo.IsReturnedByDefault ) {
                        AttributesReturnedByDefault[oc].Add(caInfo.Name);
                    }
                }

                //add object class to schema
                schemaBuilder.DefineObjectClass(ocInfo);

                //add supported operations
                IList<SafeType<SPIOperation>> supportedOps = GetSupportedOperations(oc);
                if (supportedOps != null)
                {
                    foreach (SafeType<SPIOperation> op in supportedOps)
                    {                        
                        schemaBuilder.AddSupportedObjectClass(op, ocInfo);
                    }
                }

                //remove unsupported operatons
                IList<SafeType<SPIOperation>> unSupportedOps = GetUnSupportedOperations(oc);
                if (unSupportedOps != null)
                {
                    foreach (SafeType<SPIOperation> op in unSupportedOps)
                    {
                        schemaBuilder.RemoveSupportedObjectClass(op, ocInfo);
                    }
                }
            }
            Trace.TraceInformation("Finished retrieving schema");
            _schema = schemaBuilder.Build();
            Trace.TraceInformation("Returning schema");

            return _schema;
        }

        /// <summary>
        /// Defines the supported object classes by the connector, used for schema building
        /// </summary>
        /// <returns>List of supported object classes</returns>
        protected virtual ICollection<ObjectClass> GetSupportedObjectClasses()
        {
            IDictionary<ObjectClass, ObjectClassInfo> objectClassInfos = 
                    CommonUtils.GetOCInfo("Org.IdentityConnectors.ActiveDirectory.ObjectClasses.xml");

            return objectClassInfos.Keys;
		}

        /// <summary>
        /// Gets the object class info for specified object class, used for schema building
        /// </summary>
        /// <param name="oc">ObjectClass to get info for</param>
        /// <returns>ObjectClass' ObjectClassInfo</returns>
        protected virtual ObjectClassInfo GetObjectClassInfo(ObjectClass oc)
        {
            IDictionary<ObjectClass, ObjectClassInfo> objectClassInfos = 
                    CommonUtils.GetOCInfo("Org.IdentityConnectors.ActiveDirectory.ObjectClasses.xml");

            return objectClassInfos[oc];
        }

        /// <summary>
        /// Gets the list of supported operations by the object class, used for schema building
        /// </summary>
        /// <param name="oc"></param>
        /// <returns></returns>
        protected virtual IList<SafeType<SPIOperation>> GetSupportedOperations(ObjectClass oc)
        {
            return null;
        }

        /// <summary>
        /// Gets the list of UNsupported operations by the object class, used for schema building
        /// </summary>
        /// <param name="oc"></param>
        /// <returns></returns>
        protected virtual IList<SafeType<SPIOperation>> GetUnSupportedOperations(ObjectClass oc)
        {
            if (oc.Equals(ActiveDirectoryConnector.groupObjectClass) || oc.Equals(ouObjectClass))
            {
                return new List<SafeType<SPIOperation>> {
                    SafeType<SPIOperation>.Get<AuthenticateOp>(),
                    SafeType<SPIOperation>.Get<SyncOp>()};         
            }

            return null;
        }

        #endregion

        #region SearchOp<string> Members

        // implementation of SearchSpiOp
        public virtual Org.IdentityConnectors.Framework.Common.Objects.Filters.FilterTranslator<string> CreateFilterTranslator(ObjectClass oclass, OperationOptions options)
        {
            return new ActiveDirectoryFilterTranslator();
        }

        // implementation of SearchSpiOp
        public virtual void ExecuteQuery(ObjectClass oclass, string query, 
            ResultsHandler handler, OperationOptions options)
        {
            try
            {                
                bool useGC = false;
                if (_configuration.SearchChildDomains)
                {
                    useGC = true;
                }

                IDictionary<string, object>searchOptions = options.Options;

                SearchScope searchScope = GetADSearchScopeFromOptions(options);
                string searchContainer = GetADSearchContainerFromOptions(options);

                // for backward compatibility, support old query style from resource adapters
                // but log a warning
                if((query == null) || (query.Length == 0)) {
                    if ((options != null) && (options.Options != null))
                    {
                        Object oldStyleQuery = null;
                        if (options.Options.Keys.Contains(OLD_SEARCH_FILTER_STRING))
                        {
                            oldStyleQuery = options.Options[OLD_SEARCH_FILTER_STRING];
                        }
                        else if (options.Options.Keys.Contains(OLD_SEARCH_FILTER))
                        {
                            oldStyleQuery = options.Options[OLD_SEARCH_FILTER];
                        }
                        if ((oldStyleQuery != null) && (oldStyleQuery is string))
                        {
                            query = (string)oldStyleQuery;
                            Trace.TraceWarning(_configuration.ConnectorMessages.Format(
                                "warn_CompatibilityModeQuery",
                                "Using Identity Manger Resource Adapter style query ''{0}''.  This should be updated to use the new connector query syntax.",
                                ((query != null) && (query.Length > 0)) ? query : ""));
                        }
                    }
                }

                ExecuteQuery(oclass, query, handler, options,
                    false, null, _configuration.LDAPHostName, useGC, searchContainer, searchScope);
            }
            catch (Exception e)
            {
                Trace.TraceError(String.Format("Caught Exception: {0}", e));
                throw;
            }
        }

        public string GetADSearchContainerFromOptions(OperationOptions options)
        {
            if (options != null)
            {
                QualifiedUid qUid = options.getContainer;
                if (qUid != null)
                {
                    return ConnectorAttributeUtil.GetStringValue(qUid.Uid);
                }
            }

            return _configuration.Container;
        }

        public SearchScope GetADSearchScopeFromOptions(OperationOptions options)
        {
            if (options != null)
            {
                string scope = options.Scope;
                if (scope != null)
                {
                    if (scope.Equals(OperationOptions.SCOPE_ONE_LEVEL))
                    {
                        return SearchScope.OneLevel;
                    }
                    else if (scope.Equals(OperationOptions.SCOPE_SUBTREE))
                    {
                        return SearchScope.Subtree;
                    }
                    else if (scope.Equals(OperationOptions.SCOPE_OBJECT))
                    {
                        return SearchScope.Base;
                    }
                    else
                    {
                        throw new ConnectorException(_configuration.ConnectorMessages.Format(
                            "ex_invalidSearchScope", "An invalid search scope was specified: {0}", scope));
                    }
                }
            }

            // default value is subtree;
            return SearchScope.Subtree;
        }

        // this is used by the ExecuteQuery method of SearchSpiOp, and
        // by the SyncSpiOp 
        private void ExecuteQuery(ObjectClass oclass, string query,
            ResultsHandler handler, OperationOptions options, bool includeDeleted,
            SortOption sortOption, string serverName, bool useGlobalCatalog, 
            string searchRoot, SearchScope searchScope)
        {
            Trace.TraceInformation("Search: modifying query");
            StringBuilder fullQueryBuilder = new StringBuilder();
            if (query == null)
            {
                fullQueryBuilder.Append("(objectclass=");
                fullQueryBuilder.Append(_utils.GetADObjectClass(oclass));
                fullQueryBuilder.Append(")");
            }
            else
            {
                fullQueryBuilder.Append("(&(objectclass=");
                fullQueryBuilder.Append(_utils.GetADObjectClass(oclass));
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
                // for backward compatibility ...
                if((ObjectClass.ACCOUNT.Equals(oclass)) && (!includeDeleted))
                {
                    query = String.Format("(&(ObjectCategory=Person){0})", query);
                }

                Trace.TraceInformation("Setting search string to \'{0}\'", query);
            }

            string path;
            path = GetSearchContainerPath(useGlobalCatalog, serverName, searchRoot);

            Trace.TraceInformation("Search: Getting root node for search");
            DirectoryEntry searchRootEntry = new DirectoryEntry(path,
                _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
            DirectorySearcher searcher = new DirectorySearcher(searchRootEntry, query);
            searcher.PageSize = 1000;
            searcher.SearchScope = searchScope;
            
            if (includeDeleted)
            {
                searcher.Tombstone = true;
            }

            if (sortOption != null)
            {
                searcher.Sort = sortOption;
            }

            Trace.TraceInformation("Search: Performing query");
            SearchResultCollection resultSet = searcher.FindAll();
            Trace.TraceInformation("Search: found {0} results", resultSet.Count);
            ICollection<string> attributesToReturn = null;
            if (resultSet.Count > 0)
            {
                attributesToReturn = GetAttributesToReturn(oclass, options);
            }

            Trace.TraceInformation("Building connectorObjects");
            foreach (SearchResult result in resultSet)
            {
                try
                {
                    Trace.TraceInformation("Found object {0}", result.Path);
                    ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                    builder.ObjectClass = oclass;

                    bool isDeleted = false;
                    if (result.Properties.Contains(ATT_IS_DELETED))
                    {
                        ResultPropertyValueCollection pvc = result.Properties[ATT_IS_DELETED];
                        if (pvc.Count > 0)
                        {
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
                            DirectorySearcher dcSearcher =
                                new DirectorySearcher(dcSearchRoot, dcSearchQuery);
                            savedDcResult = dcSearcher.FindOne();
                            if (savedDcResult == null)
                            {
                                // in this case, there is no choice, but to use
                                // what is in the global catalog.  We would have 
                                //liked to have read from the regular ldap, but there
                                // is not one.  This is the case for domainDNS objects
                                // (at least for child domains in certain or maybe all
                                // circumstances).
                                savedDcResult = savedGcResult;
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
                catch (DirectoryServicesCOMException e)
                {
                    // there is a chance that we found the result, but
                    // in the mean time, it was deleted.  In that case, 
                    // log an error and continue
                    Trace.TraceWarning("Error in creating ConnectorObject from DirectoryEntry.  It may have been deleted during search.");
                    Trace.TraceWarning(e.Message);
                }
                catch (Exception e)
                {
                    // In that case, of any error, try to continue
                    Trace.TraceWarning("Error in creating ConnectorObject from DirectoryEntry.");
                    Trace.TraceWarning(e.Message);
                }
            }
        }

        // this is the path that all searches come from unless otherwise directed
        private string GetSearchContainerPath()
        {
            return GetSearchContainerPath(UseGlobalCatalog(), _configuration.LDAPHostName, _configuration.Container);
        }
      
        private string GetSearchContainerPath(bool useGC, string hostname, string searchContainer)
        {
            String path; 

            if (useGC)
            {
                path = ActiveDirectoryUtils.GetGCPath(hostname, searchContainer);
            }
            else
            {
                path = ActiveDirectoryUtils.GetLDAPPath(hostname, searchContainer);
            }

            return path;
        }

        private ICollection<string> GetAttributesToReturn(ObjectClass oclass, OperationOptions options)
        {
            ICollection<string> attributeNames = null;

            if ((options.AttributesToGet != null) && (options.AttributesToGet.Length > 0))
            {
                attributeNames = new HashSet<string>(options.AttributesToGet);
            }
            else
            {
                attributeNames = AttributesReturnedByDefault[oclass];
            }

            // Uid and name are always returned
            attributeNames.Add(Uid.NAME);
            attributeNames.Add(Name.NAME);
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

        public virtual void Test()
        {
            _configuration.Validate();

            bool objectFound = true;
            // now make sure they specified a valid value for the User Object Class
            ActiveDirectorySchema ADSchema = _utils.GetADSchema();
            ActiveDirectorySchemaClass ADSchemaClass = null;
            try
            {
                ADSchemaClass = ADSchema.FindClass(_configuration.ObjectClass);

            }
            catch (ActiveDirectoryObjectNotFoundException exception)
            {
                objectFound = false;
            }
            if ((!objectFound) || (ADSchemaClass == null))
            {
                throw new ConnectorException(
                    _configuration.ConnectorMessages.Format(
                    "ex_InvalidObjectClassInConfiguration",
                    "Invalid Object Class was specified in the connector configuration.  Object Class \'{0}\' was not found in Active Directory",
                    _configuration.ObjectClass));
            }

            // see if SearchContainer is valid
            if (!DirectoryEntry.Exists(GetSearchContainerPath()))
            {
                throw new ConnectorException(
                    _configuration.ConnectorMessages.Format(
                    "ex_InvalidSearchContainerInConfiguration",
                    "An invalid search container was supplied:  {0}",
                    _configuration.Container));
            }

            // see if the Context exists 
            
            if (!DirectoryEntry.Exists(GetSearchContainerPath(UseGlobalCatalog(), 
                _configuration.LDAPHostName, _configuration.Container)))
            {
                throw new ConnectorException(
                    _configuration.ConnectorMessages.Format(
                    "ex_InvalidContainerInConfiguration",
                    "An invalid container was supplied:  {0}",
                    _configuration.Container));
            }
        }

        #endregion

        #region AdvancedUpdateOp Members
        public Uid Update(ObjectClass objclass, Uid uid, ICollection<ConnectorAttribute> attrs, OperationOptions options) {
            return Update(UpdateType.REPLACE,objclass,ConnectorAttributeUtil.AddUid(attrs,uid),options);
        }
        
        public Uid AddAttributeValues(ObjectClass objclass,
                Uid uid,
                ICollection<ConnectorAttribute> valuesToAdd,
                OperationOptions options) {
            return Update(UpdateType.ADD,objclass,ConnectorAttributeUtil.AddUid(valuesToAdd, uid),options);
        }
    
        public Uid RemoveAttributeValues(ObjectClass objclass,
                Uid uid,
                ICollection<ConnectorAttribute> valuesToRemove,
                OperationOptions options) {
            return Update(UpdateType.DELETE,objclass,ConnectorAttributeUtil.AddUid(valuesToRemove, uid),options);
        }

        // implementation of AdvancedUpdateSpiOp
        public virtual Uid Update(UpdateType type, ObjectClass oclass, 
            ICollection<ConnectorAttribute> attributes, OperationOptions options)
        {
            Uid updatedUid = null;

            Trace.TraceInformation("Update method");
            if (_configuration == null)
            {
                throw new ConfigurationException(_configuration.ConnectorMessages.Format(
                    "ex_ConnectorNotConfigured", "Connector has not been configured"));
            }

            updatedUid = ConnectorAttributeUtil.GetUidAttribute(attributes);
            if (updatedUid == null)
            {
                throw new ConnectorException(_configuration.ConnectorMessages.Format(
                    "ex_UIDNotPresent", "Uid was not present"));
            }
            
            DirectoryEntry updateEntry =
                ActiveDirectoryUtils.GetDirectoryEntryFromUid(_configuration.LDAPHostName, updatedUid,
                _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
            
            _utils.UpdateADObject(oclass, updateEntry,
                attributes, type, _configuration);

            if(!ObjectClass.ACCOUNT.Equals(oclass)) {
                // other objects use dn as guid for idm backward compatibility
                String dnUid = (string)updateEntry.Properties["distinguishedName"][0];
                if ((dnUid != null) && (dnUid.Length > 0))
                {
                    updatedUid = new Uid(ActiveDirectoryUtils.NormalizeLdapString(dnUid));
                }
            }
            return updatedUid;
        }

        #endregion

        #region DeleteOp Members

        // implementation of DeleteSpiOp
        public virtual void Delete(ObjectClass objClass, Uid uid, OperationOptions options)
        {
            DirectoryEntry de = null;
            try
            {
                de = ActiveDirectoryUtils.GetDirectoryEntryFromUid(_configuration.LDAPHostName, uid,
                    _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
            }
            catch (System.DirectoryServices.DirectoryServicesCOMException e)
            {
                // if it's not found, throw that, else just rethrow
                if (e.ErrorCode == -2147016656)
                {
                    throw new UnknownUidException();
                }
                throw;
            }

            if (objClass.Equals(ObjectClass.ACCOUNT))
            {
                // if it's a user account, get the parent's child list
                // and remove this entry
                DirectoryEntry parent = de.Parent;
                parent.Children.Remove(de);
            }
            else
            {
                // translate the object class.  We dont care what
                // it is, but this will throw the correct exception
                // if it's an invalid one.
                _utils.GetADObjectClass(objClass);
                // delete this entry and all it's children
                de.DeleteTree();
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
                IDictionary<string, object> shellArguments = new Dictionary<string, object>();
                String shellPrefix = "";
                if(options.Options.ContainsKey("variablePrefix"))
                {
                    shellPrefix = (string)options.Options["variablePrefix"];
                }

                foreach (String argumentName in arguments.Keys)
                {
                    shellArguments.Add((shellPrefix + argumentName), arguments[argumentName]);    
                }

                arguments = shellArguments;

                if (options.RunAsUser != null)
                {
                    arguments.Add("USERNAME", options.RunAsUser);
                    arguments.Add("PASSWORD", 
                                  options.RunWithPassword.ToSecureString());
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
            ActiveDirectorySyncToken _adSyncToken;
            ActiveDirectoryConfiguration _configuration;

            internal SyncResults(SyncResultsHandler syncResultsHandler, 
                ActiveDirectorySyncToken adSyncToken, ActiveDirectoryConfiguration configuration) {
                _syncResultsHandler = syncResultsHandler;
                _adSyncToken = adSyncToken;
                _configuration = configuration;
            }

            public bool SyncHandler(ConnectorObject obj)
            {
                SyncDeltaBuilder builder = new SyncDeltaBuilder();
                ICollection<ConnectorAttribute> attrs = new HashSet<ConnectorAttribute>();
                foreach(ConnectorAttribute attribute in obj.GetAttributes()) {
                    // add all attributes to the object except the
                    // one used to flag deletes.
                    if (!attribute.Name.Equals(ATT_IS_DELETED))
                    {                       
                        attrs.Add(attribute);
                    }
                }

                ConnectorObjectBuilder coBuilder = new ConnectorObjectBuilder();
                coBuilder.SetName(obj.Name);
                coBuilder.SetUid(obj.Uid);
                coBuilder.AddAttributes(attrs);
                builder.Object = coBuilder.Build();

                ConnectorAttribute tokenAttr = 
                    ConnectorAttributeUtil.Find(ATT_USN_CHANGED, obj.GetAttributes());
                if(tokenAttr == null) {
                    string msg = _configuration.ConnectorMessages.Format("ex_missingSyncAttribute", 
                        "Attribute {0} is not present in connector object.  Cannot proceed with Synchronization", 
                        ATT_USN_CHANGED);
                    Trace.TraceError(msg);
                    throw new ConnectorException(msg);
                }
                long tokenUsnValue = (long)ConnectorAttributeUtil.GetSingleValue(tokenAttr);
                
                bool? isDeleted = false;
                ConnectorAttribute isDeletedAttr =
                    ConnectorAttributeUtil.Find(ATT_IS_DELETED, obj.GetAttributes());
                if (isDeletedAttr != null)
                {
                    isDeleted = (bool?)ConnectorAttributeUtil.GetSingleValue(isDeletedAttr);
                    _adSyncToken.LastDeleteUsn = tokenUsnValue;                    
                }
                else
                {
                    _adSyncToken.LastModifiedUsn = tokenUsnValue;
                }

                builder.Token = _adSyncToken.GetSyncToken();

                if ((isDeleted != null) && (isDeleted.Equals(true)))
                {
                    builder.DeltaType = SyncDeltaType.DELETE;
                }
                else
                {
                    builder.DeltaType = SyncDeltaType.CREATE_OR_UPDATE;
                }

                builder.Uid = obj.Uid;
                _syncResultsHandler(builder.Build());
                return true;
            }
        }

        public virtual void Sync(ObjectClass objClass, SyncToken token, 
            SyncResultsHandler handler, OperationOptions options)
        {
            if (!ObjectClass.ACCOUNT.Equals(objClass))
            {
                throw new ConnectorException(_configuration.ConnectorMessages.Format(
                    "ex_SyncNotAvailable",
                    "Sync operation is not available for ObjectClass {0}", 
                    objClass.GetObjectClassValue()));
            }

            String serverName = GetSyncServerName();

            ActiveDirectorySyncToken adSyncToken = 
                new ActiveDirectorySyncToken(token, serverName, UseGlobalCatalog());

            string modifiedQuery = GetSyncUpdateQuery(adSyncToken);
            string deletedQuery = GetSyncDeleteQuery(adSyncToken);

            OperationOptionsBuilder builder = new OperationOptionsBuilder();
            SyncResults syncResults = new SyncResults(handler, adSyncToken, _configuration);

            // find modified usn's
            ExecuteQuery(objClass, modifiedQuery, syncResults.SyncHandler, builder.Build(),
                false, new SortOption(ATT_USN_CHANGED, SortDirection.Ascending),
                serverName, UseGlobalCatalog(), GetADSearchContainerFromOptions(null), SearchScope.Subtree);

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
                serverName, UseGlobalCatalog(), deleteObjectsSearchRoot, SearchScope.Subtree);

        }

        public virtual SyncToken GetLatestSyncToken(ObjectClass objectClass)
        {
            string serverName = GetSyncServerName();
            long highestCommittedUsn = 0;
            bool useGlobalCatalog = UseGlobalCatalog();
            if (useGlobalCatalog)
            {
                DirectoryContext context = new DirectoryContext(DirectoryContextType.DirectoryServer,
                    serverName, _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
                GlobalCatalog gc = GlobalCatalog.GetGlobalCatalog(context);
                highestCommittedUsn = gc.HighestCommittedUsn;
            }
            else
            {
                DirectoryContext context = new DirectoryContext(DirectoryContextType.DirectoryServer,
                    serverName, _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
                DomainController dc = DomainController.GetDomainController(context);
                highestCommittedUsn = dc.HighestCommittedUsn;
            }

            ActiveDirectorySyncToken token = 
                new ActiveDirectorySyncToken("", serverName, useGlobalCatalog);
            token.LastDeleteUsn = highestCommittedUsn;
            token.LastModifiedUsn = highestCommittedUsn;
            return token.GetSyncToken();
        }

        string GetSyncServerName()
        {
            string serverName = null;

            if (UseGlobalCatalog())
            {
                serverName = _configuration.SyncGlobalCatalogServer;
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
                if (UseGlobalCatalog())
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
            return serverName;
        }

        bool UseGlobalCatalog()
        {
            return (_configuration.SearchChildDomains);
        }

        String GetSyncUpdateQuery(ActiveDirectorySyncToken adSyncToken)
        {
            string modifiedQuery = null;

            // if the token is not null, we may be able to start from 
            // the usn contained there
            if (adSyncToken != null)
            {
                modifiedQuery = string.Format("(!({0}<={1}))", ATT_USN_CHANGED, adSyncToken.LastModifiedUsn);
            }

            return modifiedQuery;
        }

        String GetSyncDeleteQuery(ActiveDirectorySyncToken adSyncToken)
        {
            string deletedQuery = null;

            // if the token is not null, we may be able to start from 
            // the usn contained there
            if (adSyncToken != null)
            {
                deletedQuery = string.Format("(&(!({0}<={1}))(isDeleted=TRUE))", ATT_USN_CHANGED, adSyncToken.LastDeleteUsn);
            }
            else
            {
                deletedQuery = string.Format("(isDeleted=TRUE)");
            }

            return deletedQuery;
        }

        #endregion

        #region AuthenticateOp Members

        public Uid Authenticate(ObjectClass objectClass, string username, 
            Org.IdentityConnectors.Common.Security.GuardedString password, 
            OperationOptions options)
        {
            bool returnUidOnly = false;

            if (options != null)
            {
                if (options.Options.ContainsKey(OPTION_DOMAIN))
                {
                    string domainName = options.Options[OPTION_DOMAIN].ToString();
                    if ((domainName != null) && (domainName.Length > 0))
                    {
                        username = string.Format("{0}@{1}", username, options.Options["w2k_domain"]);
                    }
                }
                else if (options.Options.ContainsKey(OPTION_RETURN_UID_ONLY))
                {
                    returnUidOnly = true;
                }
            }

            PasswordChangeHandler handler = new PasswordChangeHandler(_configuration);
            return handler.Authenticate(username, password, returnUidOnly);
        }

        #endregion

        #region AttributeNormalizer Members

        public virtual ConnectorAttribute NormalizeAttribute(ObjectClass oclass, ConnectorAttribute attribute)
        {
            // if this gets big, use delegates, but for now, just
            // handle individual attributes;
            if (attribute is Uid)
            {
                String uidValue = ((Uid)attribute).GetUidValue();
                // convert to upper case
                if (uidValue != null)
                {
                    StringBuilder normalizedUidValue = new StringBuilder();

                    if (oclass.Equals(ObjectClass.ACCOUNT))
                    {
                        // convert to upper case
                        uidValue = uidValue.ToUpper();

                        // now remove spaces
                        foreach (Char nextChar in uidValue)
                        {
                            if (!nextChar.Equals(" "))
                            {
                                normalizedUidValue.Append(nextChar);
                            }
                        }

                        return new Uid(normalizedUidValue.ToString());
                    }
                    else
                    {
                        // the uid is a dn
                        return new Uid(ActiveDirectoryUtils.NormalizeLdapString(uidValue));
                    }
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
            else if (attribute.Name.Equals(PredefinedAttributes.GROUPS_NAME))
            {
                IList<object> groupValues = new List<object>();
                foreach(String groupname in attribute.Value)
                {
                    groupValues.Add(ActiveDirectoryUtils.NormalizeLdapString(groupname));
                }
                return ConnectorAttributeBuilder.Build(attribute.Name, groupValues);
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
