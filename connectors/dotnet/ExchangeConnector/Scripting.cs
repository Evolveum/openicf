using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Spi;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Org.IdentityConnectors.Exchange.Data;
using System.Diagnostics;
using System.Collections.ObjectModel;
using System.Management.Automation;

namespace Org.IdentityConnectors.Exchange
{
    public class Context
    {
        public ExchangeConnector Connector { get; set; }
        public string OperationName { get; set; }
        public Scripting.Position Position { get; set; }
        public ObjectClass ObjectClass { get; set; }
    }

    public class CreateUpdateOpContext : Context
    {
        public ICollection<ConnectorAttribute> Attributes { get; set; }
        public OperationOptions Options { get; set; }
        public Uid Uid { get; set; }
    }

    public class CreateOpContext : CreateUpdateOpContext
    {
    }

    public class UpdateOpContext : CreateUpdateOpContext
    {
    }

    public class DeleteOpContext : Context
    {
        public OperationOptions Options { get; set; }
        public Uid Uid { get; set; }
    }

    public class ExecuteQueryContext : Context
    {
        public string Query { get; set; }
        public ResultsHandler ResultsHandler { get; set; }
        public OperationOptions Options { get; set; }
    }

    public class SyncOpContext : Context
    {
        public SyncToken SyncToken { get; set; }
        public SyncResultsHandler SyncResultsHandler { get; set; }
        public OperationOptions Options { get; set; }
    }


    public class Scripting
    {
        private PowerShellSupport _powershell;
        private ScriptingInfo _scriptingInfo;

        public Scripting(string configurationFile, PowerShellSupport powershell)
        {
            if (configurationFile != null)
            {
                _scriptingInfo = PersistenceUtility.ReadScriptingInfo(configurationFile);
                _scriptingInfo.ReadFiles();
            }
            _powershell = powershell;
        }

        public enum Position
        {
            BeforeMain, InsteadOfMain, AfterMain
        }

        internal bool ExecutePowerShell(Context context, Position position)
        {
            if (_scriptingInfo == null)
            {
                return false;
            }

            context.Position = position;
            IList<ScriptToRun> scripts = _scriptingInfo.GetScriptsFor(context.OperationName, context.ObjectClass, context.Position);
            if (scripts != null && scripts.Count() > 0)
            {
                foreach (ScriptToRun scriptInfo in scripts)
                {
                    if (!context.Connector.Configuration.CacheScripts)
                    {
                        scriptInfo.ReadFile();
                    }
                    Trace.TraceInformation("Running script {0}", scriptInfo.File);
                    ICollection<PSObject> result = _powershell.InvokeScript(scriptInfo.Content, context);
                    foreach (PSObject outputItem in result)
                    {
                        if (outputItem != null)
                        {
                            Trace.TraceInformation("Result item: {0}", outputItem);
                        }
                    }

                }
                return true;
            }
            else
            {
                return false;
            }
        }
    }
}
