/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
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
 * Portions Copyrighted 2012-2014 ForgeRock AS.
 */
using System;
using System.Collections.Generic;
using System.Linq;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common;
using System.DirectoryServices;
using DS = System.DirectoryServices;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using System.Diagnostics;
using ActiveDs;
using System.IO;
using System.Security.AccessControl;
using System.Security.Principal;
using Org.IdentityConnectors.Common;
using System.Text;

namespace Org.IdentityConnectors.ActiveDirectory
{   
    /// <summary>
    /// This class will encapsulate all changes from AD attributes
    /// to Connector attributes and from Connector attributes to
    /// AD attributes.  If attributes are more complex and can't be
    /// handled here, add them to the appropriate ignore list, and handle
    /// them in the AD Connector (or elsewhere).
    /// 
    /// If it is a connector attribute that has the same name as the
    /// ad attribute, and the value requires no translation, it will
    /// be handled by the generic handler.  If not, add a delegate for
    /// either AD->Connector or for Connector->AD (or both).
    /// </summary>
    internal class CustomAttributeHandlers
    {
        // tracing (using ActiveDirectoryConnector's name!)
        internal static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        // Max range retrieval to obtain the members of a group
        private static readonly int GRP_MEMBERS_MAXRANGE = 1500;
        // names from active directory attributes to ignore during
        // generic translation 
        IList<string> IgnoreADAttributeNames_account = new List<string>();
        IList<string> IgnoreADAttributeNames_group = new List<string>();

        // names from connector attributes to ignore during
        // generic translation 
        IList<string> IgnoreConnectorAttributeNames_account = new List<string>();
        IList<string> IgnoreConnectorAttributeNames_group = new List<string>();
        IList<string> IgnoreConnectorAttributeNames_ou = new List<string>();
        IList<string> IgnoreConnectorAttributeNames_generic = new List<string>();

        // method to update a directory entry from a connector attribute
        Dictionary<string, UpdateDeFromCa_delegate> 
            UpdateDeFromCaDelegates = new Dictionary<string, 
                UpdateDeFromCa_delegate>(StringComparer.CurrentCultureIgnoreCase);

        // method to get a connector attribute from a directory entry
        Dictionary<string, GetCaFromDe_delegate>
            GetCaFromDeDelegates = new Dictionary<string,
                GetCaFromDe_delegate>(StringComparer.CurrentCultureIgnoreCase);

        ActiveDirectoryConfiguration _configuration = null;

        internal CustomAttributeHandlers(ActiveDirectoryConfiguration configuration) {
            // save the configuration
            _configuration = configuration;

            // Connector attributes names to ignore for accounts
            IgnoreConnectorAttributeNames_account.Add(Name.NAME);
            IgnoreConnectorAttributeNames_account.Add(ActiveDirectoryConnector.ATT_CONTAINER);
            IgnoreConnectorAttributeNames_account.Add(Uid.NAME);
            IgnoreConnectorAttributeNames_account.Add(OperationalAttributes.PASSWORD_NAME);
            IgnoreConnectorAttributeNames_account.Add(OperationalAttributes.CURRENT_PASSWORD_NAME);

            // Connector attributes names to ignore for groups
            IgnoreConnectorAttributeNames_group.Add(Name.NAME);
            IgnoreConnectorAttributeNames_group.Add(ActiveDirectoryConnector.ATT_CONTAINER);
            IgnoreConnectorAttributeNames_group.Add(Uid.NAME);
            IgnoreConnectorAttributeNames_group.Add("authOrig");
            IgnoreConnectorAttributeNames_group.Add("unauthOrig");
            IgnoreConnectorAttributeNames_group.Add("groupTypes");

            // Connector attributes names to ignore for ous
            IgnoreConnectorAttributeNames_ou.Add(Name.NAME);
            IgnoreConnectorAttributeNames_ou.Add(Uid.NAME);

            // Connector attributes names to ignore for everything else
            IgnoreConnectorAttributeNames_generic.Add(Name.NAME);
            IgnoreConnectorAttributeNames_generic.Add(Uid.NAME);

            // methods to update a directory entry from a connectorattribute
            UpdateDeFromCaDelegates.Add(ActiveDirectoryConnector.ATT_ACCOUNTS,
                UpdateDeFromCa_OpAtt_Accounts);
            UpdateDeFromCaDelegates.Add(PredefinedAttributes.GROUPS_NAME,
                UpdateDeFromCa_OpAtt_Groups);
            UpdateDeFromCaDelegates.Add(ActiveDirectoryConnector.ATT_MEMBER,
                UpdateDeFromCa_OpAtt_Member);
            UpdateDeFromCaDelegates.Add(ActiveDirectoryConnector.ATT_HOME_DIRECTORY,
                UpdateDeFromCa_Att_HomeDirectory);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.ENABLE_NAME,
                UpdateDeFromCa_OpAtt_Enable);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.PASSWORD_EXPIRED_NAME,
                UpdateDeFromCa_OpAtt_PasswordExpired);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,
                UpdateDeFromCa_OpAtt_PasswordExpireDate);
            UpdateDeFromCaDelegates.Add(ActiveDirectoryConnector.ATT_ACCOUNT_EXPIRES,
                UpdateDeFromCa_OpAtt_AccountExpireDate);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.LOCK_OUT_NAME,
                UpdateDeFromCa_OpAtt_Lockout);
            UpdateDeFromCaDelegates.Add(ActiveDirectoryConnector.ATT_PASSWORD_NEVER_EXPIRES,
                UpdateDeFromCa_PasswordNeverExpires);
            
            // supporting class not implemented in the framework
            /*
            UpdateDeFromCaDelegates.Add(OperationalAttributes.ENABLE_DATE_NAME,
                UpdateDeFromCa_OpAtt_EnableDate);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.DISABLE_DATE_NAME,
                UpdateDeFromCa_OpAtt_DisableDate);
            */ 
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_ALLOW_LOGON,
                UpdateDeFromCa_Att_TSAllowLogon);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_INITIAL_PROGRAM,
                UpdateDeFromCa_Att_TSInitialProgram);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_INITIAL_PROGRAM_DIR,
                UpdateDeFromCa_Att_TSInitialProgramDir);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_MAX_CONNECTION_TIME,
                UpdateDeFromCa_Att_TSMaxConnectionTime);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_MAX_DISCONNECTION_TIME,
                UpdateDeFromCa_Att_TSMaxDisconnectionTime);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_MAX_IDLE_TIME,
                UpdateDeFromCa_Att_TSMaxIdleTime);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_CONNECT_CLIENT_DRIVES_AT_LOGON,
                UpdateDeFromCa_Att_TSConnectClientDrivesAtLogon);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_CONNECT_CLIENT_PRINTERS_AT_LOGON,
                UpdateDeFromCa_Att_TSConnectClientPrintersAtLogon);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_DEFAULT_TO_MAIN_PRINTER,
                UpdateDeFromCa_Att_TSDefaultToMainPrinter);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_BROKEN_CONNECTION_ACTION,
                UpdateDeFromCa_Att_TSBrokenConnectionAction);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_RECONNECTION_ACTION,
                UpdateDeFromCa_Att_TSReconnectionAction);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_ENABLE_REMOTE_CONTROL,
                UpdateDeFromCa_Att_TSEnableRemoteControl);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_PROFILE_PATH,
                UpdateDeFromCa_Att_TSProfilePath);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_HOME_DIRECTORY,
                UpdateDeFromCa_Att_TSHomeDirectory);
            UpdateDeFromCaDelegates.Add(TerminalServicesUtils.TS_HOME_DRIVE,
                UpdateDeFromCa_Att_TSHomeDrive);

            // methods to create a connector attribute from a directory entry
            GetCaFromDeDelegates.Add(Name.NAME, GetCaFromDe_OpAtt_Name);
            GetCaFromDeDelegates.Add(Uid.NAME, GetCaFromDe_OpAtt_Uid);
            GetCaFromDeDelegates.Add(ActiveDirectoryConnector.ATT_CONTAINER,
                GetCaFromDe_Att_Container);
            GetCaFromDeDelegates.Add(ActiveDirectoryConnector.ATT_ACCOUNTS,
                GetCaFromDe_OpAtt_Accounts);
            GetCaFromDeDelegates.Add(PredefinedAttributes.GROUPS_NAME,
                GetCaFromDe_OpAtt_Groups);
            GetCaFromDeDelegates.Add(ActiveDirectoryConnector.ATT_MEMBER,
                GetCaFromDe_OpAtt_GroupMembers);
            GetCaFromDeDelegates.Add(OperationalAttributes.ENABLE_NAME,
                GetCaFromDe_OpAtt_Enabled);
            GetCaFromDeDelegates.Add(OperationalAttributes.PASSWORD_EXPIRED_NAME,
                GetCaFromDe_OpAtt_PasswordExpired);
            GetCaFromDeDelegates.Add(PredefinedAttributes.DESCRIPTION,
                GetCaFromDe_OpAtt_Description);
            GetCaFromDeDelegates.Add(PredefinedAttributes.SHORT_NAME,
                GetCaFromDe_OpAtt_ShortName);
            GetCaFromDeDelegates.Add(OperationalAttributes.LOCK_OUT_NAME,
                GetCaFromDe_OpAtt_Lockout);
            GetCaFromDeDelegates.Add(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,
                GetCaFromDe_OpAtt_PasswordExpireDate);
			GetCaFromDeDelegates.Add(ActiveDirectoryConnector.ATT_ACCOUNT_EXPIRES,
                GetCaFromDe_OpAtt_AccountExpireDate);
            GetCaFromDeDelegates.Add(ActiveDirectoryConnector.ATT_PASSWORD_NEVER_EXPIRES,
                GetCaFromDe_PasswordNeverExpires);
            // supporting class not implemented in the framework
            /*
            GetCaFromDeDelegates.Add(OperationalAttributes.ENABLE_DATE_NAME,
                GetCaFromDe_OpAtt_EnableDate);
            GetCaFromDeDelegates.Add(OperationalAttributes.DISABLE_DATE_NAME,
                GetCaFromDe_OpAtt_DisableDate);
            */
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_INITIAL_PROGRAM,
                GetCaFromDe_Att_TSInitialProgram);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_INITIAL_PROGRAM_DIR,
                GetCaFromDe_Att_TSInitalProgramDir);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_ALLOW_LOGON,
                GetCaFromDe_Att_TSAllowLogon);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_MAX_CONNECTION_TIME,
                GetCaFromDe_Att_TSMaxConnectionTime);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_MAX_DISCONNECTION_TIME,
                GetCaFromDe_Att_TSMaxDisconnectionTime);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_MAX_IDLE_TIME,
                GetCaFromDe_Att_TSMaxIdleTime);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_CONNECT_CLIENT_DRIVES_AT_LOGON,
                GetCaFromDe_Att_TSConnectClientDrivesAtLogon);            
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_CONNECT_CLIENT_PRINTERS_AT_LOGON,
                GetCaFromDe_Att_TSConnectClientPrintersAtLogon);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_DEFAULT_TO_MAIN_PRINTER,
                GetCaFromDe_Att_TSDefaultToMainPrinter);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_BROKEN_CONNECTION_ACTION,
                GetCaFromDe_Att_TSBrokenConnectionAction);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_RECONNECTION_ACTION,
                GetCaFromDe_Att_TSReconnectionAction);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_ENABLE_REMOTE_CONTROL,
                GetCaFromDe_Att_TSEnableRemoteControl);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_PROFILE_PATH,
                GetCaFromDe_Att_TSProfilePath);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_HOME_DIRECTORY,
                GetCaFromDe_Att_TSHomeDirectory);
            GetCaFromDeDelegates.Add(TerminalServicesUtils.TS_HOME_DRIVE,
                GetCaFromDe_Att_TSHomeDrive);
        }

        internal void UpdateDeFromCa(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute) {

            // if this gets big, replace with dictionary key by object class
            IList<string> ignoreList = null;
            if(oclass.Equals(ObjectClass.ACCOUNT)) {
                ignoreList = IgnoreConnectorAttributeNames_account;
            }
            else if (oclass.Equals(ActiveDirectoryConnector.groupObjectClass))
            {
                ignoreList = IgnoreConnectorAttributeNames_group;
            }
            else if (oclass.Equals(ActiveDirectoryConnector.ouObjectClass))
            {
                ignoreList = IgnoreConnectorAttributeNames_ou;
            }
            else
            {
                ignoreList = IgnoreConnectorAttributeNames_generic;
            }

            // if it's an ignored attribute, we're done
            if ((ignoreList != null) && 
                (ignoreList.Contains(attribute.Name, 
                StringComparer.CurrentCultureIgnoreCase))) {
                return;
            }

            if (UpdateDeFromCaDelegates.ContainsKey(attribute.Name))
            {
                // if it's an attribute with a special handler,
                // call the handler
                UpdateDeFromCa_delegate handler = 
                    UpdateDeFromCaDelegates[attribute.Name];
                handler(oclass, type, directoryEntry, attribute);
            }
            else
            {
                // if none of the above, call the generic handler
                UpdateDeFromCa_Att_Generic(oclass, type, directoryEntry, attribute, false);
            }
        }


        internal ConnectorAttribute GetCaFromDe(ObjectClass oclass, 
            string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttribute attribute = null;

            if (GetCaFromDeDelegates.ContainsKey(attributeName))
            {
                // if it's an attribute with a special handler,
                // call the handler
                GetCaFromDe_delegate handler = GetCaFromDeDelegates[attributeName];
                attribute = handler(oclass, attributeName, searchResult, entry);
            }
            else
            {
                // if none of the above, call the generic handler
                attribute = GetCaFromDe_Att_Generic(oclass, attributeName, searchResult, entry);
            }

            return attribute;
        }

        internal delegate void UpdateDeFromCa_delegate(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute);

        internal delegate ConnectorAttribute GetCaFromDe_delegate(ObjectClass oclass,
            string attributeName, DS.SearchResult searchResult, DirectoryEntry entry);

        public void GetAddsAndDeletes(ICollection<Object>valuesToAdd, ICollection<Object>valuesToRemove,
            PropertyValueCollection oldValues, ICollection<Object>newValues, UpdateType type) {
                if (UpdateType.ADD.Equals(type))
                {
                    // add all groups
                    if (newValues != null)
                    {
                        foreach (Object value in newValues)
                        {
                            valuesToAdd.Add(value);
                        }
                    }
                }
                else if (UpdateType.REPLACE.Equals(type))
                {
                    // look through existing values, and remove them
                    // if they are not in the newValues
                    if (oldValues != null)
                    {
                        foreach (Object value in oldValues)
                        {
                            if (newValues == null || !newValues.Contains(value))
                            {
                                valuesToRemove.Add(value);
                            }
                        }
                    }

                    // look through the values passed in and
                    // add them if they are not existing values
                    if (newValues != null)
                    {
                        foreach (Object value in newValues)
                        {
                            if ((oldValues == null) || (!oldValues.Contains(value)))
                            {
                                valuesToAdd.Add(value);
                            }
                        }
                    }
                }
                else if (UpdateType.DELETE.Equals(type))
                {
                    if (newValues != null)
                    {
                        foreach (Object value in newValues)
                        {
                            valuesToRemove.Add(value);
                        }
                    }
                }
        }

        #region UpdateDeFromCa handlers

        internal string DumpPVC(PropertyValueCollection pvc)
        {
            StringBuilder sb = new StringBuilder();
            if (pvc != null)
            {
                bool first = true;
                foreach (object o in pvc)
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        sb.Append(", ");
                    }
                    sb.Append(o);
                }
            }
            else
            {
                sb.Append("(null)");
            }
            return sb.ToString();
        }

        internal void UpdateDeFromCa_OpAtt_Groups(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            if (oclass.Equals(ObjectClass.ACCOUNT))
            {
                // in this case, AD will not allow groups to be added
                // to a user.  To simulate this, lookup each added group
                // and add this user to the group
                ICollection<Object> newValues = attribute.Value;
                PropertyValueCollection oldValues = null;
                if(directoryEntry.Properties.Contains(ActiveDirectoryConnector.ATT_MEMBEROF)) {
                    oldValues = directoryEntry.Properties[ActiveDirectoryConnector.ATT_MEMBEROF];
                }

                ICollection<Object> groupsToAdd = new HashSet<Object>();
                ICollection<Object> groupsToRemove = new HashSet<Object>();

                if (LOGGER.Switch.ShouldTrace(TraceEventType.Verbose)) {
                    LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "UpdateDeFromCa_OpAtt_Groups: user = {0}, oldValues = {1}, newValues = {2}, updateType = {3}",
                        directoryEntry.Name, DumpPVC(oldValues), CollectionUtil.Dump(newValues), type);
                }

                GetAddsAndDeletes(groupsToAdd, groupsToRemove, oldValues, newValues, type);

                if (LOGGER.Switch.ShouldTrace(TraceEventType.Verbose)) {
                    LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "UpdateDeFromCa_OpAtt_Groups: user = {0}, groupsToAdd = {1}, groupsToRemove = {2}",
                        directoryEntry.Name, CollectionUtil.Dump(groupsToAdd), CollectionUtil.Dump(groupsToRemove));
                }
                
                foreach (Object obj in groupsToRemove)
                {
                    // lookup the group and remove this user from group if it's a
                    // valid group.
                    String groupPath = ActiveDirectoryUtils.GetLDAPPath(
                        _configuration.LDAPHostName, (String)obj);
                    DirectoryEntry groupDe = new DirectoryEntry(groupPath,
                        _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
                    String distinguishedName = ActiveDirectoryUtils.GetDnFromPath(directoryEntry.Path);
                    if (LOGGER.Switch.ShouldTrace(TraceEventType.Verbose)) {
                        LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "UpdateDeFromCa_OpAtt_Groups: Group: {0}, members before = {1}, removing {2}",
                        groupPath, DumpPVC(groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER]), distinguishedName);
                    }
                    if (groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER].Contains(distinguishedName))
                    {
                        groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER].Remove(distinguishedName);
                        groupDe.CommitChanges();
                    }
                    else
                    {
                        LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "{0} is NOT a member of {1}, no remove operation is executed", distinguishedName, groupPath);
                    }
                    groupDe.Dispose();
                }

                foreach (Object obj in groupsToAdd)
                {
                    // lookup the group and add this user to group if it's a
                    // valid group.
                    String groupPath = ActiveDirectoryUtils.GetLDAPPath(
                        _configuration.LDAPHostName, (String)obj);
                    //Trace.TraceInformation("groupPath = {0}", groupPath);
                    DirectoryEntry groupDe = new DirectoryEntry(groupPath,
                        _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
                    //Trace.TraceInformation("DirectoryEntry for this group created: {0}", groupDe);
                    String distinguishedName = ActiveDirectoryUtils.GetDnFromPath(directoryEntry.Path);
                    if (LOGGER.Switch.ShouldTrace(TraceEventType.Verbose)) {
                        LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "UpdateDeFromCa_OpAtt_Groups: Group: {0}, members before = {1}, adding {2}",
                            groupPath, DumpPVC(groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER]), distinguishedName);
                    }
                    if (!groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER].Contains(distinguishedName))
                    {
                        groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER].Add(distinguishedName);
                        groupDe.CommitChanges();
                    }
                    else
                    {
                        LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "{0} is already a member of {1}, no add operation is executed", distinguishedName, groupPath);
                    }
                    groupDe.Dispose();
                }
            }
            else
            {
                throw new ConnectorException(
                    String.Format("''{0}'' is an invalid attribute for object class ''{1}''",
                        PredefinedAttributeInfos.GROUPS, oclass.GetObjectClassValue()));
            }
        }

        internal void UpdateDeFromCa_OpAtt_Member(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            if (ActiveDirectoryConnector.groupObjectClass.Equals(oclass))
            {
                UpdateDeFromCa_Att_Generic(oclass, type, directoryEntry, attribute, true);
            }
            else
            {
                throw new ConnectorException(
                    String.Format("'{0}' is an invalid attribute for object class '{1}'",
                        ActiveDirectoryConnector.ATT_MEMBER, oclass.GetObjectClassValue()));
            }
        }

        internal void UpdateDeFromCa_OpAtt_Accounts(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry, 
            ConnectorAttribute attribute) 
        {
            if (ActiveDirectoryConnector.groupObjectClass.Equals(oclass))
            {
                // create an 'attribute' with the real name, and then call the 
                // generic version
                ConnectorAttribute newAttribute = ConnectorAttributeBuilder.Build(
                    ActiveDirectoryConnector.ATT_MEMBER, attribute.Value);
                UpdateDeFromCa_Att_Generic(oclass, type, directoryEntry, newAttribute, true);
            }
            else
            {
                throw new ConnectorException(
                    String.Format("'{0}' is an invalid attribute for object class '{1}'",
                        ActiveDirectoryConnector.ATT_ACCOUNTS, oclass.GetObjectClassValue()));
            }
        }

        internal void UpdateDeFromCa_Att_HomeDirectory(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            String homeDir = ConnectorAttributeUtil.GetStringValue(attribute);

            if (homeDir != null)
            {
                if (type == UpdateType.REPLACE)
                {
                    // first set the attribute
                    UpdateDeFromCa_Att_Generic(oclass, type, directoryEntry, attribute, true);
                    
                    // now create attribute if needed/possible
                    if (_configuration.CreateHomeDirectory)
                    {
                        // from old code ... should start with '\\' and have at least one 
                        // '\' later that's not the end of the string
                        // i.e 
                        //     \\somemachine\someshare\somedirectory
                        //     \\somemachine\someshare\somedirectory\someotherdirectory
                        // but not 
                        //     \\somemachine\someshare\
                        //     \\somemachine\someshare
                        // just ignore if it's not correct
                        String directoryName = ConnectorAttributeUtil.GetStringValue(attribute);
                        if (directoryName.StartsWith("\\\\"))
                        {
                            int secondPathSepIndex = directoryName.IndexOf('\\', 2);
                            if ((secondPathSepIndex > 2) && (directoryName.Length > secondPathSepIndex + 1))
                            {
                                // name passes, so create directory

                                // create security object
                                DirectorySecurity dirSecurity = new DirectorySecurity();
                                PropertyValueCollection pvc =
                                    directoryEntry.Properties[ActiveDirectoryConnector.ATT_OBJECT_SID];
                                // there should always be exactly one sid
                                SecurityIdentifier sid = new SecurityIdentifier((byte[])pvc[0], 0);
                                // dirSecurity.SetOwner(sid);
                                InheritanceFlags iFlags = InheritanceFlags.ContainerInherit;
                                dirSecurity.AddAccessRule(
                                    new FileSystemAccessRule(sid, FileSystemRights.FullControl,
                                        InheritanceFlags.ContainerInherit | InheritanceFlags.ObjectInherit, 
                                        PropagationFlags.None, AccessControlType.Allow)
                                    );
                                Directory.CreateDirectory(directoryName, dirSecurity);
                            }
                        }
                    }
                }
                else
                {
                    throw new ConnectorException("Only updatetype of replace is supported for home directory");
                }
            }
        }

        internal void UpdateDeFromCa_OpAtt_Enable(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry, 
            ConnectorAttribute attribute) {

            // set the proper flag in the userAccountControl bitfield
            PropertyValueCollection uacPvc =
                directoryEntry.Properties[UserAccountControl.UAC_ATTRIBUTE_NAME];

            UserAccountControl.Set(uacPvc,
                UserAccountControl.ACCOUNTDISABLE,
                // attribute is enable, but the flag is for
                // disable, so send the opposite
                !ConnectorAttributeUtil.GetBooleanValue(attribute));
        }

        internal void UpdateDeFromCa_OpAtt_PasswordExpired(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            bool? passwordExpired = ConnectorAttributeUtil.GetBooleanValue(attribute);
            if ((passwordExpired.HasValue) && (passwordExpired.Value == true))
            {
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_PWD_LAST_SET].Clear();
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_PWD_LAST_SET].Value = ActiveDirectoryUtils.GetLargeIntegerFromLong(0);
            }
            else
            {
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_PWD_LAST_SET].Clear();
                Int64 int64Value = -1;
                LargeInteger li = new LargeIntegerClass();
                li.HighPart = (int)(int64Value >> 32);
                li.LowPart = (int)(int64Value & 0xFFFFFFFF);
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_PWD_LAST_SET].Value = li;
                /*
                // this value can't be set (other than to zero) I'm throwing my own exception
                // here, because if not, AD thows this (at least on my machine):
                //      System.DirectoryServices.DirectoryServicesCOMException : A device attached to the system is not functioning. (Exception from HRESULT: 0x8007001F)
                throw new ConnectorException(_configuration.ConnectorMessages.Format(
                    "ex_PasswordMustBeReset",
                    "Password expiration can only be reset by reseting the password"));
                 */
            }
        }

        internal void UpdateDeFromCa_OpAtt_PasswordExpireDate(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            DateTime? expireDate = ConnectorAttributeUtil.GetDateTimeValue(attribute);
            if(expireDate.HasValue) {
            	// FIXME map from operational attribute to real attribute for password expiration
                directoryEntry.Properties[OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME].Value =
                    ActiveDirectoryUtils.GetLargeIntegerFromLong(expireDate.Value.ToFileTime());
            }
        }

        internal void UpdateDeFromCa_OpAtt_AccountExpireDate(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            DateTime? expireDate = ConnectorAttributeUtil.GetDateTimeValue(attribute);
            if(expireDate.HasValue) {
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_ACCOUNT_EXPIRES].Value =
                    ActiveDirectoryUtils.GetLargeIntegerFromLong(expireDate.Value.ToFileTime());
            }
        }

        internal void UpdateDeFromCa_OpAtt_Lockout(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            bool? lockout = ConnectorAttributeUtil.GetBooleanValue(attribute);
            if (lockout.HasValue)
            {
                long lockoutTime = lockout.Value ? DateTimeUtil.GetCurrentUtcTimeMillis() : 0;

                if(lockoutTime != 0) {
                    throw new ConnectorException(_configuration.ConnectorMessages.Format(
                        "ex_LockAccountNotAllowed", "Active Directory does not support locking users.  User may be unlocked only"));
                }
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_LOCKOUT_TIME].Value =
                    ActiveDirectoryUtils.GetLargeIntegerFromLong(lockoutTime);
            }
        }
        internal void UpdateDeFromCa_PasswordNeverExpires(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            bool? passwordNeverExpires = ConnectorAttributeUtil.GetBooleanValue(attribute);

            PropertyValueCollection pvc = 
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_USER_ACOUNT_CONTROL];
            UserAccountControl.Set(pvc,
                UserAccountControl.DONT_EXPIRE_PASSWORD, 
                passwordNeverExpires);
        }
        // supporting class not implemented in the framework
/*
        internal void UpdateDeFromCa_OpAtt_EnableDate(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
        }

        internal void UpdateDeFromCa_OpAtt_DisableDate(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            / *
            long? utcMilliDate = ConnectorAttributeUtil.GetLongValue(attribute);
            if(utcMilliDate == null) {
                return;
            }

            DateTime disableDate = DateTime.FromFileTimeUtc((long)utcMilliDate);
            
            LargeInteger disableTicks = new LargeIntegerClass();
            disableTicks.HighPart = (int)((disableDate.Ticks >> 32) & 0xFFFFFFFF);
            disableTicks.LowPart = (int)(disableDate.Ticks & 0xFFFFFFFF);
            
            PropertyValueCollection pvc = directoryEntry.Properties["accountExpires"];
            if ((pvc == null) || (pvc.Count == 0))
            {
                // if nothing there, add the value
                pvc.Add(disableTicks);
            }
            else
            {
                // set the value
                pvc[0] = disableTicks;
            }
            * /
        }
*/
        internal void UpdateDeFromCa_Att_TSAllowLogon(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetAllowLogon(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSInitialProgram(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetInitialProgram(type, directoryEntry,
                ConnectorAttributeUtil.GetStringValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSInitialProgramDir(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetInitialProgramDir(type, directoryEntry,
                ConnectorAttributeUtil.GetStringValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSMaxConnectionTime(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetMaxConnectionTime(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSMaxDisconnectionTime(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetMaxDisconnectionTime(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSMaxIdleTime(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetMaxIdleTime(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSConnectClientDrivesAtLogon(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetConnectClientDrivesAtLogon(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSConnectClientPrintersAtLogon(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetConnectClientPrintersAtLogon(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSDefaultToMainPrinter(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetDefaultToMainPrinter(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSBrokenConnectionAction(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetBrokenConnectionAction(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSReconnectionAction(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetReconnectionAction(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSEnableRemoteControl(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetEnableRemoteControl(type, directoryEntry,
                ConnectorAttributeUtil.GetIntegerValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSProfilePath(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetProfilePath(type, directoryEntry,
                ConnectorAttributeUtil.GetStringValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSHomeDirectory(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetHomeDirectory(type, directoryEntry,
                ConnectorAttributeUtil.GetStringValue(attribute));
        }

        internal void UpdateDeFromCa_Att_TSHomeDrive(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            TerminalServicesUtils.SetHomeDrive(type, directoryEntry,
                ConnectorAttributeUtil.GetStringValue(attribute));
        }

        internal void UpdateDeFromCa_Att_Generic(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute, Boolean caseInsensitive)
        {

            // null out the values if we are replacing attributes.
            if (type.Equals(UpdateType.REPLACE))
            {
                // There is a problem where some attributes cant be set
                // even if the value is just being set to the same as
                // before.  This especially comes up for RDN (such as 
                // cn and ou).  As a workaround, and for backward compatibility
                // with IDM, if the value is the same, just ignore it.

                // check equality
                IList<Object> attributeValue = attribute.Value;
                PropertyValueCollection pvc = directoryEntry.Properties[attribute.Name];
                if((attributeValue != null) && (pvc != null) && (attributeValue.Count == pvc.Count))
                {
                    Boolean valueEqual = true;

                    foreach (Object attValueObj in attributeValue)
                    {
                        if (!pvc.Contains(attValueObj))
                        {
                            valueEqual = false;
                            break;
                        }
                    }
                    if (valueEqual)
                    {
                        // the value is already set, so just return without doing anything
                        return;
                    }
                }
                directoryEntry.Properties[attribute.Name].Value = null;
            }

            if (attribute.Value == null)
            {
                return;
            }
                // if we are updating or adding, put the
            // new values in.
            if (type.Equals(UpdateType.ADD) ||
                type.Equals(UpdateType.REPLACE))
            {
                foreach (Object valueObject in attribute.Value)
                {
                    directoryEntry.Properties[attribute.Name].Add(valueObject);
                }
            }
            else if (type.Equals(UpdateType.DELETE))
            {
                // if deleting, find the values,
                // and remove them if they exist
                if (directoryEntry.Properties.Contains(attribute.Name))
                {
                    PropertyValueCollection pvc = directoryEntry.Properties[attribute.Name];

                    foreach (Object valueObject in attribute.Value)
                    {
                        bool foundAndRemoved = false;
                        if (caseInsensitive && valueObject is string)
                        {
                            // strings have to be compared in case-insensitive way (TODO: do some normalization for DNs as well...)
                            foreach (object existing in pvc)
                            {
                                bool equals;
                                if (existing is string)
                                {
                                    equals = (valueObject as string).Equals(existing as string, StringComparison.CurrentCultureIgnoreCase);
                                }
                                else
                                {
                                    LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "existing value is not a string, it is: {0}", existing.GetType());
                                    equals = valueObject.Equals(existing);
                                }
                                if (equals)
                                {
                                    pvc.Remove(existing);
                                    foundAndRemoved = true;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            if (pvc.Contains(valueObject))
                            {
                                pvc.Remove(valueObject);
                                foundAndRemoved = true;
                            }
                        }

                        if (!foundAndRemoved)
                        {
                            LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "Removing non-existing value {0} from {1}. Current values = {2}", valueObject, attribute.Name, DumpPVC(pvc));
                        }
                    }                
                }
                
            }
        }

        #endregion

        #region GetCaFromDe Handlers
        private ConnectorAttribute GetCaFromDe_Att_Generic(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttributeBuilder attributeBuilder = new ConnectorAttributeBuilder();

            if (attributeName == null)
            {
                return null;
            }
           
            attributeBuilder.Name = attributeName;

            ResultPropertyValueCollection pvc = null;
            if (searchResult.Properties.Contains(attributeName))
            {
                pvc = searchResult.Properties[attributeName];
            }
            
            if (pvc == null)
            {
                return null;
            }
            else
            {
                // TODO unify with CommonUtils.ConvertToSupportedForm
                // (requires either finding ConnectorAttributeInfo here or making that method not dependable on such information)
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
                        if (LOGGER.Switch.ShouldTrace(TraceEventType.Verbose)) {
                            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT,
                                "Unsupported attribute type ... calling ToString (Name: \'{0}\'({1}) Type: \'{2}\' String Value: \'{3}\'",
                                attributeName, i, pvc[i].GetType(), pvc[i].ToString());
                        }
                        attributeBuilder.AddValue(pvc[i].ToString());
                    }
                }
            }

            return attributeBuilder.Build();
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_GroupMembers(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry directoryEntry)
        {
            ConnectorAttributeBuilder attributeBuilder = new ConnectorAttributeBuilder();

            if (attributeName == null)
            {
                return null;
            }
           
            attributeBuilder.Name = attributeName;

            ResultPropertyValueCollection pvc = null;
            if (searchResult.Properties.Contains(attributeName))
            {
                pvc = searchResult.Properties[attributeName];
            }
            else
                // Group without members
            {
                return null;
            }

            // Range issue most probably...
            // see: http://msdn.microsoft.com/en-us/library/ms817827.aspx
            if (pvc.Count == 0)
            {
                DirectoryEntry entry = null;
                DirectorySearcher searcher = null;
                
                int first = 0;
                int last = first + (GRP_MEMBERS_MAXRANGE - 1);
                bool badQuery = false;
                bool quit = false;
                string memberRange;

                try
                {
                    entry = searchResult.GetDirectoryEntry();
                    searcher = new DirectorySearcher(entry);
                    searcher.Filter = "(objectClass=*)";
                    do
                    {
                        if (!badQuery)
                        {
                            memberRange = String.Format("member;range={0}-{1}", first, last);
                        }
                        else
                        {
                            memberRange = String.Format("member;range={0}-*", first);
                        }
                        searcher.PropertiesToLoad.Clear();
                        searcher.PropertiesToLoad.Add(memberRange);
                        DS.SearchResult sresult = searcher.FindOne();
                        if (sresult.Properties.Contains(memberRange))
                        {
                            foreach (object valueObject in sresult.Properties[memberRange])
                            {
                                if ((valueObject == null) || (FrameworkUtil.IsSupportedAttributeType(valueObject.GetType())))
                                {
                                    attributeBuilder.AddValue(valueObject);
                                }
                            }
                            if (badQuery)
                            {
                                quit = true;
                            }
                        }
                        else
                        {
                            badQuery = true;
                        }
                        if (!badQuery)
                        {
                            first = last + 1;
                            last = first + (GRP_MEMBERS_MAXRANGE - 1);
                        }
                    }
                    while (!quit);
                }
                catch (Exception ex)
                {
                }
                finally
                {
                    if (entry != null)
                    {
                        entry.Dispose();
                    }
                    if (searcher != null)
                    {
                        searcher.Dispose();
                    }
                }
                return attributeBuilder.Build();
            }
            else if (pvc == null)
            {
                return null;
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
                        if (LOGGER.Switch.ShouldTrace(TraceEventType.Verbose)) {
                            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT,
                                "Unsupported attribute type ... calling ToString (Name: \'{0}\'({1}) Type: \'{2}\' String Value: \'{3}\'",
                                attributeName, i, pvc[i].GetType(), pvc[i].ToString());
                        }
                        attributeBuilder.AddValue(pvc[i].ToString());
                    }
                }
            }
            return attributeBuilder.Build();
        }
        private ConnectorAttribute GetCaFromDe_OpAtt_Name(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            String value = null;
            ResultPropertyValueCollection pvc = null;

            pvc = searchResult.Properties[ActiveDirectoryConnector.ATT_DISTINGUISHED_NAME];
            if ((pvc != null) && (pvc.Count == 1) && (pvc[0] is String))
            {
                value = (String)pvc[0];
            }
            else
            {
                throw new ConnectorException("There should be exactly one value for the name attribute");
            }          

            return ConnectorAttributeBuilder.Build(Name.NAME, ActiveDirectoryUtils.NormalizeLdapString(value));
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Uid(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ICollection<Object> value = new List<Object>();

            // uid is objectGuid
            ResultPropertyValueCollection pvc =
                searchResult.Properties[ActiveDirectoryConnector.ATT_OBJECT_GUID];

            if ((pvc.Count == 1) && (pvc[0] is Byte[]))
            {
                value.Add(ActiveDirectoryUtils.ConvertUIDBytesToGUIDString((Byte[])pvc[0]));
            }
            else if (pvc.Count > 1)
            {
                throw new ConnectorException("There should be only one UID, but multiple values were specified");
            }

            return ConnectorAttributeBuilder.Build(Uid.NAME, value);
        }

        private ConnectorAttribute GetCaFromDe_Att_Container(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            if (searchResult == null)
            {
                return null;
            }

            DirectoryEntry parentDe = entry.Parent;
            String container = "";
            if (parentDe != null)
            {
                container = ActiveDirectoryUtils.GetDnFromPath(parentDe.Path);
                parentDe.Dispose();
            }
            return ConnectorAttributeBuilder.Build(
                ActiveDirectoryConnector.ATT_CONTAINER, container);
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Groups(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_MEMBEROF, searchResult, entry);
            if (realAttribute == null)
            {
                return null;
            }
            else
            {
                return ConnectorAttributeBuilder.Build(PredefinedAttributes.GROUPS_NAME,
                    realAttribute.Value);
            }
        }
        
        private ConnectorAttribute GetCaFromDe_OpAtt_Accounts(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_MEMBER, searchResult, entry);
            if (realAttribute == null)
            {
                return null;
            }
            else
            {
                return ConnectorAttributeBuilder.Build(ActiveDirectoryConnector.ATT_ACCOUNTS,
                    realAttribute.Value);
            }
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Enabled(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            if (searchResult == null)
            {
                return null;
            }
            bool disabled = UserAccountControl.IsSet(
                entry.Properties[UserAccountControl.UAC_ATTRIBUTE_NAME],
                UserAccountControl.ACCOUNTDISABLE);
            return ConnectorAttributeBuilder.BuildEnabled(!disabled);
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_PasswordExpired(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_PWD_LAST_SET, searchResult, entry);
            if (realAttribute != null)
            {
                long? lastSetDate = ConnectorAttributeUtil.GetLongValue(realAttribute);
                if ((lastSetDate.HasValue) && (lastSetDate.Value != 0))
                {
                    return ConnectorAttributeBuilder.BuildPasswordExpired(false);
                }
                else
                {
                    return ConnectorAttributeBuilder.BuildPasswordExpired(true);
                }
            }
            return null;
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Description(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttribute realDescription = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_DESCRIPTION, searchResult, entry);

            if (realDescription != null)
            {
                string description = ConnectorAttributeUtil.GetStringValue(realDescription);

                if (description != null)
                {
                    return ConnectorAttributeBuilder.Build(PredefinedAttributes.DESCRIPTION, description);
                }
            }
            return null;
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_ShortName(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttribute realShortName = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_SHORT_NAME, searchResult, entry);

            if (realShortName != null)
            {
                string shortName = ConnectorAttributeUtil.GetStringValue(realShortName);

                if (shortName != null)
                {
                    return ConnectorAttributeBuilder.Build(PredefinedAttributes.SHORT_NAME, shortName);
                }
            }
            return null;
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_PasswordExpireDate(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            // get the value from ad
            // FIXME map between operational attribute and real AD attribute name
            ConnectorAttribute accountExpireAttribute = GetCaFromDe_Att_Generic(
                oclass, OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME, searchResult, entry);

            // now change name
            if (accountExpireAttribute != null)
            {
                long? expireValue = ConnectorAttributeUtil.GetLongValue(accountExpireAttribute);
                // if value present and not set to never expires
                if ((expireValue != null) && (!expireValue.Value.Equals(9223372036854775807)))
                {
                    DateTime expireDate = DateTime.FromFileTime(expireValue.Value);
                    return ConnectorAttributeBuilder.BuildPasswordExpirationDate(expireDate);
                }
                else
                {
                    return null;
                }
            }
            return null;
        }
        
        // copied from ConnectorAttributeBuilder
        private ConnectorAttribute BuildAccountExpirationDate(DateTime dateTime)
        {
            return BuildPasswordExpirationDate(DateTimeUtil.GetUtcTimeMillis(dateTime));
        }

        private ConnectorAttribute BuildPasswordExpirationDate(long dateTime)
        {
            return ConnectorAttributeBuilder.Build(ActiveDirectoryConnector.ATT_ACCOUNT_EXPIRES, dateTime);
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_AccountExpireDate(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            // get the value from ad
            ConnectorAttribute accountExpireAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_ACCOUNT_EXPIRES, searchResult, entry);

            // now change name
            if (accountExpireAttribute != null)
            {
                long? expireValue = ConnectorAttributeUtil.GetLongValue(accountExpireAttribute);
                // if value present and not set to never expires
                if ((expireValue != null) && (!expireValue.Value.Equals(9223372036854775807)))
                {
                    DateTime expireDate = DateTime.FromFileTime(expireValue.Value);
                    return BuildAccountExpirationDate(expireDate);
                }
                else
                {
                    return null;
                }
            }
            return null;
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Lockout(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            bool locked = false;

            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_LOCKOUT_TIME, searchResult, entry);
            if (realAttribute != null)
            {
                long? lockoutDate = ConnectorAttributeUtil.GetLongValue(realAttribute);
                if ((lockoutDate.HasValue) && (lockoutDate.Value != 0))
                {
                    // if there is a date (non zero), then the account
                    // is locked
                    locked = true;
                }
            }
            return ConnectorAttributeBuilder.BuildLockOut(locked);
        }

        private ConnectorAttribute GetCaFromDe_PasswordNeverExpires(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            ConnectorAttribute ca = null;
            if(entry != null) {
                PropertyValueCollection pvc = 
                    entry.Properties[ActiveDirectoryConnector.ATT_USER_ACOUNT_CONTROL];
                if (pvc != null)
                {
                    bool pne = UserAccountControl.IsSet(pvc, 
                        UserAccountControl.DONT_EXPIRE_PASSWORD);
                    ca = ConnectorAttributeBuilder.Build(attributeName, pne);
                }
            }
            return ca;
        }

        // supporting class not implemented in the framework
/*
        private ConnectorAttribute GetCaFromDe_OpAtt_EnableDate(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return null;
        }
            
        private ConnectorAttribute GetCaFromDe_OpAtt_DisableDate(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {   
            / *
            if (searchResult == null)
            {
                return null;
            }

            ResultPropertyValueCollection rpvc = 
                searchResult.Properties["accountExpires"];
            if(rpvc.Count == 0) {
                return null;
            }

            long ticks = (long)rpvc[0];
            if ((ticks < DateTime.MinValue.Ticks) || (ticks > DateTime.MaxValue.Ticks))
            {
                return null;
            }

            DateTime disableDate = new DateTime(ticks);

            return ConnectorAttributeBuilder.BuildDisableDate(disableDate);
           * /
            return null;
        }
 */

        private ConnectorAttribute GetCaFromDe_Att_TSInitialProgram(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
           return ReturnConnectorAttribute(TerminalServicesUtils.TS_INITIAL_PROGRAM,
                                            TerminalServicesUtils.GetInitialProgram(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSInitalProgramDir(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_INITIAL_PROGRAM_DIR, 
                TerminalServicesUtils.GetInitialProgramDir(entry));
        }

        
        private ConnectorAttribute GetCaFromDe_Att_TSAllowLogon(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_ALLOW_LOGON, 
                TerminalServicesUtils.GetAllowLogon(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSMaxConnectionTime(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_MAX_CONNECTION_TIME, 
                TerminalServicesUtils.GetMaxConnectionTime(entry));
        }
        
        private ConnectorAttribute GetCaFromDe_Att_TSMaxDisconnectionTime(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_MAX_DISCONNECTION_TIME, 
                TerminalServicesUtils.GetMaxDisconnectionTime(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSMaxIdleTime(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_MAX_IDLE_TIME, 
                TerminalServicesUtils.GetMaxIdleTime(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSConnectClientDrivesAtLogon(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_CONNECT_CLIENT_DRIVES_AT_LOGON, 
                TerminalServicesUtils.GetConnectClientDrivesAtLogon(entry));
        }
        
        private ConnectorAttribute GetCaFromDe_Att_TSConnectClientPrintersAtLogon(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(
                TerminalServicesUtils.TS_CONNECT_CLIENT_PRINTERS_AT_LOGON, 
                TerminalServicesUtils.GetConnectClientPrintersAtLogon(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSDefaultToMainPrinter(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(
                TerminalServicesUtils.TS_DEFAULT_TO_MAIN_PRINTER, 
                TerminalServicesUtils.GetDefaultToMainPrinter(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSBrokenConnectionAction(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(
                TerminalServicesUtils.TS_BROKEN_CONNECTION_ACTION, 
                TerminalServicesUtils.GetBrokenConnectionAction(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSReconnectionAction(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_RECONNECTION_ACTION, 
                TerminalServicesUtils.GetReconnectionAction(entry));
        }
       
        private ConnectorAttribute GetCaFromDe_Att_TSEnableRemoteControl(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_ENABLE_REMOTE_CONTROL, 
                TerminalServicesUtils.GetEnableRemoteControl(entry));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSProfilePath(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_PROFILE_PATH, 
                TerminalServicesUtils.GetProfilePath(entry));
        }
        private ConnectorAttribute GetCaFromDe_Att_TSHomeDirectory(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_HOME_DIRECTORY, 
                TerminalServicesUtils.GetHomeDirectory(entry));
        }
        private ConnectorAttribute GetCaFromDe_Att_TSHomeDrive(
            ObjectClass oclass, string attributeName, DS.SearchResult searchResult, DirectoryEntry entry)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_HOME_DRIVE, 
                TerminalServicesUtils.GetHomeDrive(entry));
        }

        #endregion

        internal ConnectorAttribute ReturnConnectorAttribute<T>
            (string name, T value) {
            ConnectorAttribute newAttribute = null;

            if (value != null)
            {
                newAttribute = ConnectorAttributeBuilder.Build(
                    name, value);
            }
            return newAttribute;
        }

        internal static string ToRealName(string name)
        {
            if (NameUtil.IsSpecialName(name))
            {
                if (name.Equals(Name.NAME))
                {
                    return ActiveDirectoryConnector.ATT_DISTINGUISHED_NAME;
                }
                else if (name.Equals(PredefinedAttributes.SHORT_NAME))
                {
                    return ActiveDirectoryConnector.ATT_SHORT_NAME;
                }
                else if (name.Equals(PredefinedAttributes.DESCRIPTION))
                {
                    return ActiveDirectoryConnector.ATT_DESCRIPTION;
                }
                else
                {
                    throw new ArgumentException("Cannot translate " + name + " to AD attribute name");
                }
            }
            else
            {
                return name;
            }
        }
    }
    
}
