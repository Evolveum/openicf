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
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Security;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common;
using Org.IdentityConnectors.Framework.Spi.Operations;
using System.DirectoryServices;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Common.Security;
using System.Diagnostics;
using ActiveDs;
using System.IO;
using System.Security.AccessControl;
using System.Security.Principal;
using Org.IdentityConnectors.Common;

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
        // names from active directory attributes to ignore during
        // generic translation 
        IList<string> IgnoreADAttributeNames_account = new List<string>();
        IList<string> IgnoreADAttributeNames_group = new List<string>();

        // names from connector attributes to ignore during
        // generic translation 
        IList<string> IgnoreConnectorAttributeNames_account = new List<string>();
        IList<string> IgnoreConnectorAttributeNames_group = new List<string>();

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

            // methods to update a directory entry from a connectorattribute
            UpdateDeFromCaDelegates.Add(PredefinedAttributes.ACCOUNTS_NAME,
                UpdateDeFromCa_OpAtt_Accounts);
            UpdateDeFromCaDelegates.Add(PredefinedAttributes.GROUPS_NAME,
                UpdateDeFromCa_OpAtt_Groups);
            UpdateDeFromCaDelegates.Add(ActiveDirectoryConnector.ATT_HOME_DIRECTORY,
                UpdateDeFromCa_Att_HomeDirectory);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.ENABLE_NAME,
                UpdateDeFromCa_OpAtt_Enable);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.PASSWORD_EXPIRED_NAME,
                UpdateDeFromCa_OpAtt_PasswordExpired);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,
                UpdateDeFromCa_OpAtt_PasswordExpireDate);
            UpdateDeFromCaDelegates.Add(OperationalAttributes.LOCK_OUT_NAME,
                UpdateDeFromCa_OpAtt_Lockout);
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
            GetCaFromDeDelegates.Add(PredefinedAttributes.ACCOUNTS_NAME,
                GetCaFromDe_OpAtt_Accounts);
            GetCaFromDeDelegates.Add(PredefinedAttributes.GROUPS_NAME,
                GetCaFromDe_OpAtt_Groups);
            GetCaFromDeDelegates.Add(OperationalAttributes.ENABLE_NAME,
                GetCaFromDe_OpAtt_Enabled);
            GetCaFromDeDelegates.Add(OperationalAttributes.PASSWORD_EXPIRED_NAME,
                GetCaFromDe_OpAtt_PasswordExpired);
            GetCaFromDeDelegates.Add(OperationalAttributes.LOCK_OUT_NAME,
                GetCaFromDe_OpAtt_Lockout);
            GetCaFromDeDelegates.Add(OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,
                GetCaFromDe_OpAtt_PasswordExpireDate);
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
            else if (oclass.Equals(ObjectClass.GROUP))
            {
                ignoreList = IgnoreConnectorAttributeNames_group;
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
                UpdateDeFromCa_Att_Generic(oclass, type, directoryEntry, attribute);
            }
        }


        internal ConnectorAttribute GetCaFromDe(ObjectClass oclass, 
            string attributeName, SearchResult searchResult)
        {
            ConnectorAttribute attribute = null;

            if (GetCaFromDeDelegates.ContainsKey(attributeName))
            {
                // if it's an attribute with a special handler,
                // call the handler
                GetCaFromDe_delegate handler = GetCaFromDeDelegates[attributeName];
                attribute = handler(oclass, attributeName, searchResult);
            }
            else
            {
                // if none of the above, call the generic handler
                attribute = GetCaFromDe_Att_Generic(oclass, attributeName, searchResult);
            }

            return attribute;
        }

        internal delegate void UpdateDeFromCa_delegate(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute);

        internal delegate ConnectorAttribute GetCaFromDe_delegate(ObjectClass oclass,
            string attributeName, SearchResult searchResult);

        public void GetAddsAndDeletes(ICollection<Object>valuesToAdd, ICollection<Object>valuesToRemove,
            PropertyValueCollection oldValues, ICollection<Object>newValues, UpdateType type) {
                if (UpdateType.ADD.Equals(type))
                {
                    // add all groups
                    foreach (Object value in newValues)
                    {
                        valuesToAdd.Add(value);
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
                            if (!newValues.Contains(value))
                            {
                                valuesToRemove.Add(value);
                            }
                        }
                    }

                    // look through the values passed in and
                    // add them if they are not existing values
                    foreach (Object value in newValues)
                    {
                        if ((oldValues == null) || (!oldValues.Contains(value)))
                        {
                            valuesToAdd.Add(value);
                        }
                    }
                }
                else if (UpdateType.DELETE.Equals(type))
                {
                    foreach (Object value in newValues)
                    {
                        valuesToRemove.Add(value);
                    }
                }
        }

        #region UpdateDeFromCa handlers



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

                GetAddsAndDeletes(groupsToAdd, groupsToRemove, oldValues, newValues, type);
                
                foreach (Object obj in groupsToRemove)
                {
                    // lookup the group and remove this user from group if it's a
                    // valid group.
                    String groupPath = ActiveDirectoryUtils.GetLDAPPath(
                        _configuration.LDAPHostName, (String)obj);
                    DirectoryEntry groupDe = new DirectoryEntry(groupPath,
                        _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
                    String distinguishedName = ActiveDirectoryUtils.GetDnFromPath(directoryEntry.Path);
                    groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER].Remove(distinguishedName);
                    groupDe.CommitChanges();
                }

                foreach (Object obj in groupsToAdd)
                {
                    // lookup the group and add this user to group if it's a
                    // valid group.
                    String groupPath = ActiveDirectoryUtils.GetLDAPPath(
                        _configuration.LDAPHostName, (String)obj);
                    DirectoryEntry groupDe = new DirectoryEntry(groupPath,
                        _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);
                    String distinguishedName = ActiveDirectoryUtils.GetDnFromPath(directoryEntry.Path);
                    groupDe.Properties[ActiveDirectoryConnector.ATT_MEMBER].Add(distinguishedName);
                    groupDe.CommitChanges();
                }
            }
            else
            {
                throw new ConnectorException(
                    String.Format("''{0}'' is an invalid attribute for object class ''{1}''",
                        PredefinedAttributeInfos.GROUPS, oclass.GetObjectClassValue()));
            }
        }

        internal void UpdateDeFromCa_OpAtt_Accounts(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry, 
            ConnectorAttribute attribute) 
        {
            if (ObjectClass.GROUP.Equals(oclass))
            {
                // create an 'attribute' with the real name, and then call the 
                // generic version
                ConnectorAttribute newAttribute = ConnectorAttributeBuilder.Build(
                    ActiveDirectoryConnector.ATT_MEMBER, attribute.Value);
                UpdateDeFromCa_Att_Generic(oclass, type, directoryEntry, newAttribute);
            }
            else
            {
                throw new ConnectorException(
                    String.Format("'{0}' is an invalid attribute for object class '{1}'",
                        PredefinedAttributeInfos.ACCOUNTS, oclass.GetObjectClassValue()));
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
                    UpdateDeFromCa_Att_Generic(oclass, type, directoryEntry, attribute);
                    
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
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_PWD_LAST_SET].Value = GetLargeIntegerFromLong(0);
            }
            else
            { 
                // this value can't be set (other than to zero) I'm throwing my own exception
                // here, because if not, AD thows this (at least on my machine):
                //      System.DirectoryServices.DirectoryServicesCOMException : A device attached to the system is not functioning. (Exception from HRESULT: 0x8007001F)
                throw new ConnectorException(_configuration.ConnectorMessages.Format(
                    "ex_PasswordMustBeReset",
                    "Password expiration can only be reset by reseting the password"));
            }
        }

        internal void UpdateDeFromCa_OpAtt_PasswordExpireDate(ObjectClass oclass,
            UpdateType type, DirectoryEntry directoryEntry,
            ConnectorAttribute attribute)
        {
            DateTime? expireDate = ConnectorAttributeUtil.GetDateTimeValue(attribute);
            if(expireDate.HasValue) {
                directoryEntry.Properties[ActiveDirectoryConnector.ATT_ACCOUNT_EXPIRES].Value =
                    GetLargeIntegerFromLong((ulong)expireDate.Value.ToFileTime());
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
                    GetLargeIntegerFromLong((ulong)lockoutTime);
            }
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
            ConnectorAttribute attribute)
        {

            // null out the values if we are replacing attributes.
            if (type.Equals(UpdateType.REPLACE))
            {
                directoryEntry.Properties[attribute.Name].Value = null;
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
                        if (pvc.Contains(valueObject))
                        {
                            pvc.Remove(valueObject);
                        }
                    }
                }
                
            }
        }

        #endregion

        #region GetCaFromDe Handlers
        private ConnectorAttribute GetCaFromDe_Att_Generic(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
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
                            attributeName, i, pvc[i].GetType(), pvc[i].ToString());
                        attributeBuilder.AddValue(pvc[i].ToString());
                    }
                }
            }

            return attributeBuilder.Build();
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Name(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
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
            ObjectClass oclass, string attributeName, SearchResult searchResult)
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
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            if (searchResult == null)
            {
                return null;
            }

            DirectoryEntry parentDe = searchResult.GetDirectoryEntry().Parent;
            String container = "";
            if (parentDe != null)
            {
                container = ActiveDirectoryUtils.GetDnFromPath(parentDe.Path);
            }

            return ConnectorAttributeBuilder.Build(
                ActiveDirectoryConnector.ATT_CONTAINER, container);
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Groups(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_MEMBEROF, searchResult);
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
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_MEMBER, searchResult);
            if (realAttribute == null)
            {
                return null;
            }
            else
            {
                return ConnectorAttributeBuilder.Build(PredefinedAttributes.ACCOUNTS_NAME,
                    realAttribute.Value);
            }
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Enabled(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            if (searchResult == null)
            {
                return null;
            }

            bool disabled = UserAccountControl.IsSet(
                searchResult.GetDirectoryEntry().Properties[UserAccountControl.UAC_ATTRIBUTE_NAME],
                UserAccountControl.ACCOUNTDISABLE);

            return ConnectorAttributeBuilder.BuildEnabled(!disabled);
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_PasswordExpired(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_PWD_LAST_SET, searchResult);
            long? lastSetDate = ConnectorAttributeUtil.GetLongValue(realAttribute);
            if ((lastSetDate.HasValue) && (lastSetDate.Value != 0))
            {
                return ConnectorAttributeBuilder.BuildPasswordExpired(false);
            }

            return ConnectorAttributeBuilder.BuildPasswordExpired(true);
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_PasswordExpireDate(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            // get the value from ad
            ConnectorAttribute accountExpireAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_ACCOUNT_EXPIRES, searchResult);

            // now change name
            if (accountExpireAttribute != null)
            {
                long? expireValue = ConnectorAttributeUtil.GetLongValue(accountExpireAttribute);
                if (expireValue != null)
                {
                    return ConnectorAttributeBuilder.BuildPasswordExpirationDate(expireValue.Value);
                }
                else
                {
                    return null;
                }
            }
            return null;
        }

        private ConnectorAttribute GetCaFromDe_OpAtt_Lockout(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            bool locked = false;

            ConnectorAttribute realAttribute = GetCaFromDe_Att_Generic(
                oclass, ActiveDirectoryConnector.ATT_LOCKOUT_TIME, searchResult);
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
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_INITIAL_PROGRAM, 
                TerminalServicesUtils.GetInitialProgram(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSInitalProgramDir(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_INITIAL_PROGRAM_DIR, 
                TerminalServicesUtils.GetInitialProgramDir(searchResult));
        }

        
        private ConnectorAttribute GetCaFromDe_Att_TSAllowLogon(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_ALLOW_LOGON, 
                TerminalServicesUtils.GetAllowLogon(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSMaxConnectionTime(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_MAX_CONNECTION_TIME, 
                TerminalServicesUtils.GetMaxConnectionTime(searchResult));
        }
        
        private ConnectorAttribute GetCaFromDe_Att_TSMaxDisconnectionTime(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_MAX_DISCONNECTION_TIME, 
                TerminalServicesUtils.GetMaxDisconnectionTime(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSMaxIdleTime(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_MAX_IDLE_TIME, 
                TerminalServicesUtils.GetMaxIdleTime(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSConnectClientDrivesAtLogon(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_CONNECT_CLIENT_DRIVES_AT_LOGON, 
                TerminalServicesUtils.GetConnectClientDrivesAtLogon(searchResult));
        }
        
        private ConnectorAttribute GetCaFromDe_Att_TSConnectClientPrintersAtLogon(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(
                TerminalServicesUtils.TS_CONNECT_CLIENT_PRINTERS_AT_LOGON, 
                TerminalServicesUtils.GetConnectClientPrintersAtLogon(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSDefaultToMainPrinter(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(
                TerminalServicesUtils.TS_DEFAULT_TO_MAIN_PRINTER, 
                TerminalServicesUtils.GetDefaultToMainPrinter(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSBrokenConnectionAction(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(
                TerminalServicesUtils.TS_BROKEN_CONNECTION_ACTION, 
                TerminalServicesUtils.GetBrokenConnectionAction(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSReconnectionAction(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_RECONNECTION_ACTION, 
                TerminalServicesUtils.GetReconnectionAction(searchResult));
        }
       
        private ConnectorAttribute GetCaFromDe_Att_TSEnableRemoteControl(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_ENABLE_REMOTE_CONTROL, 
                TerminalServicesUtils.GetEnableRemoteControl(searchResult));
        }

        private ConnectorAttribute GetCaFromDe_Att_TSProfilePath(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_PROFILE_PATH, 
                TerminalServicesUtils.GetProfilePath(searchResult));
        }
        private ConnectorAttribute GetCaFromDe_Att_TSHomeDirectory(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_HOME_DIRECTORY, 
                TerminalServicesUtils.GetHomeDirectory(searchResult));
        }
        private ConnectorAttribute GetCaFromDe_Att_TSHomeDrive(
            ObjectClass oclass, string attributeName, SearchResult searchResult)
        {
            return ReturnConnectorAttribute(TerminalServicesUtils.TS_HOME_DRIVE, 
                TerminalServicesUtils.GetHomeDrive(searchResult));
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

        // gets a long from a LargeInteger (COM object)
        ulong GetLongFromLargeInteger(LargeInteger largeInteger)
        {
            ulong longValue = ((ulong)largeInteger.HighPart) << 32;
            longValue += (ulong)largeInteger.LowPart;
            return longValue;
        }

        // sets a LargeInteger (COM object) from a long
        LargeInteger GetLargeIntegerFromLong(ulong longValue)
        {
            LargeInteger largeInteger = new LargeIntegerClass();
            largeInteger.HighPart = (int)((longValue & 0xFFFFFFFF00000000) >> 32);
            largeInteger.LowPart = (int)(longValue & 0x00000000FFFFFFFF);
            return largeInteger;
        }
    }
    
}
