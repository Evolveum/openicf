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
using System.DirectoryServices;
using Org.IdentityConnectors.Common.Security;
using ActiveDs;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using System.DirectoryServices.AccountManagement;
using System.DirectoryServices.ActiveDirectory;

namespace Org.IdentityConnectors.ActiveDirectory
{

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
        internal void Authenticate(DirectoryEntry directoryEntry, string username,
            Org.IdentityConnectors.Common.Security.GuardedString password)
        {
            password.Access(setCurrentPassword);
            String sAMAccountName = (String)directoryEntry.Properties[ActiveDirectoryConnector.ATT_SAMACCOUNT_NAME][0];

            DirectoryEntry userDe = new DirectoryEntry(directoryEntry.Path, 
                sAMAccountName, _currentPassword);
            try
            {
                string serverName = _configuration.LDAPHostName;
                PrincipalContext context = null;
                if ((serverName == null) || (serverName.Length == 0))
                {
                    DomainController domainController = ActiveDirectoryUtils.GetDomainController(_configuration);
                    context = new PrincipalContext(ContextType.Domain, 
                        domainController.Domain.Name);
                }
                else
                {
                    context = new PrincipalContext(ContextType.Machine, _configuration.LDAPHostName);
                }

                if (context == null)
                {
                    throw new ConnectorException("Unable to get PrincipalContext");
                }

                if(!context.ValidateCredentials(sAMAccountName, _currentPassword)) {
                    throw new InvalidCredentialException();
                }
            }
            catch (Exception e)
            {
                throw new InvalidCredentialException();
            }

        }
    }
}
