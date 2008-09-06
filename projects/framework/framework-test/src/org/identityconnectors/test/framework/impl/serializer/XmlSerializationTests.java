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
package org.identityconnectors.test.framework.impl.serializer;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.identityconnectors.framework.common.serializer.ObjectSerializerFactory;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.identityconnectors.framework.common.serializer.XmlObjectResultsHandler;
import org.identityconnectors.framework.common.serializer.XmlObjectSerializer;
import org.junit.Test;
import org.xml.sax.InputSource;


public class XmlSerializationTests extends ObjectSerializationTests {
    @Override
    protected Object cloneObject(Object o) {
        String xml = SerializerUtil.serializeXmlObject(o, true);
        System.out.println(xml);
        o = SerializerUtil.deserializeXmlObject(xml, true); 
        
        //pass through a list to make sure dtd correctly defines all xml objects
        List<Object> list = new ArrayList<Object>();
        list.add(o);
        xml = SerializerUtil.serializeXmlObject(list, true);
        System.out.println(xml);
        @SuppressWarnings("unchecked")
        List<Object> rv = (List<Object>)SerializerUtil.deserializeXmlObject(xml, true); 
        return rv.get(0);
    }
    
    @Test
    public void testMultiObject() throws Exception {
        ObjectSerializerFactory factory = ObjectSerializerFactory.getInstance();
        StringWriter sw = new StringWriter();
        XmlObjectSerializer ser = factory.newXmlSerializer(sw, true, true);
        ser.writeObject("foo");
        ser.writeObject("bar");
        ser.close(true);
        String xml = sw.toString();
        System.out.println(xml);
        final List<Object> results = new ArrayList<Object>();
        factory.deserializeXmlStream(new InputSource(new StringReader(xml)), 
                new XmlObjectResultsHandler() {
                public boolean handle(Object o) {
                    results.add(o);
                    return true;
                }},
                true); 
        Assert.assertEquals(2,results.size());
        Assert.assertEquals("foo",results.get(0));
        Assert.assertEquals("bar",results.get(1));
    }
}
