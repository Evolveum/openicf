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

using System.ComponentModel;
using System.Configuration.Install;
using System.ServiceProcess;

namespace Org.IdentityConnectors.Framework.Service
{
	[RunInstaller(true)]
	public class ProjectInstaller : Installer
	{
	    public static string ServiceName { get; set; }
        public static string DisplayName { get; set; }
        public static string Description { get; set; }
	    
	    static ProjectInstaller()
	    {
            ServiceName = "ConnectorServerService";
            DisplayName = "OpenICF Connector Server .Net";
            Description = "OpenICF Connector Server .Net";
	    }
	    
		private ServiceProcessInstaller serviceProcessInstaller;
		private ServiceInstaller serviceInstaller;
		
		public ProjectInstaller()
		{
			serviceProcessInstaller = new ServiceProcessInstaller();
			serviceInstaller = new ServiceInstaller();
		
			// Here you can set properties on serviceProcessInstaller or register event handlers
			serviceProcessInstaller.Account = ServiceAccount.LocalSystem;
			
			serviceInstaller.ServiceName = ServiceName;
            serviceInstaller.Description = Description;
            serviceInstaller.DisplayName = DisplayName;
            serviceInstaller.StartType = ServiceStartMode.Automatic;

			this.Installers.AddRange(new Installer[] { serviceProcessInstaller, serviceInstaller });
		}

        protected virtual string AppendPathParameter(string path, string parameter)
        {
            if (path.Length > 0 && path[0] != '"')
            {
                path = "\"" + path + "\"";
            }
            path += " " + parameter;
            return path;
        }

        protected override void OnBeforeInstall(System.Collections.IDictionary savedState)
        {
            Context.Parameters["assemblypath"] = AppendPathParameter(Context.Parameters["assemblypath"], "/service");
            base.OnBeforeInstall(savedState);
        }

        protected override void OnBeforeUninstall(System.Collections.IDictionary savedState)
        {
            Context.Parameters["assemblypath"] = AppendPathParameter(Context.Parameters["assemblypath"], "/service");
            base.OnBeforeUninstall(savedState);
        }
	}
}
