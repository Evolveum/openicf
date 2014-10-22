/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 * Portions Copyrighted 2014 ForgeRock AS.
 */
using System;
using System.Configuration;
using System.Configuration.Install;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Security.Cryptography;
using System.ServiceProcess;
using Org.IdentityConnectors.Common.Security;
using System.Security.Cryptography.X509Certificates;
using Sun.OpenConnectors.Framework.Service.Properties;

namespace Org.IdentityConnectors.Framework.Service
{

    static class Program
    {
        private const string OPT_SERVICE_NAME = "/serviceName";
        private const string OPT_CERTSTOR_NAME = "/storeName";
        private const string OPT_CERTFILE_NAME = "/certificateFile";

        private static void Usage()
        {
            Console.WriteLine("Usage: ConnectorServer.exe <command> [option], where command is one of the following: ");
            Console.WriteLine("       /install [/serviceName <serviceName>] - Installs the service.");
            Console.WriteLine("       /uninstall [/serviceName <serviceName>] - Uninstalls the service.");
            Console.WriteLine("       /run - Runs the service from the console.");
            Console.WriteLine("       /setKey [<key>] - Sets the connector server key.");
            Console.WriteLine("       /setDefaults - Sets default app.config");
            Console.WriteLine("       /storeCertificate [/storeName <certificatestorename>] [/certificateFile <certificate>]- Stores the Certificate in the storage.");
        }

        private static IDictionary<string, string> ParseOptions(string[] args)
        {
            IDictionary<string, string> rv = new Dictionary<string, string>();

            for (int i = 1; i < args.Length; i++)
            {
                String optionName = null;
                String opt = args[i].ToLower();

                if (OPT_SERVICE_NAME.Equals(opt, StringComparison.InvariantCultureIgnoreCase))
                {
                    optionName = OPT_SERVICE_NAME;
                }
                else if (OPT_CERTFILE_NAME.Equals(opt, StringComparison.InvariantCultureIgnoreCase))
                {
                    optionName = OPT_CERTFILE_NAME;
                }
                else if (OPT_CERTSTOR_NAME.Equals(opt, StringComparison.InvariantCultureIgnoreCase))
                {
                    optionName = OPT_CERTSTOR_NAME;
                }
                if (optionName != null)
                {
                    i++;
                    if (i < args.Length)
                    {
                        rv[optionName] = args[i];
                    }
                    else
                    {
                        Usage();
                        return null;
                    }
                }
                else
                {
                    Usage();
                    return null;
                }
            }
            return rv;
        }

        /// <summary>
        /// This method starts the service.
        /// </summary>
        static void Main(string[] args)
        {
            if (args.Length == 0)
            {
                Usage();
            }
            else
            {
                String cmd = args[0].ToLower();
                if (cmd.Equals("/setkey", StringComparison.InvariantCultureIgnoreCase))
                {
                    if (args.Length > 2)
                    {
                        Usage();
                        return;
                    }
                    DoSetKey(args.Length > 1 ? args[1] : null);
                    return;
                }
                if (cmd.Equals("/setDefaults", StringComparison.InvariantCultureIgnoreCase))
                {
                    if (args.Length > 1)
                    {
                        Usage();
                        return;
                    }
                    using (var file = new StreamWriter(AppDomain.CurrentDomain.SetupInformation.ConfigurationFile, false))
                    {
                        file.WriteLine(Resources.ResourceManager.GetString("DefaultConfig"));
                        Console.WriteLine("Default configuration successfully restored.");
                    }                    
                    return;
                }
                IDictionary<string, string> options =
                    ParseOptions(args);
                if (options == null)
                {
                    //there's a parse error in the options, return
                    return;
                }
                if ("/install".Equals(cmd, StringComparison.InvariantCultureIgnoreCase))
                {
                    DoInstall(options);
                }
                else if ("/uninstall".Equals(cmd, StringComparison.InvariantCultureIgnoreCase))
                {
                    DoUninstall(options);
                }
                else if ("/run".Equals(cmd, StringComparison.InvariantCultureIgnoreCase))
                {
                    DoRun(options);
                }
                else if ("/service".Equals(cmd, StringComparison.InvariantCultureIgnoreCase))
                {
                    ServiceBase.Run(new ServiceBase[] { new Service() });
                }
                else if ("/storecertificate".Equals(cmd, StringComparison.InvariantCultureIgnoreCase))
                {
                    DoStoreCertificate(options);
                }
                else
                {
                    Usage();
                    return;
                }
            }
        }

        private static void DoInstall(IDictionary<string, string> options)
        {
            if (options.ContainsKey(OPT_SERVICE_NAME))
            {
                ProjectInstaller.ServiceName = options[OPT_SERVICE_NAME];
            }
            TransactedInstaller ti = new TransactedInstaller();
            string[] cmdline =
		    {
    		    Assembly.GetExecutingAssembly ().Location
		    };
            AssemblyInstaller ai = new AssemblyInstaller(
                cmdline[0],
                new string[0]);
            ti.Installers.Add(ai);
            InstallContext ctx = new InstallContext("install.log",
                                                     cmdline);
            ti.Context = ctx;
            ti.Install(new System.Collections.Hashtable());
        }

        private static void DoUninstall(IDictionary<string, string> options)
        {
            if (options.ContainsKey(OPT_SERVICE_NAME))
            {
                ProjectInstaller.ServiceName = options[OPT_SERVICE_NAME];
            }
            TransactedInstaller ti = new TransactedInstaller();
            string[] cmdline =
		    {
    		    Assembly.GetExecutingAssembly ().Location
		    };
            AssemblyInstaller ai = new AssemblyInstaller(
                cmdline[0],
                new string[0]);
            ti.Installers.Add(ai);
            InstallContext ctx = new InstallContext("uninstall.log",
                                                     cmdline);
            ti.Context = ctx;
            ti.Uninstall(null);
        }

        private static void DoRun(IDictionary<string, string> options)
        {
            Service svc = new Service();

            svc.StartService(new String[0]);

            Console.WriteLine("Press q to shutdown.");
            Console.WriteLine("Press t for a thread dump.");

            while (true)
            {
                ConsoleKeyInfo info = Console.ReadKey();
                if (info.KeyChar == 'q')
                {
                    break;
                }
                else if (info.KeyChar == 't')
                {
                    svc.DumpRequests();
                }
            }

            svc.StopService();
        }

        private static GuardedString ReadPassword()
        {
            GuardedString rv = new GuardedString();
            while (true)
            {
                ConsoleKeyInfo info = Console.ReadKey(true);
                if (info.Key == ConsoleKey.Enter)
                {
                    Console.WriteLine();
                    rv.MakeReadOnly();
                    return rv;
                }
                else
                {
                    Console.Write("*");
                    rv.AppendChar(info.KeyChar);
                }
            }
        }

        private static void DoSetKey(string key)
        {
            GuardedString str;
            if (key == null)
            {
                Console.Write("Please enter the new key: ");
                GuardedString v1 = ReadPassword();
                Console.Write("Please confirm the new key: ");
                GuardedString v2 = ReadPassword();
                if (!v1.Equals(v2))
                {
                    Console.WriteLine("Error: Key mismatch.");
                    return;
                }
                str = v2;
            }
            else
            {
                str = new GuardedString();
                foreach (char c in key)
                {
                    str.AppendChar(c);
                }
            }
            Configuration config =
                ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);
            config.AppSettings.Settings.Remove(Service.PROP_KEY);
            config.AppSettings.Settings.Add(Service.PROP_KEY, str.GetBase64SHA1Hash());
            config.Save(ConfigurationSaveMode.Modified);
            Console.WriteLine("Key has been successfully updated.");
        }

        private static void DoStoreCertificate(IDictionary<string, string> options)
        {
            string storeName = options.ContainsKey(OPT_CERTSTOR_NAME) ? options[OPT_CERTSTOR_NAME] : "ConnectorServerSSLCertificate";


            if (!options.ContainsKey(OPT_CERTFILE_NAME) || String.IsNullOrEmpty(options[OPT_CERTFILE_NAME]))
            {
                Usage();
                throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConfigurationException("Missing required argument: " + OPT_CERTFILE_NAME);
            }
            X509Certificate2 certificate = null;
            try
            {
                certificate = new X509Certificate2(options[OPT_CERTFILE_NAME]);
            }
            catch (CryptographicException)
            {
                Console.Write("Please enter the keystore password: ");
                GuardedString v1 = ReadPassword();
                certificate = new X509Certificate2(options[OPT_CERTFILE_NAME], SecurityUtil.Decrypt(v1), X509KeyStorageFlags.MachineKeySet | X509KeyStorageFlags.PersistKeySet | X509KeyStorageFlags.Exportable);
            }
            X509Store store = new X509Store(storeName, StoreLocation.LocalMachine);

            store.Open(OpenFlags.ReadWrite);
            X509CertificateCollection certificates = store.Certificates;
            if (certificates.Count != 0)
            {
                if (certificates.Count == 1)
                {
                    store.Remove(store.Certificates[0]);
                    Console.WriteLine("Previous certificate has been removed.");
                }
                else
                {
                    Console.WriteLine("There are multiple certificates were found. You may point to the wrong store.");
                    throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConfigurationException("There is supported to be exactly one certificate in the store: " + storeName);
                }
            }
            store.Add(certificate);
            store.Close();
            Console.WriteLine("Certificate is stored in " + storeName);
        }
    }
}