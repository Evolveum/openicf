// <copyright file="SerializableScriptingInfo.cs" company="Evolveum">
//
// Copyright (c) 2010-2013 Evolveum
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// </copyright>
// <author>Pavol Mederly</author>
namespace Org.IdentityConnectors.Exchange.Data
{
    using Org.IdentityConnectors.Common;
    using Org.IdentityConnectors.Framework.Common.Objects;
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.IO;
    using System.Text;
    using System.Xml.Serialization;

    public class ScriptingInfo
    {
        [XmlElement("OperationInfo")]
        public OperationInfo[] OperationInfo { get; set; }

        override public string ToString()
        {
            StringBuilder sb = new StringBuilder();
            sb.Append("ScriptingInfo: {\n");
            foreach (OperationInfo oi in OperationInfo)
            {
                sb.Append(oi).Append("\n");
            }
            sb.Append("}");
            return sb.ToString();
        }

        internal IList<ScriptToRun> GetScriptsFor(string operationName, ObjectClass objectClass, Scripting.Position position)
        {
            var rv = new List<ScriptToRun>();
            foreach (OperationInfo operationInfo in OperationInfo)
            {
                // does operation name match?
                bool match;
                if (operationInfo.OpType == null || operationInfo.OpType.Length == 0)
                {
                    match = true;
                }
                else
                {
                    match = false;
                    foreach (string specifiedOpType in operationInfo.OpType)
                    {
                        if (specifiedOpType.Equals(operationName, StringComparison.CurrentCultureIgnoreCase))
                        {
                            match = true;
                        }
                    }
                }

                if (match)
                {
                    CollectionUtil.AddAll(rv, operationInfo.GetScriptsFor(objectClass, position));
                }
            }
            // TODO sort the list before return
            return rv;
        }

        internal void ReadFiles()
        {
            if (OperationInfo != null)
            {
                foreach (OperationInfo operationInfo in OperationInfo)
                {
                    operationInfo.ReadFiles();
                }
            }
        }
    }

    public class OperationInfo
    {
        [XmlElement("Type")]
        public string[] OpType { get; set; }

        [XmlElement("BeforeMain")]
        public ScriptToRun[] BeforeMain { get; set; }

        [XmlElement("InsteadOfMain")]
        public ScriptToRun[] InsteadOfMain { get; set; }

        [XmlElement("AfterMain")]
        public ScriptToRun[] AfterMain { get; set; }

        internal IList<ScriptToRun> GetScriptsFor(ObjectClass objectClass, Scripting.Position position)
        {
            var rv = new List<ScriptToRun>();
            ScriptToRun[] scripts;
            switch (position)
            {
                case Scripting.Position.BeforeMain: scripts = BeforeMain; break;
                case Scripting.Position.InsteadOfMain: scripts = InsteadOfMain; break;
                case Scripting.Position.AfterMain: scripts = AfterMain; break;
                default: throw new ArgumentException("Invalid position: " + position);
            }
            if (scripts != null)
            {
                foreach (ScriptToRun scriptInfo in scripts)
                {
                    if (scriptInfo.IsRelevantFor(objectClass))
                    {
                        rv.Add(scriptInfo);
                    }
                }
            }
            return rv;
        }

        override public string ToString()
        {
            StringBuilder sb = new StringBuilder();
            sb.Append("  OperationInfo: {\n");
            foreach (string type in OpType)
            {
                sb.Append("    type: ").Append(type).Append("\n");
            }
            if (BeforeMain != null)
            {
                foreach (ScriptToRun si in BeforeMain)
                {
                    sb.Append("    before: ").Append(si).Append("\n");
                }
            }
            if (InsteadOfMain != null)
            {
                foreach (ScriptToRun si in InsteadOfMain)
                {
                    sb.Append("    instead-of: ").Append(si).Append("\n");
                }
            }
            if (AfterMain != null)
            {
                foreach (ScriptToRun si in AfterMain)
                {
                    sb.Append("    after: ").Append(si).Append("\n");
                }
            }
            sb.Append("  }");
            return sb.ToString();
        }

        internal void ReadFiles()
        {
            ReadFiles(BeforeMain);
            ReadFiles(InsteadOfMain);
            ReadFiles(AfterMain);
        }

        internal void ReadFiles(ScriptToRun[] scripts)
        {
            if (scripts != null)
            {
                foreach (ScriptToRun scriptToRun in scripts)
                {
                    scriptToRun.ReadFile();
                }
            }
        }

    }

    public class ScriptToRun
    {
        [XmlElement("ObjectType")]
        public string[] ObjectType { get; set; }

        public string File { get; set; }
        public string Content { get; set; }
        public int Order { get; set; }

        public override string ToString()
        {
            return (ObjectType != null ? ("T: " + String.Join(", ", ObjectType) + ", ") : "") + "File: " + File + ", Order: " + Order;
        }

        internal bool IsRelevantFor(ObjectClass objectClass)
        {
            if (ObjectType == null || ObjectType.Length == 0)
            {
                return true;
            }
            else
            {
                foreach (string objectTypeName in ObjectType)
                {
                    if (objectClass.Is(objectTypeName))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        internal void ReadFile()
        {
            FileStream stream = System.IO.File.Open(File, FileMode.Open);           // todo check if file is null
            TextReader streamReader = new StreamReader(stream);
            try
            {
                Content = streamReader.ReadToEnd();
            }
            finally
            {
                streamReader.Close();
            }
            Trace.TraceInformation("{0} successfully read", File);
        }
    }
}
