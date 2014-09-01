using Microsoft.Win32;
using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Common.Objects;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Reflection;
using System.Text;
using System.Threading;

namespace Org.IdentityConnectors.Exchange {

    /// <summary>
    /// Defines the various supported Exchange versions.
    /// </summary>
    internal enum ExchangeVersion {
        E2007,
        E2010,
        E2013
    }

    class ExchangePowerShellSupport : IPowerShellSupport, IDisposable {

        PowerShellSupport _powerShellSupport;

        /// <summary>
        /// This class name, used for logging purposes
        /// </summary>
        private static readonly string ClassName = typeof(ExchangePowerShellSupport).ToString();
        private static readonly string LocalClassName = typeof(ExchangePowerShellSupport).Name;

        private static TraceSource LOGGER = new TraceSource(LocalClassName);

        private const int CAT_DEFAULT = 1;      // default tracing event category

        ExchangeVersion _exchangeVersion;
        string _exchangeUri;
        ConnectorMessages _messageCatalog;

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

        internal ExchangePowerShellSupport(string configuredExchangeVersion, string exchangeUri, ConnectorMessages messageCatalog) {
            
            if (configuredExchangeVersion == null && exchangeUri != null) {
                Trace.TraceWarning("No configured Exchange version. As auto-detection is not possible in remote mode, using 2010 as a default.");
                _exchangeVersion = ExchangeVersion.E2010;
            } else {
                _exchangeVersion = GetExchangeServerVersion(configuredExchangeVersion);
            }
            IList<string> snapins = new List<string>();
            if (exchangeUri == null) {
                switch(_exchangeVersion) {
                    case ExchangeVersion.E2007:
                        // used for force load of the exchange dll's (untested in current version of the connector!)
                        AppDomain.CurrentDomain.AssemblyResolve += new ResolveEventHandler(AssemblyResolver2007);
                        snapins.Add(Exchange2007SnapIn);
                        break;
                    case ExchangeVersion.E2010:
                        snapins.Add(Exchange2010SnapIn);
                        break;
                    case ExchangeVersion.E2013:
                        snapins.Add(Exchange2013SnapIn);
                        break;
                    default: throw new ArgumentException("Invalid server version: " + _exchangeVersion);
                }
            }
            _exchangeUri = exchangeUri;
            _messageCatalog = messageCatalog;
            _powerShellSupport = new PowerShellSupport(snapins, CreateExchangeRunspace, messageCatalog);
        }

        // Used to initialize an individual runspace. It is called from runspace pool manager when necessary.
        // Because of some mysterious stack overflow problems we carry out the initialization in a separate thread
        // (TODO: check if still necessary)
        Runspace CreateExchangeRunspace() {
            RunSpaceAsyncInitializer initializer = new RunSpaceAsyncInitializer(this);
            
            //initializer.InitializeInOtherThread();
            initializer.InitializeInCurrentThread();

            return initializer.RunSpace;
        }

        private class RunSpaceAsyncInitializer {
            private ExchangePowerShellSupport _outer;
            public Runspace RunSpace { get; set; }

            internal RunSpaceAsyncInitializer(ExchangePowerShellSupport outer) {
                _outer = outer;
            }

            public void InitializeInSeparateThread() {
                RunSpace = null;

                Thread oThread = new Thread(new ThreadStart(InitializeInCurrentThread));
                oThread.Start();
                ExchangePowerShellSupport.LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Waiting for run space initialization to start (in a separate thread)...");
                while (!oThread.IsAlive) ;              // wait for thread to become alive
                ExchangePowerShellSupport.LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Waiting for run space initialization to finish (in a separate thread)...");
                oThread.Join();                         // wait for thread to finish
                ExchangePowerShellSupport.LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Run space initialization finished.");
            }

            public void InitializeInCurrentThread() {
                try {
                    RunSpace = _outer.InitRunSpace();
                } catch (Exception e) {
                    Console.WriteLine("Error in InitRunSpace(): " + e);
                    Trace.TraceError("Error while initializing runspace: {0}", e);
                    throw;          // if in separate thread, this causes server to crash
                }
            }
        };

        private Runspace InitRunSpace() {
            const string MethodName = "InitRunSpace";
            Debug.WriteLine(MethodName + ":entry", ClassName);

            Runspace runspace = _powerShellSupport.DefaultRunspaceCreateMethod();

            if (_exchangeUri != null) {
                // TODO is remote management of E2007 supported?
                //WSManConnectionInfo ci = new WSManConnectionInfo(new Uri(_exchangeUri), ExchangePowerShellSchema, (PSCredential)null);
                //ci.AuthenticationMechanism = AuthenticationMechanism.Kerberos;
                //Trace.WriteLineIf(_powerShellSupport._powerShellGeneralSwitch.TraceInfo, "Creating PowerShell connection to " + _exchangeUri);
                //runspace = RunspaceFactory.CreateRunspace(ci);
                //Trace.WriteLineIf(_powerShellSupport._powerShellGeneralSwitch.TraceInfo, "Connection created.");

                runspace.Open();
                var initScript = CommonUtils.ReadResource("Org.IdentityConnectors.Exchange.RemoteInitScript.ps1", Assembly.GetExecutingAssembly());
                var parameters = new List<CommandParameter>();
                parameters.Add(new CommandParameter("exchangeUri", _exchangeUri));

                _powerShellSupport.InvokeScriptInternal(runspace, initScript, parameters);
            }

            Debug.WriteLine(MethodName + ":exit", ClassName);
            return runspace;
        }

        public ICollection<PSObject> InvokePipeline(Collection<Command> commands) {
            return _powerShellSupport.InvokePipeline(commands);
        }

        public ICollection<PSObject> InvokePipeline(Command item) {
            return _powerShellSupport.InvokePipeline(item);
        }

        public ICollection<PSObject> InvokeScript(string script, ICollection<CommandParameter> parameters) {
            return _powerShellSupport.InvokeScript(script, parameters);
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
        private ExchangeVersion GetExchangeServerVersion(string configuredExchangeVersion) {
            const string MethodName = "GetExchangeServerVersion";
            Debug.WriteLine(MethodName + ":entry", ClassName);

            if (configuredExchangeVersion != null) {
                LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Using configured Exchange version: {0}", configuredExchangeVersion);
                switch (configuredExchangeVersion) {
                    case "2007": return ExchangeVersion.E2007;
                    case "2010": return ExchangeVersion.E2010;
                    case "2013": return ExchangeVersion.E2013;
                    default: throw new ArgumentException("Invalid or unsupported Exchange version: " + configuredExchangeVersion + " (supported ones are: 2007, 2010, 2013)");
                }
            }

            LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Trying to determine Exchange version from registered PowerShell snapins");

            const string ExchangeSnapinNamePrefix = "Microsoft.Exchange.Management.PowerShell.";

            ExchangeVersion? version = null;
            using (var runspace = RunspaceFactory.CreateRunspace()) {
                runspace.Open();

                using (var pipeline = runspace.CreatePipeline()) {
                    var getSnapinsCommand = new Command("Get-PSSnapin");
                    getSnapinsCommand.Parameters.Add("Registered");

                    pipeline.Commands.Add(getSnapinsCommand);

                    var snapinList = pipeline.Invoke();

                    PipelineReader<object> reader = pipeline.Error;
                    PowerShellSupport.CheckErrorsFromReader(reader);

                    runspace.Close();

                    if ((snapinList == null) || (snapinList.Count == 0)) {
                        Trace.TraceError("No snap-in returned");
                        throw new ConnectorException(_messageCatalog.Format("ex_NoPowerShellSnapins", "There are no registered PowerShell snap-ins."));
                    }

                    foreach (var snapin in snapinList) {
                        if (snapin.Properties["Name"] != null && snapin.Properties["Name"].Value != null) {
                            var name = snapin.Properties["Name"].Value.ToString();

                            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Found registered snap-in: {0}", name);

                            if (name.StartsWith(ExchangeSnapinNamePrefix, StringComparison.InvariantCultureIgnoreCase)) {
                                switch (name.Substring(ExchangeSnapinNamePrefix.Length)) {
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

            if (!version.HasValue) {
                throw new ConnectorException(_messageCatalog.Format("ex_NoSupportedExchangeSnapin",
                                                                    "There is no supported Exchange PowerShell snap-in registered."));
            }

            LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Exchange version determined to be {0}", version.ToString());
            Debug.WriteLine(MethodName + ":exit", ClassName);
            return version.Value;
        }

        /// <summary>
        /// Exchange 2007 registry key, used for building the exchange assembly resolver
        /// </summary>
        private const string Exchange2007RegKey = "Software\\Microsoft\\Exchange\\v8.0\\Setup\\";

        /// <summary>
        /// Exchange 2010 registry key, used for building the exchange assembly resolver
        /// </summary>
        // private const string Exchange2010RegKey = "Software\\Microsoft\\ExchangeServer\\v14\\Setup\\";

        /// <summary>
        /// Exchange registry value name, used together with <see cref="Exchange2010RegKey"/> or <see cref="Exchange2007RegKey"/> w.r.t the
        /// Exchange version to manage.
        /// </summary>
        private const string ExchangeRegValueName = "MsiInstallPath";

        /// <summary>
        /// Creates Exchange 2007 Assembly Resolver, <see cref="ResolveEventHandler"/>
        /// </summary>
        /// <param name="sender">The source of the event</param>
        /// <param name="args">A <see cref="System.ResolveEventArgs"/> that contains the event data</param>
        /// <returns>Assembly resolver that resolves Exchange 2007 assemblies</returns>
        internal static Assembly AssemblyResolver2007(object sender, ResolveEventArgs args) {
            // Add path for the Exchange 2007 DLLs
            if (args.Name.Contains("Microsoft.Exchange")) {
                string installPath = GetRegistryStringValue(Exchange2007RegKey, ExchangeRegValueName);
                installPath += "\\bin\\" + args.Name.Split(',')[0] + ".dll";
                return Assembly.LoadFrom(installPath);
            }

            return null;
        }

        /// <summary>
        /// Get registry value, which is expected to be a string
        /// </summary>
        /// <param name="keyName">Registry Key Name</param>
        /// <param name="valName">Registry Value Name</param>
        /// <returns>Registry value</returns>        
        /// <exception cref="ArgumentNullException">If <paramref name="valName"/> is null</exception>
        /// <exception cref="InvalidDataException">If some problem with the registry value</exception>
        internal static string GetRegistryStringValue(string keyName, string valName) {
            const string MethodName = "GetRegistryStringValue";
            Debug.WriteLine(MethodName + "(" + keyName + ", " + valName + ")" + ":entry", ClassName);

            // argument check            
            if (keyName == null) {
                keyName = string.Empty;
            }

            if (valName == null) {
                throw new ArgumentNullException("valName");
            }

            RegistryKey regKey = Registry.LocalMachine.OpenSubKey(keyName, false);
            try {
                if (regKey != null) {
                    object val = regKey.GetValue(valName);
                    if (val != null) {
                        RegistryValueKind regType = regKey.GetValueKind(valName);
                        if (!regType.Equals(RegistryValueKind.String)) {
                            throw new InvalidDataException(String.Format(
                                CultureInfo.CurrentCulture,
                                "Invalid Registry data type, key name: {0} value name: {1} should be String",
                                keyName,
                                valName));
                        }

                        return Convert.ToString(val, CultureInfo.CurrentCulture);
                    } else {
                        throw new InvalidDataException(String.Format(
                            CultureInfo.CurrentCulture,
                            "Missing value for key name: {0} value name: {1}",
                            keyName,
                            valName));
                    }
                } else {
                    throw new InvalidDataException(String.Format(
                        CultureInfo.CurrentCulture,
                        "Unable to open registry for key: {0}",
                        keyName));
                }
            } finally {
                if (regKey != null) {
                    regKey.Close();
                }

                Debug.WriteLine(MethodName + ":exit", ClassName);
            }
        }

        public void Dispose() {
            if (_powerShellSupport != null) {
                _powerShellSupport.Dispose();
                _powerShellSupport = null;
            }
        }
    }
}
