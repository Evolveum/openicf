// <copyright file="RunSpaceInstance.cs" company="Sun Microsystems, Inc.">
// ====================
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
// 
// The contents of this file are subject to the terms of the Common Development 
// and Distribution License("CDDL") (the "License").  You may not use this file 
// except in compliance with the License.
// 
// You can obtain a copy of the License at 
// http://IdentityConnectors.dev.java.net/legal/license.txt
// See the License for the specific language governing permissions and limitations 
// under the License. 
// 
// When distributing the Covered Code, include this CDDL Header Notice in each file
// and include the License file at identityconnectors/legal/license.txt.
// If applicable, add the following below this CDDL Header, with the fields 
// enclosed by brackets [] replaced by your own identifying information: 
// "Portions Copyrighted [year] [name of copyright owner]"
// ====================
// </copyright>
// <author>Tomas Knappek</author>

namespace Org.IdentityConnectors.Exchange
{
    using System;
    using System.Collections;
    using System.Collections.Generic;
    using System.Collections.ObjectModel;
    using System.Diagnostics;
    using System.Globalization;
    using System.Management.Automation;
    using System.Management.Automation.Runspaces;
    using System.Text;

    using Org.IdentityConnectors.Framework.Common.Exceptions;

    /// <summary>
    /// <para>
    /// The implementation of the run space. This wraps the real run space object 
    /// from powershell for use in the pool
    /// First written for the exchange adapter, the snapin is not needed if you do 
    /// not use it for exchange
    /// </para>
    /// <para>
    /// Two possible ways of executing a command using different access point to
    /// the Runspace:
    /// - RunspaceInvoke: simple commands in string form, the command string can
    ///                   contain multiple commands and is basically the same form
    ///                   as what you use when typing a command in the exchange 
    ///                   shell
    /// - PipelineInvoke: complex (multi) command structured pipelines which also 
    ///                   allow complex parameters, like objects, to be passed in.
    /// </para>
    /// </summary>
    internal sealed class RunSpaceInstance : IDisposable
    {
        /// <summary>
        /// This class name, used for logging purposes
        /// </summary>
        private static readonly string ClassName = typeof(RunSpaceInstance).ToString();

        /// <summary>
        /// The exchange snap in name which needs to be loaded
        /// </summary>
        private const string ExchangeSnapIn = "Microsoft.Exchange.Management.PowerShell.Admin";

        /// <summary>
        /// Instance variable keeping the <see cref="RunspaceConfiguration"/>
        /// </summary>
        private RunspaceConfiguration runSpaceConfig;

        /// <summary>
        /// <see cref="Runspace"/> instance, managed resource
        /// </summary>
        private Runspace runSpace;

        /// <summary>
        /// <see cref="RunspaceInvoke"/> instance, managed resource
        /// </summary>
        private RunspaceInvoke runSpaceInvoke;
        
        /// <summary>
        /// Initializes a new instance of the <see cref="RunSpaceInstance" /> class. 
        /// </summary>
        /// <param name="snapin">Type of snapin to be loaded</param>
        public RunSpaceInstance(SnapIn snapin)
        {
            // initialize this
            this.InitRunSpace(snapin);
        }

        /// <summary>
        /// Snapin type to load
        /// </summary>
        internal enum SnapIn
        {
            /// <summary>
            /// None - not defined
            /// </summary>
            None,

            /// <summary>
            /// MS Exchange snapin
            /// </summary>
            Exchange
        }
        
        /// <summary>
        /// Test the state of this <see cref="RunSpaceInstance"/>, throws <see cref="InvalidRunspaceStateException"/> if in incorrect state
        /// </summary>
        public void Test()
        {
            const string MethodName = "Test";
            Debug.WriteLine(MethodName + ":entry", ClassName);

            // compare the state against the passed in state
            if (this.runSpace != null
                && this.runSpace.RunspaceStateInfo.State == RunspaceState.Opened)
            {
                Debug.WriteLine(MethodName + ":exit", ClassName);
                return;
            }

            throw new InvalidRunspaceStateException("Runspace is not in Opened state");
        }

        /// <summary>invoke the command</summary>
        /// <param name="command">command string to execute</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output/errors from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokeCommand(string command)
        {
            return this.InvokeCommand(command, null);
        }

        /// <summary>
        /// invoke the command
        /// The input is passed in to the environment as the $input variable and
        /// can be used in the script as follows:
        /// invokeCommand("$input | Set-Mailbox", inputEnum)
        /// inputEnum in the example could be the output of an earlier 
        /// invokeCommand call (and thus a complex set of objects)
        /// </summary>
        /// <param name="command">command string to execute</param>
        /// <param name="input">input passed in as $input in the execution
        /// environment</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokeCommand(
            string command,
            IEnumerable input)
        {
            const string MethodName = "InvokeCommand";
            Debug.WriteLine(MethodName + "(" + command + ")" + ":entry", ClassName);
            
            IList errors = null; 
           
            // trim the spaces and check the length
            if (command == null || command.Trim().Length == 0)
            {
                Trace.TraceError("CommandString argument can't be null or empty");
                throw new ArgumentException("CommandString argument can't be null or empty");
            }

            // run the command
            Collection<PSObject> returns =
                   this.runSpaceInvoke.Invoke(command, input, out errors);            

            // check for errors
            CheckErrors(errors);
            
            // an empty collection instead of null when we have executed
            if (returns == null)
            {
                Debug.WriteLine(MethodName + ":exit", ClassName);
                returns = new Collection<PSObject>();
            }

            Trace.WriteLine(String.Format(CultureInfo.CurrentCulture, "{0} results returned", returns.Count), ClassName);
            Debug.WriteLine(MethodName + ":exit", ClassName);
            return returns;
        }

        /// <summary>
        /// invoke the powershell pipeline
        /// </summary>
        /// <param name="commands">a collection of commands to execute</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokePipeline(Collection<Command> commands)
        {
            const string MethodName = "InvokePipeline";            
            Debug.WriteLine(MethodName + ":entry", ClassName);

            IList errors = null;
            
            if (commands == null || commands.Count == 0)
            {                
                throw new ArgumentException("Commands argument is null or empty");
            }

            // make sure the output is set
            errors = null;
            Collection<PSObject> results;

            // create the pipeline
            Pipeline pipe = this.runSpace.CreatePipeline();

            // add the commands to the pipeline            
            foreach (Command item in commands)
            {
                pipe.Commands.Add(item);
            }

            // run the pipeline if we have something to execute
            results = pipe.Invoke();
            PipelineReader<object> reader = pipe.Error;
            errors = (IList)reader.ReadToEnd();

            // check for errors
            CheckErrors(errors);

            // an empty collection instead of null when we have executed
            if (results == null)
            {
                Debug.WriteLine("NO result returned");
                results = new Collection<PSObject>();
            }

            Debug.WriteLine(MethodName + ":exit", ClassName);
            return results;
        }

        /// <summary>
        /// invoke the pipeline
        /// </summary>
        /// <param name="item">a command to execute</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokePipeline(Command item)
        {
            // create a new collection and add the command
            // specifically not a CommandCollection: that will not work here
            Collection<Command> commands = new Collection<Command>();
            commands.Add(item);
            return this.InvokePipeline(commands);
        }

        /// <summary>
        /// Implementation of <see cref="IDisposable"/>
        /// </summary>
        public void Dispose()
        {
            this.Dispose(true);
            GC.SuppressFinalize(this);
        }

        /// <summary>
        /// Checks whether errors List contains some error, if so the errors are concatenated and exception is thrown,
        /// throws <see cref="ConnectorException"/> if the <paramref name="errors"/> parameter is not empty
        /// </summary>
        /// <param name="errors">List of error messages</param>        
        private static void CheckErrors(IList errors)
        {
            StringBuilder builder = new StringBuilder();
            foreach (object error in errors)
            {
                builder.Append(error.ToString());
                builder.Append("\n");
            }

            if (builder.Length > 0)
            {
                throw new ConnectorException("Exception when executing PowerShell: " + builder);
            }
        }

        /// <summary>
        /// Dispose/Finalize pattern
        /// </summary>
        /// <param name="disposing">true if called from <see cref="RunSpaceInstance.Dispose()"/></param>
        private void Dispose(bool disposing)
        {
            if (disposing)
            {
                // Free other state (managed objects).
                // clean up the runspace with attached things:
                // the API docs show that the RunspaceInvoke will call Dispose on
                // the Runspace which in turn calls Close on the Runspace.
                if (this.runSpaceInvoke != null)
                {
                    this.runSpaceInvoke.Dispose();
                }
                else
                {
                    if (this.runSpace != null)
                    {
                        this.runSpace.Dispose();
                    }
                }
            }
        }

        /// <summary>
        /// main initialisation routine for the <see cref="Runspace"/>
        /// </summary>
        /// <param name="snapin"><see cref="SnapIn"/> to be initialized together with the <see cref="Runspace"/></param>
        private void InitRunSpace(SnapIn snapin)
        {
            const string MethodName = "InitRunSpace";
            Debug.WriteLine(MethodName + "(" + snapin + ")" + ":entry", ClassName);

            // create a new config from scratch
            PSSnapInException snapOutput = null;
            this.runSpaceConfig = RunspaceConfiguration.Create();

            switch (snapin)
            {
                case SnapIn.Exchange:
                    // used for force load of the exchange dll's
                    AppDomain.CurrentDomain.AssemblyResolve +=
                                     new ResolveEventHandler(ExchangeUtility.AssemblyResolver);

                    this.runSpaceConfig.AddPSSnapIn(ExchangeSnapIn, out snapOutput);
                    break;
            }

            // check snapOutput
            if (snapOutput != null)
            {
                throw snapOutput;
            }

            // create the real Runspace and open it for processing
            this.runSpace = RunspaceFactory.CreateRunspace(this.runSpaceConfig);
            this.runSpace.Open();
            this.runSpaceInvoke = new RunspaceInvoke(this.runSpace);

            Debug.WriteLine(MethodName + ":exit", ClassName);
        }        
    }
}
