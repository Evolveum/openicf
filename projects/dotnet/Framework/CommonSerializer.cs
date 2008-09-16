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
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
namespace Org.IdentityConnectors.Framework.Common.Serializer
{
    /**
     * Interface for reading objects from a stream.
     */
    public interface BinaryObjectDeserializer {
        /**
         * Reads the next object from the stream. Throws 
         * a wrapped {@link EOFException} if end of stream is reached.
         * @return The next object from the stream.
         */
        object ReadObject();
        
        /**
         * Closes the underlying stream
         */
        void Close();
    }
    /**
     * Interface for writing objects to a stream.
     */
    public interface BinaryObjectSerializer {
        /**
         * Writes the next object to the stream.
         * @param obj The object to write.
         * @see ObjectSerializerFactory for a list of supported types.
         */
        void WriteObject(object obj);
        
        /**
         * Flushes the underlying stream.
         */
        void Flush();
        
        /**
         * Closes the underylying stream after first flushing it.
         */
        void Close();
    }

    /**
     * Serializer factory for serializing connector objects. The list of
     * supported types are as follows:
     * TODO: list supported types
     * <ul>
     * </ul>
     * @see SerializerUtil
     */
    public abstract class ObjectSerializerFactory {
        // At some point we might make this pluggable, but for now, hard-code
        private const string IMPL_NAME = "Org.IdentityConnectors.Framework.Impl.Serializer.ObjectSerializerFactoryImpl";
    
        private static ObjectSerializerFactory _instance;
        
        private static readonly Object LOCK = new Object();

                           
        /**
         * Get the singleton instance of the {@link ObjectSerializerFactory}.
         */
        public static ObjectSerializerFactory GetInstance() {
            lock (LOCK) {
                if (_instance == null) {
                    SafeType<ObjectSerializerFactory> t = 
                        FrameworkInternalBridge.LoadType<ObjectSerializerFactory>(IMPL_NAME);
                  
                    _instance = t.CreateInstance();
                }
                return _instance;
            }
        }
    
        /**
         * Creates a <code>BinaryObjectSerializer</code> for writing objects to
         * the given stream.
         * 
         * NOTE: consider using {@link SerializerUtil#serializeBinaryObject(Object)}
         * for convenience serializing a single object.
         *  
         * NOTE2: do not mix and match {@link SerializerUtil#serializeBinaryObject(Object)}
         * with {{@link #newBinaryDeserializer(InputStream)}. This is unsafe since there
         * is header information and state associated with the stream. Objects written
         * using one method must be read using the proper corresponding method.
         * 
         * @param os The stream
         * @return The serializer
         */
        public abstract BinaryObjectSerializer NewBinarySerializer(Stream os);
        
        /**
         * Creates a <code>BinaryObjectDeserializer</code> for reading objects from
         * the given stream.
         * 
         * NOTE: consider using {@link SerializerUtil#deserializeBinaryObject(byte[])}
         * for convenience deserializing a single object.
         * 
         * NOTE2: do not mix and match {@link SerializerUtil#deserializeBinaryObject(Object)}
         * with {{@link #newBinarySerializer(OutputStream)}. This is unsafe since there
         * is header information and state associated with the stream. Objects written
         * using one method must be read using the proper corresponding method.
         *
         * @param os The stream
         * @return The deserializer
         */
        public abstract BinaryObjectDeserializer NewBinaryDeserializer(Stream i);
        
        /**
         * Creates a <code>BinaryObjectSerializer</code> for writing objects to
         * the given stream. 
         * 
         * NOTE: consider using {@link SerializerUtil#serializeXmlObject(Object,boolean)}
         * for convenience serializing a single object.
         *  
         * NOTE2: do not mix and match {@link SerializerUtil#serializeXmlObject(Object,boolean)}
         * with {{@link #deserializeXmlStream(InputSource, XmlObjectResultsHandler, boolean)}. 
         * 
         * @param w The writer
         * @param includeHeader True to include the xml header
         * @param multiObject Is this to produce a multi-object document. If false, only
         * a single object may be written.
         * @return The serializer
         */
        public abstract XmlObjectSerializer NewXmlSerializer(TextWriter w, 
                bool includeHeader,
                bool multiObject);
    
        /**
         * Deserializes XML objects from a stream
         * 
         * NOTE: Consider using {@link SerializerUtil#deserializeXmlObject(String,boolean)}
         * for convenience deserializing a single object.
         * 
         * NOTE2: Do not mix and match {@link SerializerUtil#deserializeXmlObject(Object,boolean)}
         * with {{@link #newXmlSerializer(Writer, boolean, boolean)}. 
         *
         * @param is The input source
         * @param handler The callback to receive objects from the stream
         * @param validate True iff we are to validate
         */
        public abstract void DeserializeXmlStream(TextReader reader, 
                XmlObjectResultsHandler handler,
                bool validate);
    
    }

    /**
     * Bag of utilities for serialization
     */
    public static class SerializerUtil {
    
        
        /**
         * Serializes the given object to bytes
         * @param object The object to serialize
         * @return The bytes
         * @see ObjectSerializerFactory for a list of supported types
         */
        public static byte [] SerializeBinaryObject(object obj) {
            ObjectSerializerFactory fact = ObjectSerializerFactory.GetInstance();
            MemoryStream mem = new MemoryStream();
            BinaryObjectSerializer ser = fact.NewBinarySerializer(mem);
            ser.WriteObject(obj);
            ser.Close();
            return mem.ToArray();
        }
    
        /**
         * Deserializes the given object from bytes
         * @param bytes The bytes to deserialize
         * @return The object
         * @see ObjectSerializerFactory for a list of supported types
         */
        public static object DeserializeBinaryObject(byte [] bytes) {
            ObjectSerializerFactory fact = ObjectSerializerFactory.GetInstance();
            MemoryStream mem = new MemoryStream(bytes);
            BinaryObjectDeserializer des = fact.NewBinaryDeserializer(mem);
            return des.ReadObject();
        }
        
        /**
         * Serializes the given object to xml
         * @param object The object to serialize
         * @param includeHeader True if we are to include the xml header.
         * @return The xml
         * @see ObjectSerializerFactory for a list of supported types
         */
        public static String SerializeXmlObject(Object obj, bool includeHeader) {
            ObjectSerializerFactory fact = ObjectSerializerFactory.GetInstance();
            StringWriter w = new StringWriter();
            XmlObjectSerializer ser = fact.NewXmlSerializer(w, includeHeader, false);
            ser.WriteObject(obj);
            ser.Close(true);
            return w.ToString();
        }
    
        /**
         * Deserializes the given object from xml
         * @param bytes The xml to deserialize
         * @param validate True if we are to validate the xml
         * @return The object
         * @see ObjectSerializerFactory for a list of supported types
         */
        public static Object DeserializeXmlObject(String str, bool validate) {
            ObjectSerializerFactory fact = ObjectSerializerFactory.GetInstance();
            StringReader source = new StringReader(str);
            IList<Object> rv = new List<Object>();
            fact.DeserializeXmlStream(source, 
                    obj => {
                    rv.Add(obj);
                    return true;
            },validate);
            if ( rv.Count > 0 ) {
                return rv[0];
            }
            else {
                return null;
            }
        }
        
        /**
         * Clones the given object by serializing it to bytes and then
         * deserializing it.
         * @param object The object.
         * @return A clone of the object
         */
        public static object CloneObject(Object obj) {
            byte [] bytes = SerializeBinaryObject(obj);
            return DeserializeBinaryObject(bytes);
        }
    
    }
    
    /**
     * Callback interface to receive xml objects from a stream of objects.
     */
    public delegate bool XmlObjectResultsHandler(Object obj); 
    
    /**
     * Interface for writing objects to a stream.
     */
    public interface XmlObjectSerializer {
        /**
         * Writes the next object to the stream. 
         * @param object The object to write.
         * @see ObjectSerializerFactory for a list of supported types.
         * @throws ConnectorException if there is more than one object
         * and this is not configured for multi-object document. 
         */
        void WriteObject(Object obj);
        
        /**
         * Flushes the underlying stream. 
         */
        void Flush();
        
        /**
         * Adds document end tag and optinally closes the underlying stream
         */
        void Close(bool closeUnderlyingStream);
    }


}
