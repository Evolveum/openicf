/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Xml;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Api;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Common.Objects;
using Org.IdentityConnectors.Framework.Common.Objects.Filters;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Spi.Operations;
using Org.IdentityConnectors.Test.Common.Spi;

namespace Org.IdentityConnectors.Test.Common
{    
    /// <summary>    
    /// <see cref="ResultsHandler"/> which stores all connector objects into
    /// list retrievable with <see cref="Objects"/>.
    /// </summary>
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

    /// <summary>
    /// Bag of utility methods useful to connector tests.
    /// </summary>
    public sealed class TestHelpers {
        
        private TestHelpers() {
        }
        
        /**
         * Method for convenient testing of local connectors. 
         */
        public static APIConfiguration CreateTestConfiguration(SafeType<Connector> clazz,
                Configuration config) {
            return GetSpi().CreateTestConfiguration(clazz, config);
        }
        
        /**
         * Creates an dummy message catalog ideal for unit testing.
         * All messages are formatted as follows:
         * <p>
         * <code><i>message-key</i>: <i>arg0.toString()</i>, ..., <i>argn.toString</i></code>
         * @return A dummy message catalog.
         */
        public static ConnectorMessages CreateDummyMessages() {
            return GetSpi().CreateDummyMessages();
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
            GetSpi().Search(search, oclass, filter, handler, options);
        }
                
        //At some point we might make this pluggable, but for now, hard-code
        private const String IMPL_NAME =
            "Org.IdentityConnectors.Framework.Impl.Test.TestHelpersImpl";
        private static readonly object LOCK = new object();
        private static TestHelpersSpi _instance;
        
        /**
         * Returns the instance of this factory.
         * @return The instance of this factory
         */
        private static TestHelpersSpi GetSpi() {
            lock(LOCK) {
                if (_instance == null) {
                    SafeType<TestHelpersSpi> type = FrameworkInternalBridge.LoadType<TestHelpersSpi>(IMPL_NAME);
                    _instance = type.CreateInstance();
                }
                return _instance;
            }
        }
    
        private static IDictionary<string, string> _properties = null;
        private static readonly string PREFIX = Environment.GetEnvironmentVariable("USERPROFILE") + "/.connectors/";
        public static readonly string GLOBAL_PROPS = "connectors.xml";
        
        [MethodImpl(MethodImplOptions.NoInlining)]
        public static string GetProperty(string key, string def) {
        	Assembly asm = Assembly.GetCallingAssembly();
        	return CollectionUtil.GetValue(GetProperties(asm), key, def);
        }
        
        private static IDictionary<string, string> GetProperties(Assembly asm) {
            lock(LOCK) {
                if (_properties == null) {
                    _properties = LoadProperties(asm);
                }
            }
            // create a new instance so its not mutable
            return CollectionUtil.NewReadOnlyDictionary(_properties);
        }
        
        private static IDictionary<string, string> LoadProperties(Assembly asm) {
            const string ERR = "TestHelpers: Unable to load optional XML properties file \"{0}\"";            
            string fn = null;
            IDictionary<string, string> props = null;
            IDictionary<string, string> ret = new Dictionary<string, string>();
            
            //load global properties file
            try {
            	fn = Path.Combine(PREFIX, GLOBAL_PROPS);
            	props = LoadPropertiesFile(fn);
            	CollectionUtil.AddOrReplaceAll(ret, props);
            } catch (Exception) {
            	Trace.TraceInformation(ERR, fn);        	
            }       
            
            // load the project properties file
            try {    
                fn = Path.Combine(Environment.CurrentDirectory, "project.xml");
                props = LoadPropertiesFile(fn);
                CollectionUtil.AddAll(ret, props);
            } catch (Exception) {
                Trace.TraceInformation(ERR, fn);
            }
            
            // private settings are in the "assembly name" folder, as defined in the assembly
            string prjName = asm.GetName().Name;
            if (!StringUtil.IsBlank(prjName)) {           
				//load private project properties file
                try {
					fn = Path.Combine(PREFIX, prjName + "/project.xml");
                    props = LoadPropertiesFile(fn);
                    CollectionUtil.AddOrReplaceAll(ret, props);
                } catch (IOException) {
                    Trace.TraceInformation(ERR, fn);
                }
				
               	string cfg = Environment.GetEnvironmentVariable("configuration");
               	if(!StringUtil.IsBlank(cfg)) {
               		 try {
                        // load a config-specific properties file
                        fn = Path.Combine(PREFIX, prjName + "/" + cfg + "/project.xml");
                        props = LoadPropertiesFile(fn);
                        CollectionUtil.AddOrReplaceAll(ret, props);
                    } catch (IOException) {
                        Trace.TraceInformation(ERR, fn);
                    }
               	}           
            } else {
            	TraceUtil.TraceException("Could not infer assembly name.", new Exception());
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
