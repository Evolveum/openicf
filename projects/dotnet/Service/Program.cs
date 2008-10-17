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
using System.ComponentModel;
using System.Configuration;
using System.Configuration.Install;
using System.Collections.Generic;
using System.Security;
using System.Reflection;
using System.ServiceProcess;
using System.Text;
using Org.IdentityConnectors.Common.Security;

namespace Org.IdentityConnectors.Framework.Service
{
    
	static class Program
	{
        private const string OPT_SERVICE_NAME="/serviceName";
        
	    private static void Usage()
	    {
	        Console.WriteLine("Usage: ConnectorServer.exe <command> [option], where command is one of the following: ");
	        Console.WriteLine("       /install [/serviceName <serviceName>] - Installs the service.");
	        Console.WriteLine("       /uninstall [/serviceName <serviceName>] - Uninstalls the service.");
	        Console.WriteLine("       /run - Runs the service from the console.");
	        Console.WriteLine("       /setKey [<key>] - Sets the connector server key.");
	    }
	    
	    private static IDictionary<string, string> ParseOptions(string [] args)
	    {
	        IDictionary<string, string> rv = new Dictionary<string, string>();
	        String serviceName = null;
	        for ( int i = 1; i < args.Length; i++) {
	            String opt = args[i].ToLower();
	            if (OPT_SERVICE_NAME.ToLower().Equals(opt)) {
	                i++;
	                if (i < args.Length) {
	                    serviceName = args[i];
	                }
	                else {
	                    Usage();
	                    return null;
	                }
	            }
	            else {
	                Usage();
	                return null;
	            }
	        }
	        rv["/serviceName"] = serviceName;
	        return rv;
	    }
	    
		/// <summary>
		/// This method starts the service.
		/// </summary>
		static void Main(string [] args)
		{
		    if ( args.Length == 0 )
		    {
		        //no args - start the service
		        ServiceBase.Run(new ServiceBase[] { new Service() });
		    }
		    else 
		    {
		        String cmd = args[0].ToLower();
		        if ( cmd.Equals("/setkey") ) {
		            if ( args.Length > 2 ) {
		                Usage();
		                return;
		            }
		            DoSetKey(args.Length>1?args[1]:null);
		            return;
		        }
		        IDictionary<string, string> options =
		            ParseOptions(args);
		        if ( options == null ) {
		            //there's a parse error in the options, return
		            return;
		        }
		        if ( "/install".Equals(cmd) ) {
		            DoInstall(options);
		        }
		        else if ("/uninstall".Equals(cmd)) {
		            DoUninstall(options);		            
		        }
		        else if ("/run".Equals(cmd)) {
		            DoRun(options);
		        }
		        else {
		            Usage();
		            return;
		        }
		    }
		}
		    
		private static void DoInstall(IDictionary<string, string> options)
		{
		    String serviceName = options[OPT_SERVICE_NAME];
		    if ( serviceName != null ) {
		        ProjectInstaller.ServiceName = serviceName;
		    }
		    TransactedInstaller ti = new TransactedInstaller();
		    string [] cmdline =
		    {
    		    Assembly.GetExecutingAssembly ().Location
		    };
            AssemblyInstaller ai = new AssemblyInstaller(
		        cmdline[0],
		        new string[0]);
            ti.Installers.Add (ai); 
            InstallContext ctx = new InstallContext ("install.log", 
                                                     cmdline );
            ti.Context =  ctx ; 
            ti.Install ( new System.Collections.Hashtable());
		}
		
		private static void DoUninstall(IDictionary<string, string> options)
		{
		    String serviceName = options[OPT_SERVICE_NAME];
		    if ( serviceName != null ) {
		        ProjectInstaller.ServiceName = serviceName;
		    }
		    TransactedInstaller ti = new TransactedInstaller();
		    string [] cmdline =
		    {
    		    Assembly.GetExecutingAssembly ().Location
		    };
            AssemblyInstaller ai = new AssemblyInstaller(
		        cmdline[0],
		        new string[0]);
            ti.Installers.Add (ai); 
            InstallContext ctx = new InstallContext ("uninstall.log", 
                                                     cmdline );
            ti.Context =  ctx ; 
            ti.Uninstall ( null);
		}
		
		private static void DoRun(IDictionary<string, string> options)
		{
              Service svc = new Service();
        
              svc.StartService(new String[0]);
        
              Console.WriteLine("Press q to shutdown.");
        
              while (Console.ReadKey().KeyChar != 'q') ;
        
              svc.StopService();
		}
		
		private static GuardedString ReadPassword()
		{
		    GuardedString rv = new GuardedString();
		    while (true) {
		        ConsoleKeyInfo info = Console.ReadKey(true);
		        if ( info.Key == ConsoleKey.Enter ) {
		            Console.WriteLine();
		            rv.MakeReadOnly();
		            return rv;
		        }
		        else {
		            Console.Write("*");
		            rv.AppendChar(info.KeyChar);
		        }
		    }
		}
		
		private static void DoSetKey(string key)
		{
		    GuardedString str;
		    if ( key == null ) {
		        Console.Write("Enter Key: ");
		        GuardedString v1 = ReadPassword();
		        Console.Write("Confirm Key: ");
		        GuardedString v2 = ReadPassword();
		        if (!v1.Equals(v2)) {
		            Console.WriteLine("Error: Key mismatch.");
		            return;
		        }
		        str = v2;
		    }
		    else {
    		    str = new GuardedString();
    		    foreach (char c in key.ToCharArray()) {
    		        str.AppendChar(c);
    		    }
		    }
		    Configuration config = 
		        ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);
		    config.AppSettings.Settings.Remove(Service.PROP_KEY);
		    config.AppSettings.Settings.Add(Service.PROP_KEY,str.GetBase64SHA1Hash());
		    config.Save(ConfigurationSaveMode.Modified);
		    Console.WriteLine("Key Updated.");
		}
	}
	


}
