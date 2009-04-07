// <copyright file="SerializableCommandInfo.cs" company="Sun Microsystems, Inc.">
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
namespace Org.IdentityConnectors.Exchange.Data
{
    using System.Collections.Generic;

    /// <summary>
    /// Command metadata data object, it has to be public in order to be 
    /// used by <see cref="System.Xml.Serialization.XmlSerializer"/>,
    /// can be also made "struct" however we don't expect huge amount of data here
    /// </summary>    
    public class SerializableCommandInfo
    {
        /// <summary>
        /// Command parameters local variable
        /// </summary>
        private readonly List<string> parameters;

        /// <summary>
        /// Initializes a new instance of the <see cref="SerializableCommandInfo" /> class. 
        /// </summary>
        public SerializableCommandInfo()
        {
            this.parameters = new List<string>();
        }

        /// <summary>
        /// Gets or sets Command name
        /// </summary>
        public string Name { get; set; }

        /// <summary>
        /// Gets or sets special parameter name used as id for this command
        /// </summary>
        public string NameParameter { get; set; }

        /// <summary>
        /// Gets Command parameters - only string type is supported
        /// <para>
        /// Note: Cann't be IList because it is not supported by <see cref="System.Xml.Serialization.XmlSerializer"/>
        /// </para>        
        /// </summary>
        [System.Xml.Serialization.XmlArray("Parameters")]
        [System.Xml.Serialization.XmlArrayItem("string", typeof(string))]        
        public List<string> Parameters 
        { 
            get
            {
                return this.parameters; 
            }            
        }
        
        /// <summary>
        /// Adds parameter to command parameter list
        /// </summary>
        /// <param name="parameter">string to be added as parameter</param>
        public void AddParameter(string parameter)
        {
            this.parameters.Add(parameter);
        }
    }
}
