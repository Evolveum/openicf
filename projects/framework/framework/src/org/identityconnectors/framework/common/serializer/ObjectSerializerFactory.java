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
package org.identityconnectors.framework.common.serializer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.xml.sax.InputSource;


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
    private static final String IMPL_NAME = "org.identityconnectors.framework.impl.serializer.ObjectSerializerFactoryImpl";

    private static ObjectSerializerFactory _instance;
    
    /**
     * Get the singleton instance of the {@link ObjectSerializerFactory}.
     */
    public static synchronized ObjectSerializerFactory getInstance() {
        if (_instance == null) {
            try {
                Class<?> clazz = Class.forName(IMPL_NAME);
                Object object = clazz.newInstance();
                _instance = ObjectSerializerFactory.class.cast(object);
            } catch (Exception e) {
                throw ConnectorException.wrap(e);
            }
        }
        return _instance;
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
    public abstract BinaryObjectSerializer newBinarySerializer(OutputStream os);
    
    /**
     * Creates a <code>BinaryObjectDeserializer</code> for reading objects from
     * the given stream.
     * 
     * NOTE: Consider using {@link SerializerUtil#deserializeBinaryObject(byte[])}
     * for convenience deserializing a single object.
     * 
     * NOTE2: Do not mix and match {@link SerializerUtil#deserializeBinaryObject(byte[])}
     * with {{@link #newBinarySerializer(OutputStream)}. This is unsafe since there
     * is header information and state associated with the stream. Objects written
     * using one method must be read using the proper corresponding method.
     *
     * @param is The stream
     * @return The deserializer
     */
    public abstract BinaryObjectDeserializer newBinaryDeserializer(InputStream is);
    
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
    public abstract XmlObjectSerializer newXmlSerializer(Writer w, 
            boolean includeHeader,
            boolean multiObject);

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
    public abstract void deserializeXmlStream(InputSource is, 
            XmlObjectResultsHandler handler,
            boolean validate);

}
