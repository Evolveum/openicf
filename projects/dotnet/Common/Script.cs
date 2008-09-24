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
using System.Reflection;
using System.Collections.Generic;
using System.Diagnostics;

namespace Org.IdentityConnectors.Common.Script
{
    public interface ScriptExecutor : IDisposable  {
        /// <summary>
        /// Executes the script with the given arguments.
        /// </summary>
        /// <param name="arguments">key/value set of variables to 
        /// pass to the script.</param>
        /// <returns></returns>
        object Execute(IDictionary<string,object> arguments);
    }
    public abstract class ScriptExecutorFactory {
        
        private static readonly object LOCK = new object();
        
        /// <summary>
        /// Loaded w/ all supported languages.
        /// </summary>
        private static IDictionary<string, Type> _supportedLanguages = null;
        
        /// <summary>
        /// Load all script executor factory assemblies in the same directory
        /// the 'Common' assembly.
        /// </summary>
        private static IDictionary<string, Type> LoadSupportedLanguages() {
            // attempt to process all assemblies..
            IDictionary<string, Type> ret = new Dictionary<string,Type>();
            Assembly assembly = Assembly.GetExecutingAssembly();
            FileInfo thisAssemblyFile = new FileInfo(assembly.Location);
            DirectoryInfo directory = thisAssemblyFile.Directory;
            // get all *ScriptExecutorFactory assmebly from the current directory
            FileInfo [] files = directory.GetFiles("*.ScriptExecutorFactory.dll");
            Type t = typeof(ScriptExecutorFactoryClassAttribute);
            foreach (FileInfo file in files) {
                try {
                    Assembly lib = Assembly.LoadFrom(file.ToString());            
                    foreach (Type type in lib.GetTypes()) {
                        Object [] attributes = type.GetCustomAttributes(t, false);
                        if ( attributes.Length > 0 ) {
                            ScriptExecutorFactoryClassAttribute attribute = 
                                (ScriptExecutorFactoryClassAttribute)attributes[0];
                            // attempt to test assembly..
                            Activator.CreateInstance(type);
                            // if we made it this far its okay
                            ret[attribute.Language.ToUpper()] = type;
                        }
                    }
                }
                catch (Exception e) {
                    TraceUtil.TraceException("Unable to load assembly: "+
                        assembly.FullName+". This is a fatal exception: ",e);
                    throw;
                }
            }
            return ret;
        }
        
        private static IDictionary<string, Type> GetSupportedLanguages()
        {
            lock (LOCK)
            {
                if (_supportedLanguages == null)
                {
                    _supportedLanguages = LoadSupportedLanguages();
                }
                return _supportedLanguages;
            }  
        }
        
        /**
         * Returns the set of supported languages.
         * @return The set of supported languages.
         */
        public static ICollection<String> SupportedLanguages 
        {
            get 
            {
                IDictionary<string, Type> map =
                    GetSupportedLanguages();
                return CollectionUtil.AsReadOnlySet(map.Keys);
            }
        }
        
        /**
         * Creates a ScriptExecutorFactory for the given language
         * @param language The name of the language
         * @return The script executor factory
         * @throws IllegalArgumentException If the given language is not
         *  supported.
         */
        public static ScriptExecutorFactory NewInstance(String language) {
            if ( language == null ) {
                throw new ArgumentException("Language must be specified");
            }
            Type type = CollectionUtil.GetValue(GetSupportedLanguages(),language.ToUpper(),null);
            if ( type == null ) {
                throw new ArgumentException("Language not supported: "+language);
            }
            return (ScriptExecutorFactory) Activator.CreateInstance(type);
        }
    
        
        /**
         * Creates a script executor for the given script.
         * @param loader The classloader that contains the java classes
         * that the script should have access to.
         * @param script The script text.
         * @param compile A hint to tell the script executor whether or
         * not to compile the given script. This need not be implemented
         * by all script executors. If true, the caller is saying that
         * they intend to call the script multiple times with different
         * arguments, so compile if possible.
         * @return A script executor.
         */
        public abstract ScriptExecutor NewScriptExecutor(
                Assembly [] referencedAssemblies,
                String script,
                bool compile);
    }
    
    [AttributeUsage(AttributeTargets.Class, AllowMultiple=false)]
    public class ScriptExecutorFactoryClassAttribute : System.Attribute {
        private readonly string _lang;
        
        /// <summary>
        /// Determine the language supported by the factory.
        /// </summary>
        /// <param name="lang"></param>
        public ScriptExecutorFactoryClassAttribute(string lang) {
            _lang = lang;
        }
                    
        public string Language {
            get {
                return _lang;
            }
        }
    }
}
