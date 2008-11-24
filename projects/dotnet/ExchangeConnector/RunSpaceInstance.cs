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
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Text;

using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.IdentityConnectors.Exchange
{

    /// <summary>
    /// The implementation of the run space. This wraps the real run space object 
    /// from powershell for use in the pool
    /// First written for the exchange adapter, the snapin is not needed if you do 
    /// not use it for exchange
    /// 
    /// Two possible ways of executing a command using different access point to
    /// the Runspace:
    /// - RunspaceInvoke: simple commands in string form, the command string can
    ///                   contain multiple commands and is basically the same form
    ///                   as what you use when typing a command in the exchange 
    ///                   shell
    /// - PipelineInvoke: complex (multi) command structured pipelines which also 
    ///                   allow complex parameters, like objects, to be passed in.
    /// </summary>
    public sealed class RunSpaceInstance : IDisposable
    {
        private static readonly string CLASS = typeof(RunSpaceInstance).ToString();

        // the exchange snap in which needs to be loaded
        private const string EXCHANGE_SNAPIN = "Microsoft.Exchange.Management.PowerShell.Admin";

        private RunspaceConfiguration _runSpaceConfig = null;
        private Runspace _runSpace = null;
        private RunspaceInvoke _runSpaceInvoke = null;

        //SnapIn type enum
        ///<summary>
        /// Snapin type to load
        ///</summary>
        public enum SnapIn
        {
            ///<summary>
            /// None
            ///</summary>
            None, 
            ///<summary>
            /// MS Exchange snapin
            ///</summary>
            Exchange
        };

        /// <summary>
        ///  RunSpaceInstance Constructor
        /// </summary>
        /// <param name="snapin">Type of snapin to be loaded</param>
        public RunSpaceInstance(SnapIn snapin)
        {
            // initialize this
            InitRunSpace(snapin);
        }

        /// <summary>
        /// Implementation of IDisposable
        /// </summary>
        public void Dispose()
        {
            Dispose(true);            
            GC.SuppressFinalize(this);
        }

        /// <summary>
        /// Dispose/Finalize pattern
        /// </summary>
        /// <param name="disposing"></param>
        private void Dispose(bool disposing)
        {
            if (disposing)
            {
                // Free other state (managed objects).
                // anything we should do?
            }
            // clean up the runspace with attached things:
            // the API docs show that the RunspaceInvoke will call Dispose on
            // the Runspace which in turn calls Close on the Runspace.
            if (_runSpaceInvoke != null)
            {
                _runSpaceInvoke.Dispose();                
            }
            else
            {
                if (_runSpace != null)
                {
                    _runSpace.Dispose();
                }
            }
        }

        /// <summary>
        /// Finalize
        /// </summary>
        ~RunSpaceInstance()
        {
            // Simply call Dispose(false).
            Dispose(false);
        }

        

        /// <summary>
        /// main initialisation routine for the Runspace
        /// </summary>
        private void InitRunSpace(SnapIn snapin)
        {
            string METHOD = "InitRunSpace with snapin " + snapin;
            Debug.WriteLine(METHOD + ":entry", CLASS);


            // create a new config from scratch
            PSSnapInException snapOutput = null;
            _runSpaceConfig = RunspaceConfiguration.Create();

            switch (snapin)
            {
                case SnapIn.Exchange:
                    // used for force load of the exchange dll's
                    AppDomain.CurrentDomain.AssemblyResolve +=
                                     new ResolveEventHandler(ExchangeUtils.AssemblyResolver);

                    PSSnapInInfo info = _runSpaceConfig.AddPSSnapIn(EXCHANGE_SNAPIN,
                                                            out snapOutput);
                    break;
            }

            //check snapOutput
            if (snapOutput != null)
            {
                throw snapOutput;
            }

            // create the real Runspace and open it for processing
            _runSpace = RunspaceFactory.CreateRunspace(_runSpaceConfig);
            _runSpace.Open();
            _runSpaceInvoke = new RunspaceInvoke(_runSpace);

            Debug.WriteLine(METHOD + ":exit", CLASS);
        }


        /// <summary>
        /// Test the state of this RunspaceInstance, throws InvalidRunspaceStateException if in incorrect state
        /// </summary>
        public void Test()
        {
            const string METHOD = "Test";
            Debug.WriteLine(METHOD + ":entry", CLASS);

            // compare the state against the passed in state
            if (_runSpace != null
                && _runSpace.RunspaceStateInfo.State == RunspaceState.Opened)
            {
                Debug.WriteLine(METHOD + ":exit", CLASS);
                return ;
            }

            throw new InvalidRunspaceStateException("Runspace is not in Opened state");
        }

        /// <summary>invoke the command</summary>
        /// <param name="command">command string to execute</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output/errors from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokeCommand(String commandString)
        {
            return InvokeCommand(commandString, null);
        }

        /// <summary>
        /// invoke the command
        /// The input is passed in to the environment as the $input variable and
        /// can be used in the script as follows:
        /// invokeCommand("$input | Set-Mailbox", inputEnum)
        /// inputEnum in the example could be the output of an earlier 
        /// invokeCommand call (and thus a complex set of objects)
        /// </summary>
        /// <param name="commandString">command string to execute</param>
        /// <param name="input">input passed in as $input in the execution
        /// environment</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokeCommand(String commandString,
                                                  IEnumerable input)
        {
            const string METHOD = "InvokeCommand";
            Debug.WriteLine(METHOD + ":entry", CLASS);
            
            IList errors = null;            
            // trim the spaces and check the length
            if (commandString == null || commandString.Trim().Length == 0)
            {
                Trace.TraceError("CommandString argument can't be null or empty");
                throw new ArgumentException("CommandString argument can't be null or empty");
            }

            // run the command
            Collection<PSObject> returns =
                   _runSpaceInvoke.Invoke(commandString, input, out errors);            
            //check for errors
            checkErrors(errors);
            
            // an empty collection instead of null when we have executed
            if (returns == null)
            {
                Debug.WriteLine(METHOD + ":exit", CLASS);
                returns = new Collection<PSObject>();
            } //if returns
            Trace.WriteLine(String.Format("{0} results returned", returns.Count), CLASS);
            Debug.WriteLine(METHOD + ":exit", CLASS);
            return returns;

        }

        /// <summary>
        /// invoke the pipeline
        /// </summary>
        /// <param name="commands">a collection of commands to execute</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokePipeline(Collection<Command> commands)
        {
            const string METHOD = "InvokePipeline";            
            Debug.WriteLine(METHOD + ":entry", CLASS);

            IList errors = null;
            
            if (commands == null || commands.Count == 0)
            {
                Trace.TraceInformation("Commands argument is null or empty", CLASS);
                throw new ArgumentException("Commands argument is null or empty");
            }

            // make sure the output is set
            errors = null;
            Collection<PSObject> results;

            // create the pipeline
            Pipeline pipe = _runSpace.CreatePipeline();
            // add the commands to the pipeline            
            foreach (Command item in commands)
            {
                pipe.Commands.Add(item);
            } // foreach item
            // run the pipeline if we have something to execute
            results = pipe.Invoke();
            PipelineReader<Object> reader = pipe.Error;
            errors = (IList)reader.ReadToEnd();
            //check for errors
            checkErrors(errors);
            // an empty collection instead of null when we have executed
            if (results == null)
            {
                Trace.TraceInformation("NO result returned");
                results = new Collection<PSObject>();
            } //if results                
            Debug.WriteLine(METHOD + ":exit", CLASS);
            return results;

        }

        /// <summary>
        /// invoke the pipeline
        /// </summary>
        /// <param name="commands">a command to execute</param>        
        /// <returns>collection of objects with the result
        /// if no command is passed in return null
        /// if no output from the invoke return an empty collection</returns>
        public ICollection<PSObject> InvokePipeline(Command item)
        {
            // create a new collection and add the command
            // specifically not a CommandCollection: that will not work here
            Collection<Command> commands = new Collection<Command>();
            commands.Add(item);
            return InvokePipeline(commands);
        }
        
        /// <summary>
        /// Checks whether errors List contains some error, if so the errors are concatenated and exception is thrown
        /// </summary>
        /// <param name="errors">List of error messages</param>        
        private void checkErrors(IList errors)
        {            
            StringBuilder builder = new StringBuilder();
            foreach (Object error in errors)
            {
                builder.Append(error.ToString());
                builder.Append("\n");                
            }
            
            if (builder.Length > 0)
            {
                throw new ConnectorException("Exception when executing PowerShell: " + builder.ToString());
            }            
        }

    } // class RunSpaceInstance
}
