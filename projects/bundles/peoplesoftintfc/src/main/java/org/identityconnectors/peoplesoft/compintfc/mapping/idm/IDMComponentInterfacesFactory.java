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

package org.identityconnectors.peoplesoft.compintfc.mapping.idm;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.*;
import javax.xml.bind.*;
import javax.xml.parsers.*;
import javax.xml.transform.sax.*;
import javax.xml.validation.*;

import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.peoplesoft.compintfc.*;
import org.identityconnectors.peoplesoft.compintfc.mapping.*;
import org.identityconnectors.peoplesoft.compintfc.mapping.idm.jaxbgen.*;
import org.xml.sax.*;

/**
 * @author kitko
 *
 */
public final class IDMComponentInterfacesFactory implements ComponentInterfacesFactory {

    
    @SuppressWarnings("unchecked")
    public ComponentInterfaces createMapping(PeopleSoftCompIntfcConfiguration cfg) {
        final URL dtdURL = getClass().getResource("/waveset.dtd");
        if(dtdURL == null){
            throw new IllegalStateException("Cannot load waveset.dtd");
        }
        final URL schemaURL = getClass().getResource("/PeopleSoftComponentInterfaces.xsd");
        if(schemaURL == null){
            throw new IllegalStateException("Cannot load PeopleSoftComponentInterfaces.xsd");
        }
        final String xml = cfg.getXMLMapping();
        try{
            JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(schemaURL);
            unmarshaller.setSchema(schema);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware( true );
            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            xmlReader.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    if("waveset.dtd".equals(publicId)){
                        return new InputSource(dtdURL.openStream());
                    }
                    return null;
                }
            });
            unmarshaller.setEventHandler(new ValidationEventHandler() {
                public boolean handleEvent(ValidationEvent event) {
                    if(event.getMessage().contains("cvc-complex-type.3.2.2")){
                        return true;
                    }
                    return false;
                }
            });
            xmlReader.setContentHandler( unmarshallerHandler );
            JAXBElement<WavesetType> res = (JAXBElement<WavesetType>) unmarshaller.unmarshal(new SAXSource(xmlReader, new InputSource(new StringReader(xml))));
            WavesetType root = WavesetType.class.cast(res.getValue());
            return parseWaveset(root);
        }
        catch(Exception e){
            throw new ConnectorException("Error parsing interface mapping using jaxb", e);
        }
    }
    
    private ComponentInterfaces parseWaveset(WavesetType root){
        List<AttributeType> interfacesAttr = root.getConfiguration().getExtension().getObject().getAttribute();
        if(interfacesAttr.size() != 1){
            throw new IllegalStateException("Must provide just one interfaces element in idm xml mapping");
        }
        ComponentInterfaces.Builder builder = new ComponentInterfaces.Builder();
        for(ObjectType iface : interfacesAttr.get(0).getList().getObject()){
            ComponentInterface componentInterface = parseInterface(iface);
            builder.registerInterface(iface.getName(), componentInterface);
        }
        
        return builder.build();
    }
    
    private ComponentInterface parseInterface(ObjectType iface) {
        ComponentInterface.Builder builder = new ComponentInterface.Builder();
        for(AttributeType attr : iface.getAttribute()){
            parseInterfaceAttribute(builder, attr);
        }
        return builder.build();
    }

    private void parseInterfaceAttribute(ComponentInterface.Builder builder, AttributeType attr) {
       if("componentInterface".equals(attr.getName())){
           builder.setInterfaceName(attr.getValue());
       }
       else if("getKey".equals(attr.getName())){
           builder.setGetKey(attr.getValue());
       }
       else if("findKey".equals(attr.getName())){
           builder.setFindKey(attr.getValue());
       }
       else if("createKey".equals(attr.getName())){
           builder.setCreateKey(attr.getValue());
       }
       else if("disableRule".equals(attr.getName())){
           DisableRule disableRule = parseDisableRule(attr);
           builder.setDisableRule(disableRule);
       }
       else if("properties".equals(attr.getName())){
           List<Property> properties = parseProperties(attr);
           builder.addProperties(properties);
       }
       else if("supportedObjectTypes".equals(attr.getName())){
           SupportedObjectTypes supportedObjectTypes = parseSupportedObjectTypes(attr);
           builder.setSupportedObjectTypes(supportedObjectTypes);
       }
       else{
           throw new IllegalArgumentException("Cannot parse [" + attr.getName() + "] attribute");
       }
    }

    private SupportedObjectTypes parseSupportedObjectTypes(AttributeType attr) {
        SupportedObjectTypes.Builder builder = new SupportedObjectTypes.Builder();
        for(ObjectType obj : attr.getList().getObject()){
            SupportedObjectTypes.Feautures.Builder featuresBuilder = new SupportedObjectTypes.Feautures.Builder(); 
            //Here parse feature
            if(obj.getAttribute().size() != 1){
                throw new IllegalStateException("Must provide just one attribute named 'features' for " + obj.getName());
            }
            AttributeType features = obj.getAttribute().get(0);
            if(!"features".equals(features.getName())){
                throw new IllegalStateException("Must provide just one attribute named 'features' for " + obj.getName() + " not " + features.getName());
            }
            for(ObjectType feature : features.getList().getObject()){
                featuresBuilder.addFeature(feature.getName());
            }
            builder.addFeautures(obj.getName(), featuresBuilder.build());
        }
        return builder.build();
    }

    private List<Property> parseProperties(AttributeType attr) {
        List<Property> res = new ArrayList<Property>();
        for(ObjectType object : attr.getList().getObject()){
            Property property = null;
            if(object.getAttribute().isEmpty()){
                property = new SingleProperty(object.getName());
            }
            else{
                CollectionProperty.Builder iBuilder = new CollectionProperty.Builder();
                iBuilder.setName(object.getName());
                boolean isCollectionSet = false;
                for(AttributeType iAttr : object.getAttribute()){
                   if("key".equals(iAttr.getName())){
                       iBuilder.setKey(iAttr.getValue());
                   }
                   else if("properties".equals(iAttr.getName())){
                       List<Property> iProperties = parseProperties(iAttr);
                       iBuilder.addProperties(iProperties);
                   }
                   else if("isCollection".equals(iAttr.getName()) && "true".equals(iAttr.getValue())){
                       isCollectionSet = true;
                   }
                }
                if(!isCollectionSet){
                    throw new IllegalStateException("isCollection not set for " + iBuilder.getName());
                }
                property = iBuilder.build();
            }
            res.add(property);
        }
        return res;
        
    }

    private DisableRule parseDisableRule(AttributeType attr) {
        DisableRule.Builder disableRuleBuilder = new DisableRule.Builder();
        for(AttributeType iAttr : attr.getObject().getAttribute()){
            if("property".equals(iAttr.getName())){
                disableRuleBuilder.setName(iAttr.getValue());
            }
            else if("trueValue".equals(iAttr.getName())){
                disableRuleBuilder.setTrueValue(iAttr.getValue());
            }
            else if("falseValue".equals(iAttr.getName())){
                disableRuleBuilder.setFalseValue(iAttr.getValue());
            }
        }
        return disableRuleBuilder.build();
    }
}
