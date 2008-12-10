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
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Management.Automation.Runspaces;
using System.Reflection;

using Microsoft.Win32;
using System.Xml.Serialization;
using Org.IdentityConnectors.ActiveDirectory;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Objects;

namespace Org.IdentityConnectors.Exchange
{
    /// <summary>
    /// Description of ExchangeUtils.
    /// </summary>
    public class ExchangeUtils : CommonUtils
    {
        private static readonly string CLASS = typeof(ExchangeUtils).ToString();

        private const string OC_DEF_FILE = "Org.IdentityConnectors.Exchange.ObjectClasses.xml";
        private const string EXCHANGE_REG_KEY = "Software\\Microsoft\\Exchange\\v8.0\\Setup\\";
        private const string EXCHANGE_REG_VALUE = "MsiInstallPath";

        /// <summary>use reflection to load the Exchange assembly</summary>
        internal static Assembly AssemblyResolver(object p, ResolveEventArgs args)
        {
            //Add path for the Exchange 2007 DLLs
            if (args.Name.Contains("Microsoft.Exchange"))
            {
                String installPath = GetRegistryStringValue(EXCHANGE_REG_KEY, EXCHANGE_REG_VALUE);
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
        /// <returns></returns>
        internal static String GetRegistryStringValue(string keyName, string valName)
        {
            const string METHOD = "GetRegistryStringValue";
            Debug.WriteLine(METHOD + ":entry", CLASS);
            //argument check            
            if (keyName == null)
            {
                keyName = "";
            }
            if (valName == null)
            {
                throw new ArgumentNullException("valName");
            }

            RegistryKey regKey = Registry.LocalMachine.OpenSubKey(keyName, false);
            try
            {
                Object val = regKey.GetValue(valName);
                if (val != null)
                {
                    RegistryValueKind regType = regKey.GetValueKind(valName);
                    if (!regType.Equals(RegistryValueKind.String))
                    {
                        throw new InvalidDataException(String.Format("Invalid Registry data type, key name: {0} value name: {1} should be String", keyName, valName));
                    }
                    return Convert.ToString(val);
                }
                else
                {
                    throw new InvalidDataException(String.Format("Missing value for key name: {0} value name: {1}", keyName, valName));
                }
            }
            finally
            {
                if (regKey != null)
                {
                    regKey.Close();
                }
                Debug.WriteLine(METHOD + ":exit", CLASS);
            }
        }



        ///<summary>
        /// reads the object class info definitions from xml
        ///</summary>
        ///<returns>Dictionary of object classes</returns>
        internal static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfo()
        {
            return GetOCInfo(OC_DEF_FILE);
        }

        ///<summary>
        ///</summary>
        ///<returns></returns>
        internal static IDictionary<string, XCommandInfo> GetCommandInfo ()
        {
            Assembly assembly = Assembly.GetExecutingAssembly();
            Stream stream = assembly.GetManifestResourceStream("Org.IdentityConnectors.Exchange.CommandInfos.xml");

            Assertions.NullCheck(stream, "stream");

            //we just read
            TextReader streamReader = new StreamReader(stream);

            XmlSerializer ser = new XmlSerializer(typeof(XCommandInfos));
            XCommandInfos cInfos = (XCommandInfos)ser.Deserialize(streamReader);
            streamReader.Close();

            Assertions.NullCheck(cInfos, "cInfos");

            //create map of command infos
            var map = new Dictionary<string, XCommandInfo>(cInfos.XCommandInfo.Length);
            foreach (XCommandInfo o in cInfos.XCommandInfo)
            {
                map.Add(o.Name, o);
            }

            return map;
    
        }

        /// <summary>
        /// creates command based on the commanf info, reading the calues from attributes
        /// </summary>
        /// <param name="cmdInfo">Command defition</param>
        /// <param name="attributes">Attribute values</param>
        /// <returns>Ready to execute Command</returns>
        internal static Command GetCommand(CommandInfo cmdInfo, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(cmdInfo, "cmdInfo");
            Assertions.NullCheck(attributes, "attributes");

            //create command
            Command cmd = new Command(cmdInfo.Name);

            //map name attribute, if mapping specified
            if (!string.IsNullOrEmpty(cmdInfo.NameParameter))
            {                
                object val = GetAttValue(Name.NAME, attributes);
                if (val != null)
                {
                    cmd.Parameters.Add(cmdInfo.NameParameter, val);
                }
            }

            foreach (string attName in cmdInfo.Parameters)
            {
                object val = GetAttValue(attName, attributes);
                if (val != null)
                {
                    cmd.Parameters.Add(attName, val);
                }
            }
            return cmd;
        }

        /// <summary>
        /// Helper method: Gets attribute value from the attribute collection
        /// </summary>
        /// <param name="attName">attribute name</param>
        /// <param name="attributes">collection of attribute</param>
        /// <returns>attribute value as object, null if not found</returns>
        internal static object GetAttValue(String attName, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(attName, "attName");
            Assertions.NullCheck(attributes, "attributes");

            object value = null;
            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);

            if (attribute != null)
            {
                value = ConnectorAttributeUtil.GetSingleValue(attribute);
            }

            return value;
        }

        /// <summary>
        /// Helper method for filtering the specified attributes from collection of attributes
        /// </summary>
        /// <param name="attributes">Collection of attributes</param>
        /// <param name="attName">Attribute names to be filtered out</param>
        /// <returns>Filtered collection of attributes</returns>
        internal static ICollection<ConnectorAttribute> FilterOut(ICollection<ConnectorAttribute> attributes, params string[] attName)
        {
            Assertions.NullCheck(attributes, "attributes");
            if (attName == null || attName.Length == 0)
            {
                return attributes;
            }

            IList names = new ArrayList(attName);            
            ICollection<ConnectorAttribute> filtered = new List<ConnectorAttribute>();
            foreach (ConnectorAttribute attribute in attributes)
            {
                if (!names.Contains(attribute.Name))
                {
                    filtered.Add(attribute);
                }
            }
            return filtered;
        }

        /// <summary>
        /// Helper method - Replaces specified collection Items
        /// </summary>        
        /// <param name="col"></param>
        /// <param name="replace"></param>
        /// <returns></returns>
        internal static ArrayList FilterReplace(ArrayList col, string[,] replace)
        {
            Assertions.NullCheck(col, "col");
            Assertions.NullCheck(replace, "replace");

            ArrayList newcol = (ArrayList) col.Clone();
            for (int i = 0; i < replace.GetLength(0); i++)
            {
                if (newcol.Contains(replace[i,0]))
                {                    
                    newcol.Remove(replace[i, 0]);
                    newcol.Add(replace[i,1]);
                }
            }
            return newcol;
        }

        /// <summary>
        /// finds the attributes in connector object and rename it according to input array of names, but only
        /// if the aatribute name is in attributes to get
        /// </summary>
        /// <param name="cobject"></param>
        /// <param name="attsToGet"></param>
        /// <param name="replace"></param>
        /// <returns></returns>
        internal static ConnectorObject ReplaceAttributes(ConnectorObject cobject, IList attsToGet, string[,] replace)
        {
            Assertions.NullCheck(cobject, "cobject");
            Assertions.NullCheck(attsToGet, "attsToGet");
            Assertions.NullCheck(replace, "replace");

            var attributes = cobject.GetAttributes();
            var builder = new ConnectorObjectBuilder();
            foreach (ConnectorAttribute attribute in attributes)
            {
                for (int i = 0; i < replace.GetLength(0); i++)
                {
                    string oldName = replace[i, 1];
                    string newName = replace[i, 0];
                    if (attsToGet.Contains(newName) && attribute.Name == oldName)
                    {
                        var newAttribute = RenameAttribute(attribute, replace[i, 0]);
                        builder.AddAttribute(newAttribute);
                        break;
                    }
                }

                builder.AddAttribute(attribute);
            }
            builder.AddAttributes(attributes);
            builder.ObjectClass = cobject.ObjectClass;
            builder.SetName(cobject.Name);
            builder.SetUid(cobject.Uid);
            return builder.Build();
        }

        /// <summary>
        /// Renames the connector attribute to new name
        /// </summary>
        /// <param name="cattribute"></param>
        /// <param name="newName"></param>
        /// <returns></returns>
        internal static ConnectorAttribute RenameAttribute(ConnectorAttribute cattribute, string newName)
        {
            Assertions.NullCheck(cattribute, "cattribute");
            Assertions.NullCheck(newName, "newName");

            var caBuilder = new ConnectorAttributeBuilder();
            caBuilder.AddValue(cattribute.Value);
            caBuilder.Name = newName;
            return caBuilder.Build();
        }
            

    }

    ///<summary>
    /// DAO class for getting serialized data from xml
    ///</summary>
    [XmlRoot("CommandInfos")]
    public class XCommandInfos
    {
        private readonly ArrayList lstCommandInfos = new ArrayList();

        ///<summary>
        /// Command info array
        ///</summary>
        [XmlElement("CommandInfo")]
        public XCommandInfo[] XCommandInfo
        {
            get
            {
                var items = new XCommandInfo[lstCommandInfos.Count];
                lstCommandInfos.CopyTo(items);
                return items;

            }
            set
            {
                if (value == null) return;
                var items = (XCommandInfo[])value;
                lstCommandInfos.Clear();
                foreach (XCommandInfo item in items)
                    lstCommandInfos.Add(item);

            }
        }
    }

    ///<summary>
    /// DO class for getting serialized data from XML
    ///</summary>
    public class XCommandInfo
    {

        ///<summary>
        /// Command name
        ///</summary>
        public string Name { get; set; }

        /// <summary>
        /// Special parameter name used as id for this command
        /// </summary>
        public string NameParameter { get; set; }

        ///<summary>
        /// Command parameters
        ///</summary>
        public string[] Parameter { get; set; }
    }


}
