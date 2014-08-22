// <copyright file="PowerShellSupport.cs" company="Sun Microsystems, Inc.">
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

namespace Org.IdentityConnectors.ActiveDirectory
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

    public interface IPowerShellSupport {
        ICollection<PSObject> InvokePipeline(Collection<Command> commands);
        ICollection<PSObject> InvokePipeline(Command item);
        ICollection<PSObject> InvokeScript(string script, ICollection<CommandParameter> parameters);
    }

	/// <summary>
	/// <para>
	/// The implementation of the run space. This wraps the real run space object
	/// from powershell for use in the pool
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
	public sealed class PowerShellSupport : IPowerShellSupport, IDisposable {

        public delegate Runspace CreateRunspaceDelegate();

        /// <summary>
        /// How many times to try when runspace state is "Broken".
        /// </summary>
        const int MAX_ATTEMPTS = 3;

		/// <summary>
		/// This class name, used for logging purposes
		/// </summary>
		private static readonly string ClassName = typeof(PowerShellSupport).ToString();
        private static readonly string LocalClassName = typeof(PowerShellSupport).Name;

        static PowerShellSupport() {
            Console.WriteLine("ClassName = " + ClassName);
            Console.WriteLine("LocalClassName = " + LocalClassName);
        }

		/// <summary>
		/// Pool of available runspaces
		/// </summary>
		private MyRunspacePool _runSpacePool;

		/// <summary>
		/// The catalog of localized messages.
		/// </summary>
		private ConnectorMessages _messageCatalog;

        /// <summary>
        /// Local snapin names to load.
        /// </summary>
        private IList<string> _localSnapinNames;

        internal static TraceSource LOGGER = new TraceSource(LocalClassName);
        private static TraceSource LOGGER_COMMANDS = new TraceSource(LocalClassName + ".Commands");
        private static TraceSource LOGGER_PERFORMANCE = new TraceSource(LocalClassName + ".Performance");

        private const int CAT_DEFAULT = 1;      // default tracing event category

        #region Initializating, reinitializating and closing runspace
        /// <summary>
		/// Initializes a new instance of the <see cref="PowerShellSupport" /> class.
		/// </summary>
		/// <param name="localSnapinNames">Local snapins to be loaded</param>
		/// <param name="useRemoteSession">Whether to use remote session (e.g. for managing Exchange)</param>
        /// <param name="createRunspaceMethod">A method used to create a new runspace (if null, a default implementation is used)</param>
		/// <param name="messageCatalog">The message catalog used for conveying localized messages.</param>
		/// <exception cref="ArgumentNullException">Thrown when <paramref name="messageCatalog"/> is null.</exception>
		public PowerShellSupport(IList<string> localSnapinNames, CreateRunspaceDelegate createRunspaceMethod, ConnectorMessages messageCatalog) {
			Assertions.NullCheck(messageCatalog, "messageCatalog");
			_messageCatalog = messageCatalog;
            _localSnapinNames = localSnapinNames;
            _runSpacePool = new MyRunspacePool(createRunspaceMethod ?? DefaultRunspaceCreateMethod);
        }

        public Runspace DefaultRunspaceCreateMethod() {
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Creating runspace configuration");
            RunspaceConfiguration runSpaceConfig = RunspaceConfiguration.Create();
            if (_localSnapinNames != null) {
                foreach (string snapinName in _localSnapinNames) {
                    LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Adding snap-in {0}", snapinName);
                    PSSnapInException snapOutput = null;
                    runSpaceConfig.AddPSSnapIn(snapinName, out snapOutput);
                    if (snapOutput != null) {
                        throw snapOutput;
                    }
                }
            }
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Creating the runspace");
            var runspace = RunspaceFactory.CreateRunspace(runSpaceConfig);
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Runspace created");
            return runspace;
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
        public ICollection<PSObject> InvokePipeline(Collection<Command> commands) {
            Stopwatch watch = new Stopwatch();
            watch.Start();
            Runspace runspace = _runSpacePool.acquireRunspace();
            LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "InvokePipeline: Runspace acquired: {0} ms from start", watch.ElapsedMilliseconds);

            try {
                for (int attempt = 1; ; attempt++) {
                    try {
                        return InvokePipelineInternal(runspace, commands);
                    }
                    catch (InvalidRunspaceStateException e) {
                        ProcessRunspaceException(runspace, attempt, e);
                    }
                }
            }
            finally {
                if (runspace != null) {
                    _runSpacePool.returnRunspace(runspace);
                }
                LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "InvokePipeline: Method finishing: {0} ms from start", watch.ElapsedMilliseconds);
            }
        }

        public void ProcessRunspaceException(Runspace runspace, int attempt, InvalidRunspaceStateException e) {
            RunspaceStateInfo info = runspace.RunspaceStateInfo;
            if (info != null) {
                LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT,
                    "Runspace is in wrong state. Exception: {0}, State: {1}, Reason: {2}, Attempt number: {3}",
                    e, info.State, info.Reason, attempt);
            } else {
                LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT,
                    "Runspace is in wrong state. Exception: {0}, Attempt number: {1}", e, attempt);
            }
            if (attempt == MAX_ATTEMPTS) {
                LOGGER.TraceEvent(TraceEventType.Error, CAT_DEFAULT, "Maximum number of attempts achieved, signalling the exception");
                _runSpacePool.returnFailedRunspace(runspace);
                throw e;
            } else {
                runspace = _runSpacePool.getAnotherRunspace(runspace);
            }
        }

        private ICollection<PSObject> InvokePipelineInternal(Runspace runspace, Collection<Command> commands) {
            const string MethodName = "InvokePipelineInternal";
			Debug.WriteLine(MethodName + ":entry", ClassName);

            Stopwatch watch = new Stopwatch();
            watch.Start();

			if (commands == null || commands.Count == 0) {
				throw new ArgumentException("Commands argument is null or empty");
			}

			// make sure the output is set
			Collection<PSObject> results;

			// create the pipeline
			Pipeline pipe = runspace.CreatePipeline();
            LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, 
                "InvokePipelineInternal: Pipeline created: {0} ms from start of this method", watch.ElapsedMilliseconds);

			// add the commands to the pipeline
			foreach (Command item in commands) {
				LOGGER_COMMANDS.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Item = {0}", item);
				foreach (CommandParameter cp in item.Parameters) {
					LOGGER_COMMANDS.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, " - parameter {0} = {1}", cp.Name, cp.Value);
				}
				pipe.Commands.Add(item);
			}
			LOGGER_COMMANDS.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Executing pipe: {0}", pipe);

			// run the pipeline if we have something to execute
			results = pipe.Invoke();
            LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "InvokePipelineInternal: Pipeline.Invoke returned: {0} ms from start of this method", watch.ElapsedMilliseconds);
            PipelineReader<object> reader = pipe.Error;

			// check for errors
			CheckErrorsFromReader(reader);

			// an empty collection instead of null when we have executed
			if (results == null) {
				Debug.WriteLine("NO result returned");
				results = new Collection<PSObject>();
			}

            LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "InvokePipelineInternal: Method finishing: {0} ms from start of this method", watch.ElapsedMilliseconds);
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
		public ICollection<PSObject> InvokePipeline(Command item) {
			// create a new collection and add the command
			// specifically not a CommandCollection: that will not work here
			Collection<Command> commands = new Collection<Command>();
			commands.Add(item);
			return this.InvokePipeline(commands);
		}

        public ICollection<PSObject> InvokeScript(string script, ICollection<CommandParameter> parameters) {
            Stopwatch watch = new Stopwatch();
            watch.Start();
            Runspace runspace = _runSpacePool.acquireRunspace();
            LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "InvokeScript: Runspace acquired: {0} ms from start", watch.ElapsedMilliseconds);

            try {
                for (int attempt = 1; ; attempt++) {
                    try {
                        return InvokeScriptInternal(runspace, script, parameters);
                    }
                    catch (InvalidRunspaceStateException e) {
                        ProcessRunspaceException(runspace, attempt, e);
                    }
                }
            }
            finally {
                if (runspace != null) {
                    _runSpacePool.returnRunspace(runspace);
                }
                LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, 
                    "InvokeScript: Method finishing: {0} ms from start", watch.ElapsedMilliseconds);
            }
        }

        public ICollection<PSObject> InvokeScriptInternal(Runspace runspace, string script, ICollection<CommandParameter> parameters) {
            const string MethodName = "InvokeScriptInternal";
            Debug.WriteLine(MethodName + ":entry", ClassName);

            if (script == null || script.Equals("")) {
                throw new ArgumentException("Script argument is null or empty");
            }

            Stopwatch watch = new Stopwatch();
            watch.Start();

            // make sure the output is set
            Collection<PSObject> results;

            using (PowerShell powerShellInstance = PowerShell.Create()) {
                LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "InvokeScriptInternal: Powershell instance created: {0} ms from start", watch.ElapsedMilliseconds);
                powerShellInstance.AddScript(script);
                if (parameters != null) {
                    foreach (CommandParameter cp in parameters) {
                        powerShellInstance.AddParameter(cp.Name, cp.Value);
                    }
                }
                
                powerShellInstance.Runspace = runspace;

                // invoke execution on the pipeline (collecting output)
                LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, 
                    "InvokeScriptInternal: Invoke started: {0} ms from start", watch.ElapsedMilliseconds); 
                results = powerShellInstance.Invoke();
                LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, 
                    "InvokeScriptInternal: Invoke returned: {0} ms from start", watch.ElapsedMilliseconds); 

                PSDataCollection<ErrorRecord> errors = powerShellInstance.Streams.Error;
                CheckErrors(errors);

                // an empty collection instead of null when we have executed
                if (results == null) {
                    results = new Collection<PSObject>();
                }

                Debug.WriteLine(MethodName + ":exit", ClassName);
                LOGGER_PERFORMANCE.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, 
                    "InvokeScriptInternal: Method finishing: {0} ms from start", watch.ElapsedMilliseconds);
                return results;
            }
        }

        public delegate void ThrowIcfExceptionDelegate(Exception e, ErrorRecord error, string message);

		/// <summary>
		/// Checks whether errors reader contains some error, if so the errors are concatenated and exception is thrown,
		/// throws <see cref="ConnectorException"/> if the <paramref name="errors"/> parameter is not empty
		/// 
		/// Introduced because of "Problem while PowerShell execution System.NotSupportedException: Specified method 
		/// is not supported. at System.Management.Automation.Internal.PSDataCollectionPipelineReader`2.ReadToEnd()"
		/// (occurring on Exchange 2010/Windows Server 2008 R2)
		/// </summary>
		/// <param name="reader">pipeline reader</param>
        public static void CheckErrorsFromReader(PipelineReader<object> reader) {
            CheckErrorsFromReader(reader, DefaultThrowIcfExceptionImplementation);
        }

        public static void CheckErrorsFromReader(PipelineReader<object> reader, ThrowIcfExceptionDelegate throwIcfExceptionDelegate) {
			IList<ErrorRecord> errors = new List<ErrorRecord>();
			while (true) {
				object error = reader.Read();
				if (error == AutomationNull.Value) {
					break;
				}
				AddError(errors, error);
			}
            CheckErrors(errors, throwIcfExceptionDelegate);
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

        /// <summary>
        /// Checks whether errors List contains some error, if so the errors are concatenated and exception is thrown,
        /// throws <see cref="ConnectorException"/> if the <paramref name="errors"/> parameter is not empty
        /// </summary>
        /// <param name="errors">List of error messages</param>
        private static void CheckErrors(IList<ErrorRecord> errors) {
            CheckErrors(errors, DefaultThrowIcfExceptionImplementation);
        }

        private static void CheckErrors(IList<ErrorRecord> errors, ThrowIcfExceptionDelegate throwIcfExceptionDelegate) {
            if (errors == null) {
                return;
            }

            Exception firstException = null;
            ErrorRecord firstErrorRecord = null;

            StringBuilder builder = new StringBuilder();
            foreach (ErrorRecord error in errors) {
                if (error != null) {
                    Trace.TraceInformation("ErrorRecord:\n - CategoryInfo: {0}\n - FullyQualifiedErrorId: {1}\n" +
                        " - ErrorDetails: {2}\n - Exception: {3}\n - InvocationInfo: {4}\n - PipelineIterationInfo: {5}\n - TargetObject: {6}",
                        error.CategoryInfo, error.FullyQualifiedErrorId, error.ErrorDetails, error.Exception,
                        error.InvocationInfo, CollectionUtil.Dump(error.PipelineIterationInfo), error.TargetObject);

                    var c = error.CategoryInfo;
                    Trace.TraceInformation("CategoryInfo details:\n - Category: {0}\n - Activity: {1}\n" +
                        " - Reason: {2}\n - TargetName: {3}\n - TargetType: {4}", c.Category, c.Activity, c.Reason, c.TargetName, c.TargetType);

                    if (firstException == null && error.Exception != null) {
                        firstException = error.Exception;
                    }
                    if (firstErrorRecord == null) {
                        firstErrorRecord = error;
                    }
                    builder.Append(error.ToString());
                    builder.Append("\n");
                }
            }

            if (firstException != null) {
                throwIcfExceptionDelegate(firstException, firstErrorRecord, builder.ToString());
            } else if (builder.Length > 0) {
                throw new ConnectorException(builder.ToString());
            }
        }

        private static void DefaultThrowIcfExceptionImplementation(Exception e, ErrorRecord error, string message) {
            Trace.TraceInformation("DefaultThrowIcfExceptionImplementation dealing with {0}, message = {1}", e, message);

            // TODO is this really correct? It is not sure that the object in question is really missing (although it's quite probable).
            if (error.CategoryInfo != null && error.CategoryInfo.Reason.Equals("ManagementObjectNotFoundException")) {
                throw new ObjectNotFoundException(message);
            }

            if (e == null) {
                throw new ConnectorException(message);
            }
            if (e is RemoteException) {
                RemoteException remote = (RemoteException)e;
                PSObject serialized = remote.SerializedRemoteException;
                if (serialized != null && serialized.BaseObject is Exception) {
                    DefaultThrowIcfExceptionImplementation((Exception)serialized.BaseObject, error, message);
                } else {
                    throw new ConnectorException("Remote exception: " + message, e);
                }
            }
            string name = e.GetType().Name;
            switch (name) {
                case "DuplicateAcceptedDomainException":
                case "ADObjectAlreadyExistsException":
                    throw new AlreadyExistsException(message, e);
                default:
                    throw new ConnectorException(message, e);
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
        private IDictionary<int, Runspace> _usedRunspaces = new Dictionary<int, Runspace>();
        private Queue<Runspace> _freeRunspaces = new Queue<Runspace>();
        private PowerShellSupport.CreateRunspaceDelegate _createRunspaceDelegate;

        private const int CAT_DEFAULT = 1;      // default tracing event category

        public MyRunspacePool(PowerShellSupport.CreateRunspaceDelegate createRunspaceDelegate)
        {
            _createRunspaceDelegate = createRunspaceDelegate;
        }

        internal Runspace acquireRunspace() {
            int id = Thread.CurrentThread.ManagedThreadId;
            if (_usedRunspaces.ContainsKey(id)) {
                Trace.TraceInformation("Using unreturned runspace for thread {0}", id);
                return _usedRunspaces[id];
            } else {
                Runspace selectedRunspace;
                if (_freeRunspaces.Count > 0) {
                    selectedRunspace = _freeRunspaces.Dequeue();
                } else {
                    Trace.TraceInformation("No runspace available, creating new one.");
                    selectedRunspace = _createRunspaceDelegate();
                    if (selectedRunspace.RunspaceStateInfo.State == RunspaceState.BeforeOpen) {
                        PowerShellSupport.LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Opening runspace.");
                        selectedRunspace.Open();
                        PowerShellSupport.LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Runspace opened.");
                    }
                }
                _usedRunspaces[id] = selectedRunspace;
                return selectedRunspace;
            }
        }

        internal void returnRunspace(Runspace runspace) {
            returnRunspace(runspace, true);
        }

        internal void returnFailedRunspace(Runspace runspace) {
            returnRunspace(runspace, false);
        }

        private void returnRunspace(Runspace runspace, bool enqueue) {      // enqueue=false is used when runspace is broken
            int id = Thread.CurrentThread.ManagedThreadId;
            if (!_usedRunspaces.ContainsKey(id)) {
                throw new InvalidOperationException("Runspace being returned (" + runspace + ") is not registered as borrowed for thread " + id);
            }
            if (_usedRunspaces[id] != runspace) {
                throw new InvalidOperationException("Runspace being returned (" + runspace + ") to the pool does not match the one that was borrowed (" + _usedRunspaces[id] + ")");
            }
            _usedRunspaces.Remove(id);

            if (_freeRunspaces.Contains(runspace)) {
                throw new InvalidOperationException("Runspace being returned (" + runspace + ") is already present in the free runspace pool");
            }
            if (enqueue) {
                _freeRunspaces.Enqueue(runspace);
            }
        }

        internal Runspace getAnotherRunspace(Runspace existingRunspace) {
            returnRunspace(existingRunspace, false);
            existingRunspace.Dispose();
            return acquireRunspace();
        }

        internal void Close() {
            foreach (KeyValuePair<int,Runspace> p in _usedRunspaces) {
                Trace.TraceWarning("Disposing of unreturned runspace (thread {0})", p.Key);
                p.Value.Dispose();
            }
            _usedRunspaces.Clear();
            foreach (Runspace rs in _freeRunspaces) {
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

    public class ObjectNotFoundException : ConnectorException {
        public ObjectNotFoundException() : base() {
        }

        public ObjectNotFoundException(string message) : base(message) {
        }

        public ObjectNotFoundException(Exception ex) : base(ex) {
        }

        public ObjectNotFoundException(string message, Exception ex) : base(message, ex) {
        }

    }
}
