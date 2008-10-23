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
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.IdentityConnectors.ActiveDirectory
{
    public class ActiveDirectorySyncToken
    {
        internal long LastModifiedUsn { get; set; }
        internal long LastDeleteUsn { get; set; }
        internal bool UseGlobalCatalog { get; set; }
        internal string SyncServer { get; set; }

        public ActiveDirectorySyncToken(SyncToken token, string serverName, bool useGlobalCatalog)
            : this(token == null ? null : (string)token.Value, serverName, useGlobalCatalog)
        {
        }
         
        public ActiveDirectorySyncToken(String tokenValue, string configServerName, bool configUseGlobalCatalog)
        {
            UseGlobalCatalog = configUseGlobalCatalog;
            SyncServer = configServerName;

            if ((tokenValue == null) || (tokenValue.Length == 0))
            {
                LastDeleteUsn = 0;
                LastModifiedUsn = 0;
                return;
            }

            string[] tokenParts = (tokenValue).Split('|');
            if (tokenParts.Length != 4)
            {
                throw new ConnectorException("Unable to parse sync token");
            }

            string tokenSyncServer = tokenParts[3];
            bool tokenUseGlobalCatalog = bool.Parse(tokenParts[2]);

            // If the token server is the same as the configured server,
            // use the token value (usn) to limit the query.  The token is
            // server specific though, so we cant use the usn if it didn't come
            // from this server.
            // If no server is configured, just try to use what we used last time.
            if ((SyncServer != null) && (SyncServer.Equals(configServerName)) &&
                (UseGlobalCatalog.Equals(tokenUseGlobalCatalog)))
            {
                LastModifiedUsn = long.Parse(tokenParts[0]);
                LastDeleteUsn = long.Parse(tokenParts[1]);
            }
            else
            {
                LastModifiedUsn = 0;
                LastDeleteUsn = 0;
            }
        }

        public SyncToken GetSyncToken()
        {
            return new SyncToken(String.Format("{0}|{1}|{2}|{3}",
                LastModifiedUsn, LastDeleteUsn, UseGlobalCatalog, SyncServer));
        }
    }
}
