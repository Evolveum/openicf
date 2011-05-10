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
 */
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.DirectoryServices.Protocols;
using System.Linq;
using System.Net;
using System.Security.Permissions;
using System.Text;
using System.DirectoryServices;
using Org.IdentityConnectors.Common.Security;
using ActiveDs;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using System.DirectoryServices.AccountManagement;
using System.DirectoryServices.ActiveDirectory;
using System.Threading;
using Org.IdentityConnectors.Framework.Common.Objects;
using System.Security.Principal;
using System.Runtime.InteropServices;

namespace Org.IdentityConnectors.ActiveDirectory
{
    internal class AuthenticationHelper
    {
        // errors are documented in winerror.h
        internal static readonly int ERROR_PASSWORD_MUST_CHANGE = 1907;
        internal static readonly int ERROR_LOGON_FAILURE = 1326;
        internal static readonly int ERROR_ACCOUNT_LOCKED_OUT = 1909;
        internal static readonly int ERROR_ACCOUNT_EXPIRED = 1793;

        [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        public static extern bool LogonUser(String lpszUsername, String lpszDomain, String lpszPassword,
            int dwLogonType, int dwLogonProvider, ref IntPtr phToken);

        [DllImport("kernel32.dll", CharSet = CharSet.Auto)]
        public extern static bool CloseHandle(IntPtr handle);

        private ActiveDirectoryConfiguration _configuration = null;

        internal AuthenticationHelper(ActiveDirectoryConfiguration configuration)
        {
            _configuration = configuration;
        }

        // Advice from Microsoft - If you incorporate this code into a DLL, be sure to 
        // demand FullTrust.  See full article in WindowsIdentity.Impersonate() documentation:  
        // http://msdn.microsoft.com/en-us/library/chf6fbt4.aspx
        [PermissionSet(SecurityAction.Demand, Name = "FullTrust")]
        internal Uid ValidateUserCredentials(string username, string password)
        {
            IntPtr tokenHandle = new IntPtr(0);
            try
            {
                const int LOGON32_PROVIDER_DEFAULT = 0;
                const int LOGON32_LOGON_INTERACTIVE = 2;
                const int LOGON32_LOGON_NETWORK = 3;

                tokenHandle = IntPtr.Zero;
                bool success = LogonUser(username, null, password, LOGON32_LOGON_NETWORK,
                                         LOGON32_PROVIDER_DEFAULT, ref tokenHandle);
                if(!success)
                {
                    int lastWindowsError = Marshal.GetLastWin32Error(); 
                    if(lastWindowsError == ERROR_PASSWORD_MUST_CHANGE)
                    {
                        string message = _configuration.ConnectorMessages.Format("ex_PasswordMustChange",
                                                         "User password must be changed");

                        PasswordExpiredException pweException = new PasswordExpiredException(message);
                        pweException.Uid = GetUidFromSamAccountName(username);

                        throw pweException;
                    }
                    else if(lastWindowsError == ERROR_LOGON_FAILURE)
                    {
                        string message = _configuration.ConnectorMessages.Format(
                            "ex_InvalidCredentials", "Invalid credentials supplied for user {0}", username);
                        
                        throw new InvalidCredentialException(message);
                    }
                    else if (lastWindowsError == ERROR_ACCOUNT_LOCKED_OUT)
                    {
                        string message = _configuration.ConnectorMessages.Format("ex_AccountLocked",
                                                                                 "User's account has been locked");
                        throw new InvalidCredentialException(message);
                    }
                    else if (lastWindowsError == ERROR_ACCOUNT_EXPIRED)
                    {
                        string message = _configuration.ConnectorMessages.Format("ex_AccountExpired",
                                                                                 "User account expired for user {0}", username);
                        throw new InvalidCredentialException(message);
                    }
                    else
                    {
                        // no idea what could have gone wrong, so log it and throw connector error
                        string errorMessage = string.Format(
                            "Windows returned error number {0} from LogonUser call", lastWindowsError);
                        Trace.TraceError(errorMessage);
                        //TODO: Add localization
                        throw new ConnectorException(errorMessage);
                    }
                }
                WindowsIdentity windowsId = new WindowsIdentity(tokenHandle);
                Uid uid = GetUidFromSid(windowsId.User);
                return uid;
            }
            catch(Exception e)
            {
                Trace.TraceError(e.Message);
                throw;
            }
            finally
            {
                if(tokenHandle != IntPtr.Zero)
                {
                    CloseHandle(tokenHandle);
                }                
            }
        }

        internal Uid GetUidFromSid(SecurityIdentifier accountSid)
        {
            string sidString = "<SID=" + accountSid.Value + ">";
            DirectoryEntry userDe = new DirectoryEntry(
                ActiveDirectoryUtils.GetLDAPPath(_configuration.LDAPHostName, sidString),
                _configuration.DirectoryAdminName, _configuration.DirectoryAdminPassword);

            return new Uid(ActiveDirectoryUtils.ConvertUIDBytesToGUIDString(userDe.Guid.ToByteArray()));
        }

        internal Uid GetUidFromSamAccountName(String sAMAccountName)
        {
            WindowsIdentity windowsId = new WindowsIdentity(sAMAccountName);

            try
            {
                if (windowsId.User == null)
                {
                    throw new ConnectorException(_configuration.ConnectorMessages.Format(
                    "ex_SIDLookup", "An execption occurred during validation of user {0}.  The user's sid could not be determined.",
                    sAMAccountName));
                }
                return GetUidFromSid(windowsId.User);
            }
            finally
            {
                if (windowsId != null)
                {
                    windowsId.Dispose();
                    windowsId = null;
                }
            }
        }
    } 

    /** 
     * This class will decrypt passwords, and handle
     * authentication and password changes (both
     * administrative and user)
     */
    internal class PasswordChangeHandler
    {
        String _currentPassword;
        String _newPassword;
        ActiveDirectoryConfiguration _configuration = null;
        static Semaphore authenticationSem = new Semaphore(1, 1, "ActiveDirectoryConnectorAuthSem");
        static readonly int ERR_PASSWORD_MUST_BE_CHANGED = -2147022989;
        static readonly int ERR_PASSWORD_EXPIRED = -2147023688;


        internal PasswordChangeHandler(ActiveDirectoryConfiguration configuration)
        {
            _configuration = configuration;
        }

        /// <summary>
        /// sets the _currentPassword variable
        /// </summary>
        /// <param name="clearChars"></param>
        internal void setCurrentPassword(UnmanagedArray<char> clearChars)
        {
            _currentPassword = "";

            // build up the string from the unmanaged array
            for (int i = 0; i < clearChars.Length; i++)
            {
                _currentPassword += clearChars[i];
            }
        }

        /// <summary>
        /// Sets the _newPassword variable
        /// </summary>
        /// <param name="clearChars"></param>
        internal void setNewPassword(UnmanagedArray<char> clearChars)
        {
            _newPassword = "";

            // build up the string from the unmanaged array
            for (int i = 0; i < clearChars.Length; i++)
            {
                _newPassword += clearChars[i];
            }
        }

        /// <summary>
        /// Does an administrative password change.  The Directory
        /// entry must be created with username and password of 
        /// a user with permission to change the password
        /// </summary>
        /// <param name="directoryEntry"></param>
        /// <param name="gsNewPassword"></param>
        internal void changePassword(DirectoryEntry directoryEntry,
            GuardedString gsNewPassword)
        {
            // decrypt and save the new password
            gsNewPassword.Access(setNewPassword);

            // get the native com object as an IADsUser, and set the 
            // password
            IADsUser user = (IADsUser)directoryEntry.NativeObject;
            user.SetPassword(_newPassword);
        }

        /// <summary>
        /// Does a user password change.  Must supply the currentpassword
        /// and the new password
        /// </summary>
        /// <param name="directoryEntry"></param>
        /// <param name="gsCurrentPassword"></param>
        /// <param name="gsNewPassword"></param>
        internal void changePassword(DirectoryEntry directoryEntry,
            GuardedString gsCurrentPassword, GuardedString gsNewPassword)
        {
            // decrypt and save the old nad new passwords
            gsNewPassword.Access(setNewPassword);
            gsCurrentPassword.Access(setCurrentPassword);

            // get the native com object as an IADsUser, and change the 
            // password
            IADsUser user = (IADsUser)directoryEntry.NativeObject;
            user.ChangePassword(_currentPassword, _newPassword);
        }

        /// <summary>
        ///     Authenticates the user
        /// </summary>
        /// <param name="directoryEntry"></param>
        /// <param name="username"></param>
        /// <param name="password"></param>
        internal Uid Authenticate(/*DirectoryEntry directoryEntry,*/ string username,
            Org.IdentityConnectors.Common.Security.GuardedString password, bool returnUidOnly)
        {
            AuthenticationHelper authHelper = new AuthenticationHelper(_configuration);
            if(returnUidOnly)
            {
                return authHelper.GetUidFromSamAccountName(username);
            }
            password.Access(setCurrentPassword);
            return authHelper.ValidateUserCredentials(username, _currentPassword);
        }

    }
}
