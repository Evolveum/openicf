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
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Org.IdentityConnectors.Framework.Common.Objects;
using System.DirectoryServices;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using System.Diagnostics;
using Org.IdentityConnectors.Framework.Common;
using Org.IdentityConnectors.Framework.Spi.Operations;
using System.Security;
using ActiveDs;
using Org.IdentityConnectors.Common.Security;
using System.DirectoryServices.ActiveDirectory;

namespace Org.IdentityConnectors.ActiveDirectory
{
    /// <summary>
    /// Collection of Active directory utilities.  Some are static methods,
    /// other require configuration, so they are instance methods.
    /// </summary>
    public class ActiveDirectoryUtils
    {
        ActiveDirectoryConfiguration _configuration = null;
        private CustomAttributeHandlers _customHandlers = null;

        /// <summary>
        /// Constructor
        /// </summary>
        /// <param name="configuration">
        /// Configuration object for the connector.
        /// </param>
        public ActiveDirectoryUtils(ActiveDirectoryConfiguration configuration)
        {
            _configuration = configuration;
            _customHandlers = new CustomAttributeHandlers(_configuration);
        }

        /// <summary>
        /// Converts a guid in byte array form to a string suitable
        /// for ldap search.
        /// </summary>
        /// <param name="guidBytes"></param>
        /// <returns></returns>
        internal static String ConvertUIDBytesToSearchString(Byte[] guidBytes)
        {
            String searchGuid = "";

            for (int i = 0; i < guidBytes.Length; i++)
            {
                searchGuid += String.Format("\\{0:X2}", guidBytes[i]);
            }

            return searchGuid;
        }

        /// <summary>
        /// Converts a guid in byte array form to a string with the format
        /// &gt;GUID = xxxxxxxxxxxxxxxxxxxxxxxxxxxxx&lt; where the x's represent
        /// uppercase hexadecimal digits
        /// </summary>
        /// <param name="guidBytes"></param>
        /// <returns></returns>
        internal static String ConvertUIDBytesToGUIDString(Byte[] guidBytes)
        {
            return ConvertBytesToADSpecialString("GUID", guidBytes);
        }

        internal static String ConvertSIDBytesToGUIDString(Byte[] guidBytes)
        {
            return ConvertBytesToADSpecialString("SID", guidBytes);
        }

        internal static String ConvertBytesToADSpecialString(string attribute, Byte[] bytes)
        {
            String guidString = "<" + attribute + "=";

            for (int i = 0; i < bytes.Length; i++)
            {
                guidString += String.Format("{0:X2}", bytes[i]);
            }
            guidString += ">";

            return guidString;
        }

        /// <summary>
        /// Returns an ldap path in the form of: 
        /// LDAP://servernameIfSpecified/path
        /// </summary>
        /// <param name="serverName">Servername can be null</param>
        /// <param name="path">Path should not be null</param>
        /// <returns></returns>
        internal static String GetLDAPPath(string serverName, string path)
        {
            return GetFullPath("LDAP", serverName, path);
        }

        /// <summary>
        /// Returns a path string in the format:
        /// GC://servernameIfSpecified/path
        /// </summary>
        /// <param name="serverName">Servername is optional</param>
        /// <param name="path">Path should be specified</param>
        /// <returns></returns>
        internal static String GetGCPath(string serverName, string path)
        {
            return GetFullPath("GC", serverName, path);
        }

        /// <summary>
        /// Returns a path string in the format:
        /// provider://servernameIfSpecified/path
        /// </summary>
        /// <param name="provider">provider (such as ldap or gc)</param>
        /// <param name="serverName">servername - optional</param>
        /// <param name="path">path to resource</param>
        /// <returns></returns>
        internal static String GetFullPath(string provider, string serverName, string path)
        {
            IADsPathname pathName = getADSPathname(provider, serverName, path);
            return pathName.Retrieve((int)ADS_FORMAT_ENUM.ADS_FORMAT_X500);
        }

        /// <summary>
        /// uses iadspathname to create paths in a standard way
        /// </summary>
        /// <param name="provider"></param>
        /// <param name="serverName"></param>
        /// <param name="path"></param>
        /// <returns></returns>
        internal static IADsPathname getADSPathname(string provider, string serverName, string path)
        {
            IADsPathname pathName = new PathnameClass();
            if ((provider != null) && (provider.Length != 0))
            {
                pathName.Set(provider, (int)ADS_SETTYPE_ENUM.ADS_SETTYPE_PROVIDER);
            }

            if ((serverName != null) && (serverName.Length != 0))
            {
                pathName.Set(serverName, (int)ADS_SETTYPE_ENUM.ADS_SETTYPE_SERVER);
            }

            if ((path != null) && (path.Length != 0))
            {
                // must supply a path
                pathName.Set(path, (int)ADS_SETTYPE_ENUM.ADS_SETTYPE_DN);
            }
            return pathName;
        }

        /// <summary>
        /// Gets the dn of the parent object of the object specified by childDn
        /// </summary>
        /// <param name="childDn">distinguished name of an object to retrieve the parent of</param>
        /// <returns>distinguished name of the parent of 'childDn' or null</returns>
        internal static string GetParentDn(string childDn)
        {
            IADsPathname pathName = getADSPathname(null, null, childDn);
            return pathName.Retrieve((int)ADS_FORMAT_ENUM.ADS_FORMAT_X500_PARENT);
        }

        /// <summary>
        /// Updates an AD object (also called by create after object is created)
        /// </summary>
        /// <param name="oclass"></param>
        /// <param name="directoryEntry"></param>
        /// <param name="attributes"></param>
        /// <param name="type"></param>
        /// <param name="config"></param>
        internal void UpdateADObject(ObjectClass oclass, 
            DirectoryEntry directoryEntry, ICollection<ConnectorAttribute> attributes,
            UpdateType type, ActiveDirectoryConfiguration config) 
        {
            if(oclass.Equals(ObjectClass.ACCOUNT)) 
            {
                // translate attribute passed in
                foreach (ConnectorAttribute attribute in attributes)
                {
                    // encountered problems when processing change password at the same time 
                    // as setting expired.  It would be set to expired, but the change would 
                    // clear that.  So we must ensure that expired comes last.
                    if (OperationalAttributes.PASSWORD_EXPIRED_NAME.Equals(attribute.Name))
                    {
                        continue;
                    }

                    AddConnectorAttributeToADProperties(oclass,
                        directoryEntry, attribute, type);

                    //  Uncommenting the next line is very helpful in
                    //  finding mysterious errors.                    
                    //  directoryEntry.CommitChanges();
                }

                directoryEntry.CommitChanges();

                // now do the password change.  This is handled separately, because
                // it might be a user changing his own password, or it might be an
                // administrative change.

                GuardedString gsNewPassword = ConnectorAttributeUtil.GetPasswordValue(attributes);
                if (gsNewPassword != null)
                {
                    GuardedString gsCurrentPassword = ConnectorAttributeUtil.GetCurrentPasswordValue(attributes);
                    PasswordChangeHandler changeHandler = new PasswordChangeHandler(_configuration);
                    if (gsCurrentPassword == null)
                    {
                        // just a normal password change
                        changeHandler.changePassword(directoryEntry, gsNewPassword);
                    }
                    else
                    {
                        changeHandler.changePassword(directoryEntry,
                            gsCurrentPassword, gsNewPassword);
                    }


                UserAccountControl.Set(directoryEntry.Properties[ActiveDirectoryConnector.ATT_USER_ACOUNT_CONTROL], 
                    UserAccountControl.PASSWD_NOTREQD, false);
                    directoryEntry.CommitChanges();
                }

                // see note in loop above for explaination of this
                ConnectorAttribute expirePasswordAttribute = ConnectorAttributeUtil.Find(
                    OperationalAttributes.PASSWORD_EXPIRED_NAME, attributes);

                if (expirePasswordAttribute != null)
                {
                    AddConnectorAttributeToADProperties(oclass,
                        directoryEntry, expirePasswordAttribute, type);
                    directoryEntry.CommitChanges();
                }

                UserAccountControl.Set(directoryEntry.Properties[ActiveDirectoryConnector.ATT_USER_ACOUNT_CONTROL],
                    UserAccountControl.PASSWD_NOTREQD, false);

                directoryEntry.CommitChanges();

                HandleNameChange(type, directoryEntry, attributes);
                HandleContainerChange(type, directoryEntry, attributes, config);
            }
            else if (oclass.Equals(ObjectClass.GROUP))
            {
                // translate attribute passed in
                foreach (ConnectorAttribute attribute in attributes)
                {
                    // Temporary
                    // Trace.TraceInformation(String.Format("Setting attribute {0} to {1}",
                    //    attribute.Name, attribute.Value));
                    AddConnectorAttributeToADProperties(oclass,
                        directoryEntry, attribute, type);
                    //                  Uncommenting the next line is very helpful in
                    //                  finding mysterious errors.
                                     directoryEntry.CommitChanges();
                }

                directoryEntry.CommitChanges();
                HandleNameChange(type, directoryEntry, attributes);
                HandleContainerChange(type, directoryEntry, attributes, config);
            }
            else if (oclass.Equals(ActiveDirectoryConnector.ouObjectClass))
            {
                // translate attribute passed in
                foreach (ConnectorAttribute attribute in attributes)
                {
                    // Temporary
                    // Trace.TraceInformation(String.Format("Setting attribute {0} to {1}",
                    //    attribute.Name, attribute.Value));
                    AddConnectorAttributeToADProperties(oclass,
                        directoryEntry, attribute, type);
                    //                  Uncommenting the next line is very helpful in
                    //                  finding mysterious errors.
                    directoryEntry.CommitChanges();
                }

                directoryEntry.CommitChanges();
                HandleNameChange(type, directoryEntry, attributes);
                HandleContainerChange(type, directoryEntry, attributes, config);
            }
            else
            {
                throw new ConnectorException(
                    _configuration.ConnectorMessages.Format("ex_InvalidObjectClass", 
                    "Invalid object class: {0}", oclass.GetObjectClassValue()));
            }            
        }

        internal ConnectorAttribute GetConnectorAttributeFromADEntry(ObjectClass oclass,
            String attributeName, SearchResult searchResult)
        {
            // Boolean translated = false;
            if (searchResult == null)
            {
                throw new ConnectorException(_configuration.ConnectorMessages.Format(
                    "ex_AttributeNull",
                    "Could not add connector attribute to <null> search result"));
            }

            return _customHandlers.GetCaFromDe(oclass, 
                attributeName, searchResult);

        }

        internal void AddConnectorAttributeToADProperties(ObjectClass oclass,
            DirectoryEntry directoryEntry, ConnectorAttribute attribute, 
            UpdateType type)
        {
            // Boolean translated = false;
            if (directoryEntry == null)
            {
                throw new ConnectorException(_configuration.ConnectorMessages.Format(
                    "ex_CouldNotAddNullAttributeToDe",
                    "Could not add connector attribute to <null> directory entry"));
            }

            _customHandlers.UpdateDeFromCa(oclass, type,
                directoryEntry, attribute);

        }

        /*
        /// <summary>
        /// creates and returns a connector attribute or null.  the attribute
        /// has the name 'name' and the values associated with 'name' in the
        /// directory entry
        /// </summary>
        /// <param name="name"></param>
        /// <param name="pvc"></param>
        /// <returns></returns>
        private static ConnectorAttribute CreateConnectorAttribute(String name, 
            PropertyValueCollection pvc)
        {
            ConnectorAttributeBuilder attributeBuilder = new ConnectorAttributeBuilder();

            if (name == null)
            {
                return null;
            }

            attributeBuilder.Name = name;

            if (pvc == null)
            {
                attributeBuilder.AddValue(null);
            }
            else
            {
                for (int i = 0; i < pvc.Count; i++)
                {
                    Object valueObject = pvc[i];
                    if ((pvc[i] == null) ||
                        (FrameworkUtil.IsSupportedAttributeType(valueObject.GetType())))
                    {
                        attributeBuilder.AddValue(pvc[i]);
                    }
                    else
                    {
                        Trace.TraceWarning(
                            "Unsupported attribute type ... calling ToString (Name: \'{0}\'({1}) Type: \'{2}\' String Value: \'{3}\'",
                            name, i, pvc[i].GetType(), pvc[i].ToString());
                        attributeBuilder.AddValue(pvc[i].ToString());
                    }
                }
            }

            return attributeBuilder.Build();
        }
        
        private static void AddConnectorAttributeToADProperties_general(
            PropertyCollection properties,
            ConnectorAttribute attribute, UpdateType type)
        {
            // null out the values if we are deleting
            // or replacing attributes.
            if (type.Equals(UpdateType.DELETE) ||
                type.Equals(UpdateType.REPLACE))
            {
                properties[attribute.Name].Value = null;
            }

            // if we are updating or adding, put the
            // new values in.
            if (type.Equals(UpdateType.ADD) ||
                type.Equals(UpdateType.REPLACE))
            {
                foreach (Object valueObject in attribute.Value)
                {
                    properties[attribute.Name].Add(valueObject);
                }
            }
        }
        */
        
        /// <summary>
        /// Gets a single value from a propertyvaluecollection  
        /// for a particular property name.  Its an error if the
        /// property contains multiple values.
        /// </summary>
        /// <param name="pvc"></param>
        /// <returns></returns>
        internal Object GetSingleValue(PropertyValueCollection pvc)
        {
            if((pvc == null) || (pvc.Count == 0))
            {
                return null;
            }

            if (pvc.Count > 1)
            {
                String msg = _configuration.ConnectorMessages.Format(
                    "ex_ExpectingSingleValue",
                    "Expecting single value, but found multiple values for attribute {0}",
                    pvc.PropertyName);
                throw new ConnectorException(msg);
            }

            return pvc[0];
        }

        /// <summary>
        /// Finds a DirectoryEntry by it's uid
        /// </summary>
        /// <param name="serverName"></param>
        /// <param name="uid"></param>
        /// <param name="adminUserName"></param>
        /// <param name="adminPassword"></param>
        /// <returns></returns>
        internal static DirectoryEntry GetDirectoryEntryFromUid(String serverName,
            Uid uid, string adminUserName, string adminPassword)
        {
            DirectoryEntry foundDirectoryEntry = new DirectoryEntry(
                ActiveDirectoryUtils.GetLDAPPath(serverName, uid.GetUidValue()),
                adminUserName, adminPassword);
            string dn = (string)foundDirectoryEntry.Properties["distinguishedName"][0];
            foundDirectoryEntry = new DirectoryEntry(
                ActiveDirectoryUtils.GetLDAPPath(serverName, dn),
                adminUserName, adminPassword);
            return foundDirectoryEntry;
        }

        /// <summary>
        /// Returns the AD ObjectClass associated with a particular
        /// Connector ObjectClass
        /// </summary>
        /// <param name="oclass"></param>
        /// <returns></returns>
        internal String GetADObjectClass(ObjectClass oclass)
        {

            if (oclass.Equals(ObjectClass.ACCOUNT))
            {
                return "User";
            }
            else if (oclass.Equals(ObjectClass.GROUP))
            {
                return "Group";
            }
            else if ("ORGANIZATIONAL UNIT".Equals(oclass.GetObjectClassValue(), StringComparison.CurrentCultureIgnoreCase))
            {
                return "organizationalUnit";
            }
            else
            { 
                String msg = _configuration.ConnectorMessages.Format(
                    "ex_ObjectClassInvalidForConnector",
                    "ObjectClass \'{0}\' is not valid for this connector",
                    oclass.GetObjectClassValue());
                throw new ConnectorException(msg);
            }
        }

        /// <summary>
        /// Puts an ldap string into a normalilzed format
        /// </summary>
        /// <param name="ldapString"></param>
        /// <returns></returns>
        public static String NormalizeLdapString(String ldapString)
        {
            StringBuilder normalPath = new StringBuilder();
            String[] parts = ldapString.Split(',');
            for (int i = 0; i < parts.Length; i++)
            {
                normalPath.Append(parts[i].Trim().ToUpper());
                // append a comma after each part (except the last one)
                if (i < (parts.Length - 1))
                {
                    normalPath.Append(",");
                }
            }
            return normalPath.ToString();
        }

        public static String GetRelativeName(Name name)
        {
            return GetNameAsCN(name.GetNameValue());
        }

        /// <summary>
        /// Returns the leaf value of a distinguished name
        /// </summary>
        /// <param name="nameValue"></param>
        /// <returns></returns>
        internal static String GetNameAsCN(String nameValue)
        {
            IADsPathname pathName = getADSPathname(null, null, nameValue);            
            return pathName.Retrieve((int)ADS_FORMAT_ENUM.ADS_FORMAT_LEAF);
        }

        /// <summary>
        /// This does not work ... for now, don't handle container changes
        /// </summary>
        /// <param name="type"></param>
        /// <param name="directoryEntry"></param>
        /// <param name="attributes"></param>
        /// <param name="config"></param>
        private static void HandleContainerChange(UpdateType type, 
            DirectoryEntry directoryEntry, ICollection<ConnectorAttribute> attributes, 
            ActiveDirectoryConfiguration config)
        {
            // this return means that te connector attribute is ignored for
            // the purpose of moving an object to a different container
            return;

            // this code seems right, but doesnt work.  The DirectoryEntry.Move()
            // method always throws an Exception with the text 'unspecified error'

            ConnectorAttribute containerAttribute =
                ConnectorAttributeUtil.Find(ActiveDirectoryConnector.ATT_CONTAINER, attributes);
            if (containerAttribute != null)
            {
                // this only make sense for replace.  you can't
                // add a name or delete a name
                if (type.Equals(UpdateType.REPLACE))
                {
                    DirectoryEntry parent = directoryEntry.Parent;
                    String oldContainer = null;
                    if (parent != null)
                    {
                        PropertyValueCollection parentDNValues = 
                            parent.Properties[ActiveDirectoryConnector.ATT_DISTINGUISHED_NAME];
                        if ((parentDNValues.Count == 1) && (parentDNValues[0] is String))
                        {
                            oldContainer = (String)parentDNValues[0];
                        }
                        else
                        {
                            String msg = String.Format("Unable to retrieve the distinguished name for {0}.",
                                parent.Path);
                            throw new ConnectorException(msg);
                        }
                    }

                    String newContainer = ConnectorAttributeUtil.GetStringValue(containerAttribute);

                    if (newContainer != null)
                    {
                        try
                        {
                            if (!NormalizeLdapString(oldContainer).Equals(
                                NormalizeLdapString(newContainer)))
                            {
                                DirectoryEntry newContainerDe = new DirectoryEntry(newContainer,
                                    config.DirectoryAdminName, config.DirectoryAdminPassword);
                                directoryEntry.MoveTo(newContainerDe);
                            }
                        }
                        catch (Exception e)
                        {
                            throw e;
                        }
                    }
                }
            }
        }

        private static void HandleNameChange(UpdateType type, 
            DirectoryEntry directoryEntry,
            ICollection<ConnectorAttribute> attributes)
        {
            Name nameAttribute = ConnectorAttributeUtil.GetNameFromAttributes(attributes);
            if (nameAttribute != null)
            {
                // this only make sense for replace.  you can't
                // add a name or delete a name
                if (type.Equals(UpdateType.REPLACE))
                {
                    String oldName = directoryEntry.Name;
                    String newName = GetRelativeName(nameAttribute);
                    if (!NormalizeLdapString(oldName).Equals(NormalizeLdapString(newName)))
                    {
                        directoryEntry.Rename(newName);
                    }
                }
            }
        }

        public static SecureString GetSecureString(String stringToSecure)
        {
            SecureString secure = new SecureString();

            foreach (char nextChar in stringToSecure)
            {
                secure.AppendChar(nextChar);
            }

            return secure;
        }

        internal static string GetDnFromPath(string fullPath)
        {
            IADsPathname pathName = new PathnameClass();
            pathName.Set(fullPath, (int)ADS_SETTYPE_ENUM.ADS_SETTYPE_FULL);
            return pathName.Retrieve((int)ADS_FORMAT_ENUM.ADS_FORMAT_X500_DN);
        }

        internal static DomainController GetDomainController(ActiveDirectoryConfiguration configuration)
        {
            String serverName = configuration.LDAPHostName;
            DomainController controller = null;

            if ((serverName == null) || (serverName.Length == 0))
            {
                // get the active directory schema
                DirectoryContext context = new DirectoryContext(
                        DirectoryContextType.Domain,
                        configuration.DomainName,
                        configuration.DirectoryAdminName,
                        configuration.DirectoryAdminPassword);
                controller = DomainController.FindOne(context);
            }
            else
            {
                DirectoryContext context = new DirectoryContext(
                        DirectoryContextType.DirectoryServer,
                        configuration.LDAPHostName,
                        configuration.DirectoryAdminName,
                        configuration.DirectoryAdminPassword);
                controller = DomainController.GetDomainController(context);
            }
            
            return controller;
        }
    }

}
