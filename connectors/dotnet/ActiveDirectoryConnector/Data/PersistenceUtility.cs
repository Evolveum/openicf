// <copyright file="PersistenceUtility.cs" company="Sun Microsystems, Inc.">
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

namespace Org.IdentityConnectors.ActiveDirectory.Data
{
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.Globalization;
    using System.IO;
    using System.Reflection;
    using System.Xml.Serialization;

    /// <summary>
    /// Persitence helper class, it uses <see cref="XmlSerializer"/> to read the persistent data
    /// </summary>
    public sealed class PersistenceUtility
    {
        // tracing (using ActiveDirectoryConnector's name!)
        internal static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        /// <summary>
        /// Prevents a default instance of the <see cref="PersistenceUtility" /> class from being created. 
        /// </summary>
        private PersistenceUtility() {
        }

        internal static ScriptingInfo ReadScriptingInfo(string filename) {
            LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Reading scripting info from file {0}", filename);

            Stream stream = File.Open(filename, FileMode.Open);
            using (TextReader streamReader = new StreamReader(stream)) {
                XmlSerializer ser = new XmlSerializer(typeof(ScriptingInfo));
                ScriptingInfo scriptingInfo = (ScriptingInfo)ser.Deserialize(streamReader);
                int count = scriptingInfo != null && scriptingInfo.OperationInfo != null ? scriptingInfo.OperationInfo.Length : 0;
                LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "{0} operation definition(s) read", count);
                return scriptingInfo;
            }
        }
    }
}
