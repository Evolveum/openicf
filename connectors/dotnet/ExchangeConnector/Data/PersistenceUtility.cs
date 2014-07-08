﻿// <copyright file="PersistenceUtility.cs" company="Sun Microsystems, Inc.">
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
    using System.Diagnostics;
    using System.Globalization;
    using System.IO;
    using System.Reflection;
    using System.Xml.Serialization;

    /// <summary>
    /// Persitence helper class, it uses <see cref="XmlSerializer"/> to read the persistent data
    /// </summary>
    internal sealed class PersistenceUtility
    {
        /// <summary>
        /// Prevents a default instance of the <see cref="PersistenceUtility" /> class from being created. 
        /// </summary>
        private PersistenceUtility()
        {
        }

        /// <summary>
        /// Reads the <see cref="SerializableCommandInfo"/> from persistent store (xml file)
        /// </summary>
        /// <returns>List of <see cref="SerializableCommandInfo"/></returns>
        /// <exception cref="IOException">if not able to read from persistent store</exception>
        internal static IList<SerializableCommandInfo> ReadCommandInfo()
        {
            Trace.TraceInformation("PersistenceUtility.ReadCommandInfo entry");
            // persistent file
            const string PersistFile = "Org.IdentityConnectors.Exchange.Data.CommandInfos.xml";

            Assembly assembly = Assembly.GetExecutingAssembly();
            Stream stream = assembly.GetManifestResourceStream(PersistFile);
            if (stream == null)
            {
                throw new IOException(
                        string.Format(CultureInfo.CurrentCulture, "Unable to read the {0} file from Assembly", PersistFile));
            }
            //Trace.TraceInformation("PersistenceUtility.ReadCommandInfo having stream = " + stream);

            // we just read
            using (TextReader streamReader = new StreamReader(stream))
            {
                //Trace.TraceInformation("PersistenceUtility.ReadCommandInfo creating a serializer");
                XmlSerializer ser = new XmlSerializer(typeof(List<SerializableCommandInfo>));
                //Trace.TraceInformation("PersistenceUtility.ReadCommandInfo calling Deserialize");
                List<SerializableCommandInfo> commandInfos = (List<SerializableCommandInfo>)ser.Deserialize(streamReader);
                //Trace.TraceInformation("PersistenceUtility.ReadCommandInfo exit");
                return commandInfos;
            }            
        }

        internal static ScriptingInfo ReadScriptingInfo(string filename)
        {
            Trace.TraceInformation("Reading scripting info from file {0}", filename);

            Stream stream = File.Open(filename, FileMode.Open);
            using (TextReader streamReader = new StreamReader(stream))
            {
                XmlSerializer ser = new XmlSerializer(typeof(ScriptingInfo));
                ScriptingInfo scriptingInfo = (ScriptingInfo)ser.Deserialize(streamReader);
                int count = scriptingInfo != null && scriptingInfo.OperationInfo != null ? scriptingInfo.OperationInfo.Length : 0;
                Trace.TraceInformation("{0} operation definition(s) read", count);
                return scriptingInfo;
            }
        }            

    }
}
