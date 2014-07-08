using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Text;

namespace Org.IdentityConnectors.Exchange
{
    class DistributionGroupHandler : SimplePowerShellObjectHandler
    {
        public const string OBJECTCLASS_NAME = "DistributionGroup";

        override internal string GetObjectClassName() {
            return OBJECTCLASS_NAME;
        }

        override internal bool ExecuteSetAfterNew() {
            return true;
        }

    }
}
