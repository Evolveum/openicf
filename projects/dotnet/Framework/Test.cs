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

using System.IO;
using System.Xml;
using System.Collections;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Api;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;

namespace Org.IdentityConnectors.Framework.Test
{
    public sealed class ToListResultsHandler {
        private IList<ConnectorObject> _objects 
            = new List<ConnectorObject>();
        public bool Handle(ConnectorObject obj) {
            _objects.Add(obj);
            return true;
        }
        
        public IList<ConnectorObject> Objects {
            get {
                return _objects;
            }
        }
    }
    public abstract class TestHelpers {
        
        /**
         * Method for convenient testing of local connectors. 
         */
        public static APIConfiguration CreateTestConfiguration(SafeType<Connector> clazz,
                Configuration config) {
            return GetInstance().CreateTestConfigurationImpl(clazz, config);
        }
        
            
        public static IList<ConnectorObject> SearchToList(SearchApiOp search, 
                ObjectClass oclass, 
                Filter filter) {
            return SearchToList(search, oclass, filter, null);
        }
        
        public static IList<ConnectorObject> SearchToList(SearchApiOp search, 
                ObjectClass oclass, 
                Filter filter,
                OperationOptions options) {
            ToListResultsHandler handler = new
                 ToListResultsHandler();
            search.Search(oclass,filter, handler.Handle,options);
            return handler.Objects;
        }
        /**
         * Performs a raw, unfiltered search at the SPI level,
         * eliminating duplicates from the result set.
         * @param search The search SPI
         * @param oclass The object class - passed through to
         * connector so it may be null if the connecor
         * allowing it to be null. (This is convenient for
         * unit tests, but will not be the case in general)
         * @param filter The filter to search on
         * @param options The options - may be null - will
         *  be cast to an empty OperationOptions
         * @return The list of results.
         */
        public static IList<ConnectorObject> SearchToList<T>(SearchOp<T> search, 
                ObjectClass oclass, 
                Filter filter) where T : class{
            return SearchToList(search,oclass,filter,null);
        }
        /**
         * Performs a raw, unfiltered search at the SPI level,
         * eliminating duplicates from the result set.
         * @param search The search SPI
         * @param oclass The object class - passed through to
         * connector so it may be null if the connecor
         * allowing it to be null. (This is convenient for
         * unit tests, but will not be the case in general)
         * @param filter The filter to search on
         * @param options The options - may be null - will
         *  be cast to an empty OperationOptions
         * @return The list of results.
         */
        public static IList<ConnectorObject> SearchToList<T>(SearchOp<T> search, 
                ObjectClass oclass, 
                Filter filter,
                OperationOptions options) where T : class{
            ToListResultsHandler handler = new
                 ToListResultsHandler();
            Search(search,oclass,filter, handler.Handle, options);
            return handler.Objects;
        }
        
        /**
         * Performs a raw, unfiltered search at the SPI level,
         * eliminating duplicates from the result set.
         * @param search The search SPI
         * @param oclass The object class - passed through to
         * connector so it may be null if the connecor
         * allowing it to be null. (This is convenient for
         * unit tests, but will not be the case in general)
         * @param filter The filter to search on
         * @param handler The result handler
         * @param options The options - may be null - will
         *  be cast to an empty OperationOptions
         */
        public static void Search<T>(SearchOp<T> search,
                ObjectClass oclass, 
                Filter filter, 
                ResultsHandler handler,
                OperationOptions options) where T : class {
            GetInstance().SearchImpl(search, oclass, filter, handler, options);
        }
        
        
        //At some point we might make this pluggable, but for now, hard-code
        private const String IMPL_NAME =
            "Org.IdentityConnectors.Framework.Impl.Test.TestHelpersImpl";
        private static readonly object LOCK = new object();
        private static TestHelpers _instance;
        
        /**
         * Returns the instance of this factory.
         * @return The instance of this factory
         */
        private static TestHelpers GetInstance() {
            lock(LOCK) {
                if (_instance == null) {
                    SafeType<TestHelpers> type = FrameworkInternalBridge.LoadType<TestHelpers>(IMPL_NAME);
                    _instance = type.CreateInstance();
                }
                return _instance;
            }
        }
    
        
        abstract protected APIConfiguration CreateTestConfigurationImpl(SafeType<Connector> clazz,
                Configuration config);
        abstract protected void SearchImpl<T>(SearchOp<T> search,
                ObjectClass oclass, 
                Filter filter, 
                ResultsHandler handler,
                OperationOptions options) where T : class;
 

        private static IDictionary<string, string> _properties = null;
        public static readonly string GLOBAL_PROPS = "connectors.xml";
        
        public static string GetProperty(string key, string def) {
            return CollectionUtil.GetValue(GetProperties(), key, def);
        }
        
        private static IDictionary<string, string> GetProperties() {
            lock(LOCK) {
                if (_properties == null) {
                    _properties = LoadProperties();
                }
            }
            // create a new instance so its not mutable
            return CollectionUtil.NewReadOnlyDictionary(_properties);
        }
        
        private static IDictionary<string, string> LoadProperties() {
            const string ERR = "Unable to load optional XML properties file: ";
            string fn = null;
            IDictionary<string, string> props = null;
            IDictionary<string, string> ret = new Dictionary<string, string>();
            try {
                // load the local properties file
                fn = Path.Combine(Environment.CurrentDirectory, "project.xml");
                props = LoadPropertiesFile(fn);
                CollectionUtil.AddAll(ret, props);
            } catch (Exception e) {
                TraceUtil.TraceException(ERR + fn, e);
            }
            // global settings are prefixed w/ the project name
            string prjName = Environment.GetEnvironmentVariable("project.name");
            if (!StringUtil.IsBlank(prjName)) {
                // includes the parent configuration and the specific config.
                IList<string> configurations = CollectionUtil.NewList(new string[] {prjName});
                // determine the configuration property
                string cfg = Environment.GetEnvironmentVariable("configuration");
                if (!StringUtil.IsBlank(cfg)) {
                    string name = prjName + "-" + cfg;
                    configurations.Add(name);
                }
                // load the user properties file (project specific)
                fn = Path.Combine(
                    Environment.SpecialFolder.LocalApplicationData.ToString(),
                    GLOBAL_PROPS);
                try {
                    props = LoadPropertiesFile(fn);
                    foreach (string cfgName in configurations) {
                        string cmp = cfgName + ".";
                        foreach (string key in props.Keys) {
                            if (key.StartsWith(cmp)) {
                                String newKey = key.Substring(cmp.Length);
                                ret[newKey] = props[key];
                            }
                        }
                    }
                } catch (IOException e) {
                    TraceUtil.TraceException(ERR + fn, e);
                }
                // load the project file then the project 
                // configuration specific file
                foreach (string cfgFn in configurations) {
                    // load the user project specific file
                    try {
                        // load the local properties file
                        fn = System.IO.Path.Combine(
                            Environment.SpecialFolder.LocalApplicationData.ToString(),
                            cfgFn + ".xml");
                        props = LoadPropertiesFile(fn);
                        CollectionUtil.AddAll(ret, props);
                    } catch (IOException e) {
                        TraceUtil.TraceException(ERR + fn, e);
                    }
                }
            }
            // load the environment variables
            foreach (DictionaryEntry entry in Environment.GetEnvironmentVariables()) {
                ret[entry.Key.ToString()] = entry.Value.ToString();
            }
            return ret;
        }
        
        /// <summary>
        /// Format for the xml file is simple &lt;property name='' value=''/&gt;
        /// </summary>
        /// <param name="fn"></param>
        /// <returns></returns>
        public static IDictionary<string, string> LoadPropertiesFile(string filename) {
            IDictionary<string, string> ret = new Dictionary<string, string>();
            //Environment.
            XmlTextReader reader = null;
            try {
                // Load the reader with the data file and ignore all white space nodes.         
                reader = new XmlTextReader(filename);
                reader.WhitespaceHandling = WhitespaceHandling.None;
                // Parse the file and display each of the nodes.
                while (reader.Read()) {
                    if (reader.NodeType == XmlNodeType.Element && 
                        reader.Name.Equals("property")) {
                        string name = reader.GetAttribute("name");
                        string xmlValue = reader.GetAttribute("value");
                        if (!StringUtil.IsBlank(name) && xmlValue != null) {
                            ret[name] = xmlValue;
                        }
                    }
                }           
            } finally {
                if (reader!=null)
                  reader.Close();
            }
            return ret;
        }
    }
}
