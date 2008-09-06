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
using System.Reflection;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Common.Security;
using Org.IdentityConnectors.Framework.Spi;
using Org.IdentityConnectors.Framework.Api.Operations;
using Org.IdentityConnectors.Framework.Spi.Operations;
using Org.IdentityConnectors.Framework.Common.Objects;
namespace Org.IdentityConnectors.Framework.Common
{
    internal static class FrameworkInternalBridge {
        private static readonly Object LOCK = new Object();
        private static Assembly _assembly = null;
        /// <summary>
        /// Loads a class from the FrameworkInternal module
        /// </summary>
        /// <param name="typeName"></param>
        /// <returns></returns>
        public static Type LoadType(String typeName) {
            
            Assembly assembly;
            lock(LOCK) {                
                if (_assembly == null) {
                    AssemblyName assemName = new AssemblyName();
                    assemName.Name = "FrameworkInternal";
                    _assembly = Assembly.Load(assemName);
                }
                assembly = _assembly;
            }
            
            return assembly.GetType(typeName,true);
            
        }
    }
    
    public static class FrameworkUtil {
        private static readonly IDictionary<Type,Type> SPI_TO_API;
        private static readonly ICollection<Type> CONFIG_SUPPORTED_TYPES;
        private static readonly ICollection<Type> ATTR_SUPPORTED_TYPES;

        static FrameworkUtil() {
            IDictionary<Type,Type> temp =
                new Dictionary<Type,Type>();
            temp[typeof(AuthenticateOp)]=typeof(AuthenticationApiOp);
            temp[typeof(CreateOp)]=typeof(CreateApiOp);
            temp[typeof(DeleteOp)]=typeof(DeleteApiOp);
            temp[typeof(SearchOp<>)]=typeof(SearchApiOp);
            temp[typeof(UpdateOp)]=typeof(UpdateApiOp);
            temp[typeof(AdvancedUpdateOp)]=typeof(UpdateApiOp);
            temp[typeof(SchemaOp)]=typeof(SchemaApiOp);
            temp[typeof(TestOp)]=typeof(TestApiOp);
            temp[typeof(ScriptOnConnectorOp)]=typeof(ScriptOnConnectorApiOp);
            temp[typeof(ScriptOnResourceOp)]=typeof(ScriptOnResourceApiOp);
            temp[typeof(SyncOp)]=typeof(SyncApiOp);
            SPI_TO_API = CollectionUtil.NewReadOnlyDictionary(temp);
            
            CONFIG_SUPPORTED_TYPES = CollectionUtil.NewReadOnlySet<Type>
            ( 
                typeof(string),
                typeof(long),
                typeof(long?),
                typeof(char),
                typeof(char?),
                typeof(double),
                typeof(double?),
                typeof(float),
                typeof(float?),
                typeof(int),
                typeof(int?),
                typeof(bool),
                typeof(bool?),
                typeof(Uri),
                typeof(FileName),
                typeof(GuardedString)
            );
            ATTR_SUPPORTED_TYPES = CollectionUtil.NewReadOnlySet<Type>
            ( 
                typeof(string),
                typeof(long),
                typeof(long?),
                typeof(char),
                typeof(char?),
                typeof(double),
                typeof(double?),
                typeof(float),
                typeof(float?),
                typeof(int),
                typeof(int?),
                typeof(bool),
                typeof(bool?),
                typeof(byte[]),
                typeof(BigDecimal),
                typeof(BigInteger),
                typeof(GuardedString)
            );
    
        }
        
        
        /**
         * Determines if the class is a supported attribute type. If not it throws
         * an {@link IllegalArgumentException}.
         * 
         * <ul>
         * <li>string</li>
         * <li>long</li>
         * <li>long?</li>
         * <li>char</li>
         * <li>char?</li>
         * <li>double</li>
         * <li>double?</li>
         * <li>float</li>
         * <li>float?</li>
         * <li>int</li>
         * <li>int?</li>
         * <li>bool</li>
         * <li>bool?</li>
         * <li>byte[]</li>
         * <li>BigDecimal</li>
         * <li>BigInteger</li>
         * </ul>
         * 
         * @param clazz
         *            type to check against the support list of types.
         * @throws IllegalArgumentException
         *             iff the type is not on the supported list.
         */
        public static void CheckAttributeType(Type type) {
            if (!FrameworkUtil.IsSupportedAttributeType(type)) {
                String MSG = "Attribute type ''"+type+"'' is not supported.";
                throw new ArgumentException(MSG);
            }
        }
        public static void CheckAttributeValue(Object value) {
            if ( value != null ) {
                CheckAttributeType(value.GetType());
            }
        }
        public static ICollection<Type> Spi2Apis(Type type) {
            type = ReflectionUtil.GetTypeErasure(type);
            AssertSpiOperation(type);     
            HashSet<Type> set = new HashSet<Type>();
            set.Add(SPI_TO_API[type]);
            // add GetApiOp if search is available..
            
            if (type.Equals(typeof(SearchOp<>))) {
                set.Add(typeof(GetApiOp));
            }
            return set;
        }
        public static ICollection<Type> AllSPIOperations() {
            return SPI_TO_API.Keys;
        }
        public static ICollection<Type> AllAPIOperations() {
            ICollection<Type> set = new HashSet<Type>();
            CollectionUtil.AddAll(set,
                                  SPI_TO_API.Values);
            // add Get because it doesn't have a corresponding SPI.
            set.Add(typeof(GetApiOp));
            set.Add(typeof(ValidateApiOp));
            return CollectionUtil.AsReadOnlySet(set);
        }
        public static ICollection<Type> GetDefaultSupportedOperations(Type connector) {
            AssertConnectorType(connector);
            ICollection<Type> rv = new HashSet<Type>();
            ICollection<Type> interfaces = ReflectionUtil.GetTypeErasure(ReflectionUtil.GetAllInterfaces(connector));
            foreach (Type spi in AllSPIOperations()) {
                if (interfaces.Contains(spi)) {                    
                    CollectionUtil.AddAll(rv,Spi2Apis(spi));
                }
            }
            //finally add unconditionally supported ops
            CollectionUtil.AddAll(rv,GetUnconditionallySupportedOperations());
            return CollectionUtil.AsReadOnlySet(rv);
        }
        public static ICollection<Type> GetUnconditionallySupportedOperations() {
            HashSet<Type> ret;
            ret = new HashSet<Type>();
            //add validate api op always
            ret.Add(typeof(ValidateApiOp));
            //add ScriptOnConnectorApiOp always
            ret.Add(typeof(ScriptOnConnectorApiOp));
            return ret;        
        }
        public static ICollection<Type> GetAllSupportedConfigTypes() {
            return CONFIG_SUPPORTED_TYPES;
        }
        public static bool IsSupportedConfigurationType (Type type) {
            if ( type.IsArray) {
                return IsSupportedConfigurationType(type.GetElementType());
            }
            else {
                return CONFIG_SUPPORTED_TYPES.Contains(type);
            }
        }
        public static ICollection<Type> GetAllSupportedAttributeTypes() {
            return ATTR_SUPPORTED_TYPES;
        }
        public static bool IsSupportedAttributeType(Type clazz) {
            return ATTR_SUPPORTED_TYPES.Contains(clazz);
        }
        
        public static void AssertConnectorType(Type connector) {
            Type connectorInter = typeof(Connector);
            if (!connectorInter.IsAssignableFrom(connector)) {
                throw new ArgumentException(connector+" does not implement IConnector");
            }
        }
        public static void AssertSpiOperation(Type operation) {
            Type spiInter = typeof(SPIOperation);
            if (!spiInter.IsAssignableFrom(operation)) {
                throw new ArgumentException(operation+" does not implement ISPIOperation");
            }
        }
        public static void AssertApiOperation(Type operation) {
            Type spiInter = typeof(APIOperation);
            if (!spiInter.IsAssignableFrom(operation)) {
                throw new ArgumentException(operation+" does not implement APIOperation");
            }
        }
        /**
         * Determines if the class is a supported type for an OperationOption. If not it throws
         * an {@link IllegalArgumentException}.
         * 
         * @param clazz
         *            type to check against the support list of types.
         * @throws IllegalArgumentException
         *             iff the type is not on the supported list.
         */
        public static void CheckOperationOptionType(Type clazz) {
            //the set of supported operation option types
            //is the same as that for configuration beans
            if (!FrameworkUtil.IsSupportedConfigurationType(clazz)) {
                String MSG = "ConfigurationOption type '+"+clazz.Name+"+' is not supported.";
                throw new ArgumentException(MSG);
            }
        }
        /**
         * Determines if the class of the object is a supported attribute type.
         * If not it throws an {@link IllegalArgumentException}.
         * @param value The value to check or null.
         */
        public static void CheckOperationOptionValue(Object val) {
            if ( val != null ) {
                CheckOperationOptionType(val.GetType());
            }
        }
    }
}
