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
