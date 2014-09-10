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
using System.Collections.Generic;
using System.Reflection;
using System.IO;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Serializer;
using System.Text;
using System.Diagnostics;
using System.Globalization;
using System.Collections;
using Org.IdentityConnectors.Framework.Common;

namespace Org.IdentityConnectors.ActiveDirectory
{
    public class CommonUtils
    {
        // tracing (using ActiveDirectoryConnector's name!)
        internal static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        public static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfoFromFile(string fileName) {
            String fullPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, fileName);
            LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Reading ObjectClass information from file {0}", fullPath);
            var stream = File.Open(fullPath, FileMode.Open);
            return GetOCInfoInternal(stream, true);
        }

        public static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfoFromExecutingAssembly(string resourceName) {
            return GetOCInfoFromAssembly(resourceName, Assembly.GetExecutingAssembly());
        }
        
        public static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfoFromAssembly(string resourceName, Assembly assembly) {
            LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Reading ObjectClass information from assembly resource {0}", resourceName);
            var stream = assembly.GetManifestResourceStream(resourceName);
            return GetOCInfoInternal(stream, false);
        }

        private static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfoInternal(Stream stream, bool dump) {
            Assertions.NullCheck(stream, "stream");

            //we just read
            TextReader streamReader = new StreamReader(stream);
            String xml;
            try {
                xml = streamReader.ReadToEnd();
            } finally {
                streamReader.Close();
            }

            if (dump) {
                LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "XML = {0}", xml);
            }

            //read from xml
            var ret = (ICollection<object>)SerializerUtil.DeserializeXmlObject(xml, true);

            Assertions.NullCheck(ret, "ret");

            //create map of object infos
            var map = new Dictionary<ObjectClass, ObjectClassInfo>(ret.Count);
            foreach (ObjectClassInfo o in ret) {
                map.Add(new ObjectClass(o.ObjectType.ToString()), o);
            }

            return map;
        }

        public static IDictionary<ObjectClass, ObjectClassInfo> MergeOCInfo(IDictionary<ObjectClass, ObjectClassInfo> global, IDictionary<ObjectClass, ObjectClassInfo> local)
        {
            var infos = new List<IDictionary<ObjectClass, ObjectClassInfo>>();
            infos.Add(global);
            if (local != null)
            {
                infos.Add(local);
            }
            return MergeOCInfo(infos);
        }

        // Merges two ObjectClassInfos
        public static IDictionary<ObjectClass, ObjectClassInfo> MergeOCInfo(IList<IDictionary<ObjectClass, ObjectClassInfo>> infos)
        {
            if (infos.Count == 0)
            {
                return null;
            }
            else if (infos.Count == 1)
            {
                return infos[0];
            }
            else
            {
                IDictionary<ObjectClass, ObjectClassInfo> rv = new Dictionary<ObjectClass, ObjectClassInfo>();
                foreach (IDictionary<ObjectClass, ObjectClassInfo> info in infos) 
                {
                    Merge(rv, info);
                }
                return rv;
            }
        }

        private static void Merge(IDictionary<ObjectClass, ObjectClassInfo> target, IDictionary<ObjectClass, ObjectClassInfo> source)
        {
            foreach (ObjectClass oc in source.Keys)
            {
                ObjectClassInfo sourceOCI = source[oc];
                if (!target.ContainsKey(oc))
                {
                    ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
                    builder.ObjectType = sourceOCI.ObjectType;
                    builder.IsContainer = sourceOCI.IsContainer;
                    builder.AddAllAttributeInfo(sourceOCI.ConnectorAttributeInfos);
                    ObjectClassInfo targetOCI = builder.Build();
                    LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Adding object class info {0}", targetOCI.ObjectType);
                    target.Add(oc, targetOCI);
                }
                else
                {
                    ObjectClassInfo targetOCI = target[oc];
                    if (!targetOCI.ObjectType.Equals(sourceOCI.ObjectType))
                    {
                        throw new ArgumentException("Incompatible ObjectType for object class " + oc);
                    }
                    if (targetOCI.IsContainer != sourceOCI.IsContainer)
                    {
                        throw new ArgumentException("Incompatible Container flag for object class " + oc);
                    }
                    ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
                    builder.ObjectType = targetOCI.ObjectType;
                    builder.IsContainer = targetOCI.IsContainer;
                    builder.AddAllAttributeInfo(targetOCI.ConnectorAttributeInfos);
                    foreach (ConnectorAttributeInfo info in sourceOCI.ConnectorAttributeInfos)
                    {
                        if (info.Is(Name.NAME))
                        {
                            // The __NAME__ attribute is a special one and has to be provided on each object class.
                            // So, even if we just want to extend an object class with a few attributes, we have to provide
                            // artificial __NAME__ attribute there. When merging, we simply skip it.
                            continue;
                        }

                        foreach (ConnectorAttributeInfo existingInfo in targetOCI.ConnectorAttributeInfos)
                        {
                            if (existingInfo.Is(info.Name))
                            {
                                throw new ArgumentException("Attempted to redefine attribute " + info.Name);
                            }
                        }
                        LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Adding connector attribute info {0}:{1}", info.Name, info.ValueType);
                        builder.AddAttributeInfo(info);
                    }
                    ObjectClassInfo targetRebuilt = builder.Build();
                    LOGGER.TraceEvent(TraceEventType.Information, CAT_DEFAULT, "Replacing object class info {0}", targetOCI.ObjectType);
                    target.Remove(oc);
                    target.Add(oc, targetRebuilt);
                }
            }
        }

        // TODO find a better place for this method
        public static string DumpConnectorAttributes(ICollection<ConnectorAttribute> attributes)
        {
            StringBuilder sb = new StringBuilder();
            if (attributes != null)
            {
                foreach (ConnectorAttribute attribute in attributes)
                {
                    sb.Append(" - ").Append(attribute.GetDetails()).Append("\n");
                }
            }
            else
            {
                sb.Append("(null)");
            }
            return sb.ToString();
        }

        public static string ReadResource(string filename, Assembly assembly) {
            if (assembly == null) {
                assembly = Assembly.GetExecutingAssembly();
            }
            Stream stream = assembly.GetManifestResourceStream(filename);
            if (stream == null) {
                throw new IOException(
                        string.Format(CultureInfo.CurrentCulture, "Unable to read the {0} file from Assembly", filename));
            }

            using (TextReader streamReader = new StreamReader(stream)) {
                return streamReader.ReadToEnd();
            }
        }

        /// <summary>
        /// Converts any value to a form that can be stored into connector attribute.
        /// </summary>
        /// <param name="value"></param>
        /// <param name="isMultivalued"></param>
        /// <returns></returns>
        public static ICollection<object> ConvertToSupportedForm(ConnectorAttributeInfo cai, object value) {
            IList<object> rv = new List<object>();
            if (cai.IsMultiValued && value is IEnumerable) {
                foreach (object v in (IEnumerable)value) {
                    if (v == null) {
                        continue;
                    }
                    if (!IsSupported(cai, v)) {
                        rv.Add(v.ToString());
                    } else {
                        rv.Add(v);
                    }
                }
            } else {
                if (value != null && !IsSupported(cai, value)) {
                    rv.Add(value.ToString());
                } else {
                    rv.Add(value);
                }
            }
            return rv;
        }

        private static bool IsSupported(ConnectorAttributeInfo cai, object value) {
            if (!FrameworkUtil.IsSupportedAttributeType(value.GetType())) {
                LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, 
                    "Unsupported attribute type ... calling ToString (Name: \'{0}\' Type: \'{1}\' String Value: \'{2}\'",
                    cai.Name, value.GetType(), value.ToString());
                return false;
            } else {
                return true;
            }
        }

    }
}
