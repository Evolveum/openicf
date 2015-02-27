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
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
using System;
using System.Text;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Common.Exceptions;
using Org.IdentityConnectors.Framework.Spi.Operations;

namespace Org.IdentityConnectors.Sample
{
    public class SampleConfiguration : Org.IdentityConnectors.Framework.Spi.AbstractConfiguration
    {
        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_Username",
            Required = true, HelpMessageKey = "help_Username", Order = 1)]
        public String Username
        { get; set; }

        [ConfigurationProperty(Confidential = true, DisplayMessageKey = "display_Password",
            Required = true, HelpMessageKey = "help_Password", Order = 2)]
        public String Password
        { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_Property1", 
            HelpMessageKey = "help_Property1", Order = 3)]
        public String Property1
        { get; set; }

        [ConfigurationProperty(Confidential = false, DisplayMessageKey = "display_Property2", 
            Required=true, HelpMessageKey = "help_Property2", Order = 4)]
        public String Property2
        { get; set; }

        public SampleConfiguration()
        {
            Username = "admin";
            Password = "";
        }

        /// <summary>
        /// Determines if the configuration is valid.
        /// </summary>
        /// <remarks>See <see cref="Org.IdentityConnectors.Framework.Spi.Configuration"/> for the definition of a valid
        /// configuration.</remarks>
        /// <exception cref="Org.IdentityConnectors.Framework.Common.Exceptions.ConfigurationException"/>
        /// Thrown when the configuration is not valid.</exception>
        public override void Validate()
        {
            // TODO implement
        }
    }
}
