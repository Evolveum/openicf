/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Collections.ObjectModel;

namespace Org.ForgeRock.OpenICF.Connectors.MsPowerShell
{
    public class MsPowerShellHost : IDisposable
    {
        private readonly PowerShell _ps;
        private readonly Runspace _space;


        public MsPowerShellHost()
        {
            _ps = PowerShell.Create();
            Init();
        }

        public MsPowerShellHost(string[] modules)
        {
            InitialSessionState initial = InitialSessionState.CreateDefault();
            initial.ImportPSModule(modules);
            _space = RunspaceFactory.CreateRunspace(initial);
            _space.Open();
            _ps = PowerShell.Create();
            _ps.Runspace = _space;
            Init();
        }

        public MsPowerShellHost(RunspacePool rsPool)
        {
            _ps = PowerShell.Create();
            _ps.RunspacePool = rsPool;
            Init();
        }

        public void ValidateScript(String script)
        {
            Collection<PSParseError> errors;
            PSParser.Tokenize(script, out errors);
            if (errors.Count != 0)
            {
                throw new ParseException(errors[0].Message);
            }
        }


        public Collection<Object> ExecuteScript(String script, IDictionary<String, Object> arguments)
        {
            var result = new Collection<object>();
           
            _ps.Commands.Clear();
            foreach (var entry in arguments)
            {
                if (entry.Value != null)
                {
                    _ps.AddCommand("Set-Variable").AddArgument(entry.Key).AddArgument(entry.Value);
                }
            }
            _ps.AddScript(script);

            try
            {
                _ps.Streams.ClearStreams();
                var psResult = _ps.Invoke();

                if ((psResult != null) && (psResult.Count > 0))
                {
                    foreach (var psobj in psResult)
                    {
                        result.Add(psobj.BaseObject);
                    }
                }
                return result;
            }
            finally
            {
                _ps.Stop();
            }
        }

        private void Init()
        {
            _ps.Streams.Verbose.DataAdded += new EventHandler<DataAddedEventArgs>(SendVerbose);
            _ps.Streams.Warning.DataAdded += new EventHandler<DataAddedEventArgs>(SendWarning);
        }

        void SendWarning( object sender, DataAddedEventArgs e)
        {
            if (sender == null) return;
            var rec = sender as PSDataCollection<WarningRecord>;
            Trace.TraceWarning("POWERSHELL: {0}", rec[e.Index]);
        }

        void SendVerbose(object sender, DataAddedEventArgs e)
        {
            if (sender == null) return;
            var rec = sender as PSDataCollection<VerboseRecord>;
            Trace.TraceInformation("POWERSHELL: {0}", rec[e.Index]);
        }

        public void Dispose() 
        {
            if (_ps != null) _ps.Dispose();
            if (_space != null)
            {
                _space.Close();
                _space.Dispose();
            }
        }
    }
}
