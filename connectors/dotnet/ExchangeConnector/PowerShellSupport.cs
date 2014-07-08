// <copyright file="RunSpaceInstance.cs" company="Sun Microsystems, Inc.">
// ====================
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
// <author>Pavol Mederly</author>

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
	using System.Management.Automation.Internal;
	using System.Text;

	using Org.IdentityConnectors.Framework.Common.Exceptions;
	using Org.IdentityConnectors.Framework.Common.Objects;
	using Org.IdentityConnectors.Common;
    using System.Threading;

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
	public sealed class PowerShellSupport : IDisposable
	{
        /// <summary>
        /// How many times to try when runspace state is "Broken".
        /// </summary>
        const int MAX_ATTEMPTS = 3;

		/// <summary>
		/// This class name, used for logging purposes
		/// </summary>
		private static readonly string ClassName = typeof(PowerShellSupport).ToString();

		/// <summary>
		/// The Exchange 2007 snap in name which needs to be loaded
		/// </summary>
		private const string Exchange2007SnapIn = "Microsoft.Exchange.Management.PowerShell.Admin";

		/// <summary>
		/// The Exchange 2010 snap in name which needs to be loaded
		/// </summary>
		private const string Exchange2010SnapIn = "Microsoft.Exchange.Management.PowerShell.E2010";

   		/// <summary>
		/// The Exchange 2013 snap in name which needs to be loaded
		/// </summary>
		private const string Exchange2013SnapIn = "Microsoft.Exchange.Management.PowerShell.SnapIn";

		/// <summary>
		/// PowerShell schema used to remotely manage Exchange.
		/// </summary>
		private const string ExchangePowerShellSchema = "http://schemas.microsoft.com/powershell/Microsoft.Exchange";
		
		/// <summary>
		/// Pool of available runspaces
		/// </summary>
		private MyRunspacePool _runSpacePool;

		/// <summary>
		/// The catalog of localized messages.
		/// </summary>
		private ConnectorMessages _messageCatalog;

        /// <summary>
        /// Switch controlling tracing of PowerShell commands.
        /// </summary>
        private TraceSwitch _performanceSwitch = new System.Diagnostics.TraceSwitch("PowerShellPerformance", "Information about performance of PowerShell commands.");
        private TraceSwitch _powerShellCommandsSwitch = new System.Diagnostics.TraceSwitch("PowerShellCommands", "Information about execution of PowerShell commands.");
        private TraceSwitch _powerShellGeneralSwitch = new System.Diagnostics.TraceSwitch("PowerShellGeneral", "Information about general aspects of PowerShell interaction.");

        private ExchangeVersion _exchangeVersion;
        private SnapIn _snapIn;
        private string _exchangeUri;

        #region Initializating, reinitializating and closing runspace
        /// <summary>
		/// Initializes a new instance of the <see cref="PowerShellSupport" /> class.
		/// </summary>
		/// <param name="snapin">Type of snapin to be loaded</param>
		/// <param name="exchangeUri">URI of the server that executes PowerShell cmdlets for remote Exchange 2010</param>
		/// <param name="messageCatalog">The message catalog used for conveying localized messages.</param>
		/// <exception cref="ArgumentNullException">Thrown when <paramref name="messageCatalog"/> is null.</exception>
		internal PowerShellSupport(SnapIn snapin, string exchangeUri, string configuredExchangeVersion, ConnectorMessages messageCatalog)
		{
			Assertions.NullCheck( messageCatalog, "messageCatalog" );
			_messageCatalog = messageCatalog;

            _snapIn = snapin;

            if (configuredExchangeVersion == null && exchangeUri != null)
            {
                Trace.TraceWarning("No configured Exchange version; and auto-detection is not possible in remote mode. Using 2010 as a default.");
                _exchangeVersion = ExchangeVersion.E2010;
            }
            else
            {
                _exchangeVersion = GetExchangeServerVersion(configuredExchangeVersion);
            }
            if (_exchangeVersion == ExchangeVersion.E2007 && _exchangeUri == null)
            {
                // used for force load of the exchange dll's
                AppDomain.CurrentDomain.AssemblyResolve +=
                    new ResolveEventHandler(ExchangeUtility.AssemblyResolver2007);
            }
            _exchangeUri = exchangeUri;
            _runSpacePool = new MyRunspacePool(CreateRunspace);
        }

        Runspace CreateRunspace()
        {
            RunSpaceAsyncInitializer initializer = new RunSpaceAsyncInitializer(this);
            initializer.InitializeInOtherThread();
            return initializer.RunSpace;
		}

        private class RunSpaceAsyncInitializer
        {
            private PowerShellSupport _powerShellSupportInstance;
            public Runspace RunSpace { get; set; }

            internal RunSpaceAsyncInitializer(PowerShellSupport powerShellSupportInstance)
            {
                _powerShellSupportInstance = powerShellSupportInstance;
            }

            public void Initialize()
            {
                try
                {
                    RunSpace = _powerShellSupportInstance.InitRunSpace();
                }
                catch (Exception e)
                {
                    Trace.TraceError("Error while initializing runspace: {0}", e);
                    throw;
                }
            }

            public void InitializeInOtherThread()
            {
                RunSpace = null;

                Thread oThread = new Thread(new ThreadStart(Initialize));
                oThread.Start();
                Trace.TraceInformation("Waiting for run space initialization to start (in a separate thread)...");
                while (!oThread.IsAlive) ;              // wait for thread to become alive
                Trace.TraceInformation("Waiting for run space initialization to finish (in a separate thread)...");
                oThread.Join();                         // wait for thread to finish
                Trace.TraceInformation("Run space initialization finished.");
            }
        };

        /// <summary>
        /// main initialisation routine for the <see cref="Runspace"/>
        /// </summary>
        /// <param name="snapin"><see cref="SnapIn"/> to be initialized together with the <see cref="Runspace"/></param>
        private Runspace InitRunSpace()
        {
            const string MethodName = "InitRunSpace";
            Debug.WriteLine(MethodName + ":entry", ClassName);

            Runspace runspace;

            switch (_snapIn)
            {
                case SnapIn.Exchange:
                    if (_exchangeUri != null)
                    {
                        // TODO is remote management of E2007 supported?
                        WSManConnectionInfo ci = new WSManConnectionInfo(new Uri(_exchangeUri), ExchangePowerShellSchema, (PSCredential) null);
                        ci.AuthenticationMechanism = AuthenticationMechanism.Kerberos;
                        Trace.WriteLineIf(_powerShellGeneralSwitch.TraceInfo, "Creating PowerShell connection to " + _exchangeUri);
                        runspace = RunspaceFactory.CreateRunspace(ci);
                        Trace.WriteLineIf(_powerShellGeneralSwitch.TraceInfo, "Connection created.");

                    } else {
                    
                        string snapinName;

                        switch (_exchangeVersion)
                        {
                            case ExchangeVersion.E2007:
                                snapinName = Exchange2007SnapIn;
                                break;
                            case ExchangeVersion.E2010:
                                snapinName = Exchange2010SnapIn;
                                break;
                            case ExchangeVersion.E2013:
                                snapinName = Exchange2013SnapIn;
                                break;
                            default: throw new ArgumentException("Invalid server version: " + _exchangeVersion);
                        }

                        // create a new config from scratch
                        RunspaceConfiguration runSpaceConfig = RunspaceConfiguration.Create();
                        PSSnapInException snapOutput = null;
                        runSpaceConfig.AddPSSnapIn(snapinName, out snapOutput);
                        if (snapOutput != null)
                        {
                            throw snapOutput;
                        }
                        runspace = RunspaceFactory.CreateRunspace(runSpaceConfig);
                    }
                    break;

                case SnapIn.None:
                default:
                    runspace = RunspaceFactory.CreateRunspace();
                    break;
            }

            // check snapOutput

            // create the real Runspace and open it for processing

            Trace.WriteLineIf(_powerShellGeneralSwitch.TraceInfo, "Opening runspace.");
            runspace.Open();
            Trace.WriteLineIf(_powerShellGeneralSwitch.TraceInfo, "Runspace opened.");

            Debug.WriteLine(MethodName + ":exit", ClassName);
            return runspace;
        }

        /// <summary>
        /// Determines the version of the Exchange server.
        /// </summary>
        /// <remarks>As the remote management functionality is not utilized, the Exchange powershell snap-in must be registered
        /// on the local computer. Different snap-in is used to manage Exchange 2007 and 2010, hence the server version is determined by the
        /// registered snap-in.
        /// </remarks>
        /// <returns>The version of the Exchange server to manage.</returns>
        /// <exception cref="ConnectorException">Thrown when the version cannot be determined.</exception>
        private ExchangeVersion GetExchangeServerVersion(string configuredExchangeVersion)
        {
            const string MethodName = "GetServerVersion";
            Debug.WriteLine(MethodName + ":entry", ClassName);

            if (configuredExchangeVersion != null)
            {
                Trace.WriteLineIf(_powerShellGeneralSwitch.TraceInfo, "Using configured Exchange version: {0}", configuredExchangeVersion);
                switch (configuredExchangeVersion)
                {
                    case "2007": return ExchangeVersion.E2007;
                    case "2010": return ExchangeVersion.E2010;
                    case "2013": return ExchangeVersion.E2013;
                    default: throw new ArgumentException("Invalid or unsupported Exchange version: " + configuredExchangeVersion + " (supported ones are: 2007, 2010, 2013)");
                }
            }

            Trace.WriteLineIf(_powerShellGeneralSwitch.TraceInfo, "Trying to determine Exchange version from registered PowerShell snapins");

            const string ExchangeSnapinNamePrefix = "Microsoft.Exchange.Management.PowerShell.";

            ExchangeVersion? version = null;
            using (var runspace = RunspaceFactory.CreateRunspace())
            {
                runspace.Open();

                using (var pipeline = runspace.CreatePipeline())
                {
                    var getSnapinsCommand = new Command("Get-PSSnapin");
                    getSnapinsCommand.Parameters.Add("Registered");

                    pipeline.Commands.Add(getSnapinsCommand);

                    var snapinList = pipeline.Invoke();

                    PipelineReader<object> reader = pipeline.Error;
                    CheckErrorsFromReader(reader);

                    runspace.Close();

                    if ((snapinList == null) || (snapinList.Count == 0))
                    {
                        Trace.TraceError("No snap-in returned");
                        throw new ConnectorException(_messageCatalog.Format("ex_NoPowerShellSnapins", "There are no registered PowerShell snap-ins."));
                    }

                    foreach (var snapin in snapinList)
                    {
                        if (snapin.Properties["Name"] != null && snapin.Properties["Name"].Value != null)
                        {
                            var name = snapin.Properties["Name"].Value.ToString();

                            Trace.WriteLineIf(_powerShellGeneralSwitch.TraceVerbose, "Found registered snap-in: {0}", name);

                            if (name.StartsWith(ExchangeSnapinNamePrefix, StringComparison.InvariantCultureIgnoreCase))
                            {
                                switch (name.Substring(ExchangeSnapinNamePrefix.Length))
                                {
                                    case "Admin":
                                        //Microsoft.Exchange.Management.PowerShell.Admin snap-in is used to manage Exchange 2007
                                        version = ExchangeVersion.E2007;
                                        break;
                                    case "SnapIn":      // TODO test if this works
                                        version = ExchangeVersion.E2013;
                                        break;
                                    case "E2010":
                                        //Microsoft.Exchange.Management.PowerShell.E2010 snap-in is used to manage Exchange 2010
                                        version = ExchangeVersion.E2010;
                                        break;
                                }
                            }
                        }
                    }
                }
            }

            if (!version.HasValue)
            {
                throw new ConnectorException(_messageCatalog.Format("ex_NoSupportedExchangeSnapin",
                                                                    "There is no supported Exchange PowerShell snap-in registered."));
            }

            if (_powerShellGeneralSwitch.TraceInfo)
                Trace.TraceInformation("Exchange version determined to be {0}", version.ToString());
            Debug.WriteLine(MethodName + ":exit", ClassName);
            return version.Value;
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
        /// Dispose/Finalize pattern
        /// </summary>
        /// <param name="disposing">true if called from <see cref="PowerShellSupport.Dispose()"/></param>
        private void Dispose(bool disposing)
        {
            if (disposing)
            {
                // Free other state (managed objects).
                // clean up the runspace with attached things:
                // the API docs show that the RunspaceInvoke will call Dispose on
                // the Runspace which in turn calls Close on the Runspace.
                _runSpacePool.Close();
            }
            _messageCatalog = null;
        }

        #endregion


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
		/// Defines the various supported Exchange versions.
		/// </summary>
		private enum ExchangeVersion
		{
			E2007,
			E2010,
            E2013
		}
		
		/// <summary>
		/// Test the state of this <see cref="PowerShellSupport"/>, throws <see cref="InvalidRunspaceStateException"/> if in incorrect state
		/// </summary>
		public void Test()
		{
			const string MethodName = "Test";
			Debug.WriteLine(MethodName + ":entry", ClassName);
            // currently nothing to do; there are many runspaces, some of which may be in Broken state (will be corrected when they are to be really used)
            Debug.WriteLine(MethodName + ":exit", ClassName);
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
            Stopwatch watch = new Stopwatch();
            watch.Start();
            Runspace runspace = _runSpacePool.acquireRunspace();
            Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokePipeline: Runspace acquired: {0} ms from start", watch.ElapsedMilliseconds));

            try
            {
                for (int attempt = 1; ; attempt++)
                {
                    try
                    {
                        return InvokePipelineInternal(runspace, commands);
                    }
                    catch (InvalidRunspaceStateException e)
                    {
                        ProcessRunspaceException(runspace, attempt, e);
                    }
                }
            }
            finally
            {
                if (runspace != null)
                {
                    _runSpacePool.returnRunspace(runspace);
                }
                Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokePipeline: Method finishing: {0} ms from start", watch.ElapsedMilliseconds));
            }
        }

        public void ProcessRunspaceException(Runspace runspace, int attempt, InvalidRunspaceStateException e)
        {
            RunspaceStateInfo info = runspace.RunspaceStateInfo;
            if (info != null)
            {
                Trace.TraceWarning("Runspace is in wrong state. Exception: {0}, State: {1}, Reason: {2}, Attempt number: {3}",
                    e, info.State, info.Reason, attempt);
            }
            else
            {
                Trace.TraceWarning("Runspace is in wrong state. Exception: {0}, Attempt number: {1}", e, attempt);
            }
            if (attempt == MAX_ATTEMPTS)
            {
                Trace.TraceError("Maximum number of attempts achieved, signalling the exception");
                _runSpacePool.returnFailedRunspace(runspace);
                throw e;
            }
            else
            {
                runspace = _runSpacePool.getAnotherRunspace(runspace);
            }
        }

        public ICollection<PSObject> InvokePipelineInternal(Runspace runspace, Collection<Command> commands)
		{
            const string MethodName = "InvokePipelineInternal";
			Debug.WriteLine(MethodName + ":entry", ClassName);

            Stopwatch watch = new Stopwatch();
            watch.Start();

			if (commands == null || commands.Count == 0)
			{
				throw new ArgumentException("Commands argument is null or empty");
			}

			// make sure the output is set
			Collection<PSObject> results;

			// create the pipeline
			Pipeline pipe = runspace.CreatePipeline();
            Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokePipelineInternal: Pipeline acquired: {0} ms from start", watch.ElapsedMilliseconds));

			// add the commands to the pipeline
			foreach (Command item in commands)
			{
				Trace.WriteLine("Item = " + item.ToString());
				foreach (CommandParameter cp in item.Parameters) {
					Trace.WriteLine(" - parameter " + cp.Name + " = " + cp.Value);
				}
				pipe.Commands.Add(item);
			}
			Trace.WriteLine("Executing pipe: " + pipe.ToString());

			// run the pipeline if we have something to execute
			results = pipe.Invoke();
            Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokePipelineInternal: Pipeline.Invoke returned: {0} ms from start", watch.ElapsedMilliseconds));
            PipelineReader<object> reader = pipe.Error;

			// check for errors
			CheckErrorsFromReader(reader);

			// an empty collection instead of null when we have executed
			if (results == null)
			{
				Debug.WriteLine("NO result returned");
				results = new Collection<PSObject>();
			}

            Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokePipelineInternal: Method finishing: {0} ms from start", watch.ElapsedMilliseconds));
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

        public ICollection<PSObject> InvokeScript(string script, Context context)           // TODO generalize parameters
        {
            Stopwatch watch = new Stopwatch();
            watch.Start();
            Runspace runspace = _runSpacePool.acquireRunspace();
            Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokeScript: Runspace acquired: {0} ms from start", watch.ElapsedMilliseconds));

            try
            {
                for (int attempt = 1; ; attempt++)
                {
                    try
                    {
                        return InvokeScriptInternal(runspace, script, context);
                    }
                    catch (InvalidRunspaceStateException e)
                    {
                        ProcessRunspaceException(runspace, attempt, e);
                    }
                }
            }
            finally
            {
                if (runspace != null)
                {
                    _runSpacePool.returnRunspace(runspace);
                }
                Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokeScript: Method finishing: {0} ms from start", watch.ElapsedMilliseconds));
            }
        }

        public ICollection<PSObject> InvokeScriptInternal(Runspace runspace, string script, Context context)
        {
            const string MethodName = "InvokeScriptInternal";
            Debug.WriteLine(MethodName + ":entry", ClassName);

            if (script == null || script.Equals(""))
            {
                throw new ArgumentException("Script argument is null or empty");
            }

            Stopwatch watch = new Stopwatch();
            watch.Start();

            // make sure the output is set
            Collection<PSObject> results;

            using (PowerShell powerShellInstance = PowerShell.Create())
            {
                Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokeScriptInternal: Powershell instance created: {0} ms from start", watch.ElapsedMilliseconds));
                powerShellInstance.AddScript(script);

                powerShellInstance.AddParameter("ctx", context);
                powerShellInstance.Runspace = runspace;

                // invoke execution on the pipeline (collecting output)
                Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokeScriptInternal: Invoke started: {0} ms from start", watch.ElapsedMilliseconds)); 
                results = powerShellInstance.Invoke();
                Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokeScriptInternal: Invoke returned: {0} ms from start", watch.ElapsedMilliseconds)); 

                PSDataCollection<ErrorRecord> errors = powerShellInstance.Streams.Error;
                CheckErrors(errors);

                // an empty collection instead of null when we have executed
                if (results == null)
                {
                    results = new Collection<PSObject>();
                }

                Debug.WriteLine(MethodName + ":exit", ClassName);
                Trace.WriteLineIf(_performanceSwitch.TraceVerbose, String.Format("InvokeScriptInternal: Method finishing: {0} ms from start", watch.ElapsedMilliseconds));
                return results;
            }
        }

		/// <summary>
		/// Checks whether errors List contains some error, if so the errors are concatenated and exception is thrown,
		/// throws <see cref="ConnectorException"/> if the <paramref name="errors"/> parameter is not empty
		/// </summary>
		/// <param name="errors">List of error messages</param>
		private static void CheckErrors(IList<ErrorRecord> errors)
		{
            if (errors == null)
            {
                return;
            }

            Exception firstException = null;

			StringBuilder builder = new StringBuilder();
			foreach (ErrorRecord error in errors) {
                if (error != null) {
                    if (firstException == null && error.Exception != null) {
                        firstException = error.Exception;
                    }
                    builder.Append(error.ToString());
                    builder.Append("\n");
                }
			}

            if (firstException != null) {
                throwIcfException(firstException, builder.ToString());
            } else if (builder.Length > 0) {
				throw new ConnectorException(builder.ToString());
			}
		}

        private static void throwIcfException(Exception e, string message) {
            if (e == null) {
                throw new ConnectorException(message);
            }
            if (e is RemoteException) {
                RemoteException remote = (RemoteException)e;
                PSObject serialized = remote.SerializedRemoteException;
                if (serialized != null && serialized.BaseObject is Exception) {
                    throwIcfException((Exception)serialized.BaseObject, message);
                } else {
                    throw new ConnectorException("Remote exception: " + message, e);
                }
            }
            Trace.TraceInformation("throwIcfException dealing with {0}, message = {1}", e, message);
            string name = e.GetType().Name;
            switch (name) {
                case "DuplicateAcceptedDomainException":
                case "ADObjectAlreadyExistsException":
                    throw new AlreadyExistsException(message, e);
                default:
                    throw new ConnectorException(message, e);
            }
        }

		/// <summary>
		/// Checks whether errors reader contains some error, if so the errors are concatenated and exception is thrown,
		/// throws <see cref="ConnectorException"/> if the <paramref name="errors"/> parameter is not empty
		/// 
		/// Introduced because of "Problem while PowerShell execution System.NotSupportedException: Specified method 
		/// is not supported. at System.Management.Automation.Internal.PSDataCollectionPipelineReader`2.ReadToEnd()"
		/// (occurring on Exchange 2010/Windows Server 2008 R2)
		/// </summary>
		/// <param name="reader">pipeline reader</param>
		private static void CheckErrorsFromReader(PipelineReader<object> reader)
		{
			IList<ErrorRecord> errors = new List<ErrorRecord>();
			while (true) {
				object error = reader.Read();
				if (error == AutomationNull.Value) {
					break;
				}
				AddError(errors, error);
			}
            CheckErrors(errors);
		}
		
		private static void AddError(IList<ErrorRecord> errors, object error) {
			if (error == null) {
				return;
			} else if (error is IList) {
				foreach (object err in (IList) error) {
					AddError(errors, err);
				}
			} else if (error is PSObject) {
                AddError(errors, ((PSObject)error).BaseObject);
			} else if (error is ErrorRecord) {
				errors.Add((ErrorRecord) error);
			} else {
				throw new ConnectorException("Unexpected kind of error: " + error);
			}
		}
	}

    /*
     * 
    * RunSpace management: 
    * 
    * RunSpaces are not to be shared between threads. So each thread has to get its own one.
    * However, we want to reuse runspaces after they are no longer in use in any particular
    * thread (to minimize the number of runspace initializations, as they can be quite costly).
    * 
    */

    internal class MyRunspacePool
    {
        public delegate Runspace CreateRunspaceDelegate();

        private IDictionary<int, Runspace> _usedRunspaces = new Dictionary<int, Runspace>();
        private Queue<Runspace> _freeRunspaces = new Queue<Runspace>();
        private CreateRunspaceDelegate _createRunspaceDelegate;

        public MyRunspacePool(CreateRunspaceDelegate createRunspaceDelegate)
        {
            _createRunspaceDelegate = createRunspaceDelegate;
        }

        internal Runspace acquireRunspace()
        {
            int id = Thread.CurrentThread.ManagedThreadId;
            if (_usedRunspaces.ContainsKey(id))
            {
                Trace.TraceInformation("Using unreturned runspace for thread {0}", id);
                return _usedRunspaces[id];
            }
            else
            {
                Runspace selectedRunspace;
                if (_freeRunspaces.Count > 0)
                {
                    selectedRunspace = _freeRunspaces.Dequeue();
                }
                else
                {
                    Trace.TraceInformation("No runspace available, creating new one.");
                    selectedRunspace = _createRunspaceDelegate();
                }
                _usedRunspaces[id] = selectedRunspace;
                return selectedRunspace;
            }
        }

        internal void returnRunspace(Runspace runspace)
        {
            returnRunspace(runspace, true);
        }

        internal void returnFailedRunspace(Runspace runspace)
        {
            returnRunspace(runspace, false);
        }

        private void returnRunspace(Runspace runspace, bool enqueue)       // enqueue=false is used when runspace is broken
        {
            int id = Thread.CurrentThread.ManagedThreadId;
            if (!_usedRunspaces.ContainsKey(id))
            {
                throw new InvalidOperationException("Runspace being returned (" + runspace + ") is not registered as borrowed for thread " + id);
            }
            if (_usedRunspaces[id] != runspace)
            {
                throw new InvalidOperationException("Runspace being returned (" + runspace + ") to the pool does not match the one that was borrowed (" + _usedRunspaces[id] + ")");
            }
            _usedRunspaces.Remove(id);

            if (_freeRunspaces.Contains(runspace))
            {
                throw new InvalidOperationException("Runspace being returned (" + runspace + ") is already present in the free runspace pool");
            }
            if (enqueue)
            {
                _freeRunspaces.Enqueue(runspace);
            }
        }

        internal Runspace getAnotherRunspace(Runspace existingRunspace)
        {
            returnRunspace(existingRunspace, false);
            existingRunspace.Dispose();
            return acquireRunspace();
        }

        internal void Close()
        {
            foreach (KeyValuePair<int,Runspace> p in _usedRunspaces)
            {
                Trace.TraceWarning("Disposing of unreturned runspace (thread {0})", p.Key);
                p.Value.Dispose();
            }
            _usedRunspaces.Clear();
            foreach (Runspace rs in _freeRunspaces)
            {
                rs.Dispose();
            }
            _freeRunspaces.Clear();
        }
    }

    /*
    // worse implementation
    internal class MyRunspacePool2
    {
        private delegate Runspace CreateRunspaceDelegate();

        private IDictionary<Runspace, int?> _borrows = new Dictionary<Runspace, int?>();
        private CreateRunspaceDelegate _createRunspaceDelegate;

        MyRunspacePool2(CreateRunspaceDelegate createRunspaceDelegate)
        {
            _createRunspaceDelegate = createRunspaceDelegate;
        }

        internal Runspace acquireRunspace()
        {
            int id = Thread.CurrentThread.ManagedThreadId;
            Runspace firstFree = null;
            foreach (KeyValuePair<Runspace, int?> entry in _borrows)
            {
                if (entry.Value.HasValue)
                {
                    if (entry.Value == id)
                    {
                        Trace.TraceInformation("Using unreturned runspace for thread {0}", id);
                        return entry.Key;
                    }
                }
                else
                {
                    if (firstFree == null)
                    {
                        firstFree = entry.Key;
                    }
                }
            }

            Runspace selectedRunspace;
            if (firstFree != null)
            {
                selectedRunspace = firstFree;
            }
            else
            {
                Trace.TraceInformation("No runspace available, creating new one.");
                selectedRunspace = _createRunspaceDelegate();
            }
            _borrows[selectedRunspace] = id;
            return selectedRunspace;
        }

        internal void returnRunspace(Runspace runspace)
        {
            int id = Thread.CurrentThread.ManagedThreadId;
            if (_borrows[runspace] != id)
            {
                throw new InvalidOperationException("Runspace being returned (" + runspace + ") does not belong to this thread, but to " + _borrows[runspace]);
            }
            _borrows[runspace] = null;
        }
    }
    */

}
