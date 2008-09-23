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
package org.identityconnectors.dummy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.*;

@ConnectorClass(displayNameKey="DummyConnector",configurationClass=DummyConfiguration.class)
public class DummyConnector
    implements Connector, CreateOp, DeleteOp, SearchOp, UpdateOp, SchemaOp
{

    public DummyConnector()
    {
    }

    public void dispose()
    {
    }

    public Configuration getConfiguration()
    {
        return _configuration;
    }

    public void init(Configuration cfg)
    {
        _configuration = (DummyConfiguration)cfg;
        _connection = new DummyConnection();
        _schema = schema();
    }

    public Uid create(ObjectClass objClass, Set attrs, OperationOptions options)
    {
        validateAttributes(objClass, attrs, true);
        Uid uid = generateUid();
        _map.put(uid, attrs);
        return uid;
    }

    private void validateAttributes(ObjectClass oclass, Set attrs, boolean checkRequired)
    {
        ObjectClassInfo oci = _schema.findObjectClassInfo(oclass.getObjectClassValue());
        Map attrMap = new HashMap(AttributeUtil.toMap(attrs));
        for(Iterator i$ = oci.getAttributeInfo().iterator(); i$.hasNext();)
        {
            AttributeInfo attributeInfo = (AttributeInfo)i$.next();
            if(checkRequired && attributeInfo.isRequired() && !attrMap.containsKey(attributeInfo.getName()))
                throw new IllegalArgumentException((new StringBuilder()).append("Required attribute ").append(attributeInfo.getName()).append(" is missing").toString());
            if(!(attributeInfo.isCreateable() || attributeInfo.isUpdateable()) && attrMap.containsKey(attributeInfo.getName()))
                throw new IllegalArgumentException((new StringBuilder()).append("Non-writeable attribute ").append(attributeInfo.getName()).append(" is present").toString());
        }

        Map ociMap = AttributeInfoUtil.toMap(oci.getAttributeInfo());
        for(Iterator i$ = attrs.iterator(); i$.hasNext();)
        {
            Attribute attribute = (Attribute)i$.next();
            if(!ociMap.containsKey(attribute.getName()))
                throw new IllegalArgumentException((new StringBuilder()).append("Unknown attribute ").append(attribute.getName()).append(" is present").toString());
        }

    }

    public synchronized Uid generateUid()
    {
        _uidIndex++;
        return new Uid((new StringBuilder()).append(_uidIndex).append("").toString());
    }

    public void delete(ObjectClass objClass, Uid uid, OperationOptions options)
    {
        if(!_map.containsKey(uid))
        {
            throw new UnknownUidException();
        } else
        {
            _map.remove(uid);
            return;
        }
    }

    public FilterTranslator createFilterTranslator(ObjectClass oclass, OperationOptions options)
    {
        return new DummyFilterTranslator();
    }

    public void executeQuery(ObjectClass oclass, String query, ResultsHandler handler, OperationOptions options)
    {
        String attrsToGet[] = options.getAttributesToGet();
        ConnectorObjectBuilder builder;
        for(Iterator iter = _map.entrySet().iterator(); iter.hasNext(); handler.handle(builder.build()))
        {
            java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
            builder = new ConnectorObjectBuilder();
            if(attrsToGet == null)
            {
                builder.addAttributes((Collection)entry.getValue());
            } else
            {
                Map map = AttributeUtil.toMap((Collection)entry.getValue());
                String attributes[] = attrsToGet;
                int length = attributes.length;
                for(int i = 0; i < length; i++)
                {
                    String attribute = attributes[i];
                    builder.addAttribute(new Attribute[] {
                        (Attribute)map.get(attribute)
                    });
                }

            }
            builder.setUid((Uid)entry.getKey());
        }

    }

    public Uid update(ObjectClass objclass, Set attrs, OperationOptions options)
    {
        validateAttributes(objclass, attrs, false);
        Map attrMap = new HashMap(AttributeUtil.toMap(attrs));
        Uid uid = (Uid)attrMap.remove(Uid.NAME);
        if(uid == null)
            throw new RuntimeException("missing Uid");
        if(!_map.containsKey(uid))
        {
            throw new UnknownUidException();
        } else
        {
            Name name = (Name)attrMap.remove(Name.NAME);
            Attribute currentPassword = (Attribute)attrMap.remove(OperationalAttributeInfos.CURRENT_PASSWORD);
            Attribute resetPassword = (Attribute)attrMap.remove(OperationalAttributeInfos.RESET_PASSWORD);
            Set object = (Set)_map.get(uid);
            object.addAll(attrMap.values());
            return uid;
        }
    }

    public Schema schema()
    {
        return staticSchema();
    }

    public static Schema staticSchema()
    {
        if(_schema != null)
            return _schema;
        try
        {
            SchemaBuilder schemaBuilder = new SchemaBuilder(DummyConnector.class);
            Set attributes = new HashSet();
            attributes.add(OperationalAttributeInfos.CURRENT_PASSWORD);
            attributes.add(OperationalAttributeInfos.DISABLE_DATE);
            attributes.add(OperationalAttributeInfos.ENABLE);
            attributes.add(OperationalAttributeInfos.ENABLE_DATE);
            attributes.add(OperationalAttributeInfos.LOCK_OUT);
            attributes.add(OperationalAttributeInfos.PASSWORD);
            attributes.add(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
            attributes.add(OperationalAttributeInfos.RESET_PASSWORD);
            Method setters[] = getAttributeInfoBuilderSetters();
            AttributeInfoBuilder builder = new AttributeInfoBuilder();
            attributes.addAll(buildAttributeInfo(builder, setters));
            schemaBuilder.defineObjectClass("account", attributes);
            _schema = schemaBuilder.build();
            return _schema;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static List buildAttributeInfo(AttributeInfoBuilder builder, Method methods[])
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        List list = new LinkedList();
        Class supportedTypes[] = (Class[])FrameworkUtil.getAllSupportedAttributeTypes().toArray(new Class[0]);
        buildAttributeInfo(builder, methods, 0, "", list, supportedTypes, new int[] {
            0
        });
        return list;
    }

    private static void buildAttributeInfo(AttributeInfoBuilder builder, Method methods[], int index, String name, List list, Class supportedTypes[], int typeIndex[])
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        if(index < methods.length)
        {
            methods[index].invoke(builder, new Object[] {
                Boolean.valueOf(true)
            });
            // We want to make arrays more obvious, so we will use Array in the name
            //
            String t = "_T";
            if (methods[index].getName().contains("MultiValue"))
            	t = "_Array";
            buildAttributeInfo(builder, methods, index + 1, (new StringBuilder()).append(name).append(t).toString(), list, supportedTypes, typeIndex);
            methods[index].invoke(builder, new Object[] {
                Boolean.valueOf(false)
            });
            buildAttributeInfo(builder, methods, index + 1, (new StringBuilder()).append(name).append("_F").toString(), list, supportedTypes, typeIndex);
        } else
        {
            Class clazz = supportedTypes[typeIndex[0]];
            String prefix = clazz.getSimpleName();
            if(clazz.isArray())
                prefix = (new StringBuilder()).append(clazz.getComponentType().getSimpleName()).append("Array").toString();
            builder.setName((new StringBuilder()).append(prefix).append(name).toString().toUpperCase());
            builder.setType(clazz);
            AttributeInfo info = builder.build();
            if((info.isCreateable() || info.isUpdateable()) || !info.isRequired())
            	if (!(info.isRequired() && clazz.equals(byte[].class)))
	            {
	                list.add(info);
	                typeIndex[0]++;
	                if(typeIndex[0] == supportedTypes.length)
	                    typeIndex[0] = 0;
	            }
        }
    }

    private static Method[] getAttributeInfoBuilderSetters()
    {
        List setters = new LinkedList();
        Method methods[] = AttributeInfoBuilder.class.getMethods();
        int length = methods.length;
        for(int i = 0; i < length; i++)
        {
            Method method = methods[i];
            if(!method.getName().startsWith("setReturned") && method.getName().startsWith("set") && 
                method.getParameterTypes().length == 1 && 
            	(method.getParameterTypes()[0] == Boolean.TYPE || method.getParameterTypes()[0] == Boolean.class))
                setters.add(method);
        }

        return (Method[])setters.toArray(new Method[0]);
    }

    public void executeQuery(ObjectClass x0, Object x1, ResultsHandler x2, OperationOptions x3)
    {
        executeQuery(x0, (String)x1, x2, x3);
    }

    private static int _uidIndex;
    private static Schema _schema;
    private static Map _map = new HashMap();
    private DummyConfiguration _configuration;
    private DummyConnection _connection;

}
