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

package org.identityconnectors.peoplesoft.mapping.idm;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.identityconnectors.peoplesoft.mapping.*;
import org.identityconnectors.peoplesoft.mapping.idm.IDMHandler.*;
import org.xml.sax.*;

/**
 * @author kitko
 *
 */
final class IDMSaxParser {
    
    ComponentInterfaces parse(InputSource source, EntityResolver entityResolver) throws SAXException, IOException, ParserConfigurationException{
        SAXParserFactory saxfactory = SAXParserFactory.newInstance();
        IDMHandler idmHandler = new IDMHandler();
        SAXParser parser = saxfactory.newSAXParser();
        XMLReader xmlReader = parser.getXMLReader();
        xmlReader.setEntityResolver(entityResolver);
        xmlReader.setContentHandler(idmHandler);
        xmlReader.parse(source);
        XmlObject root = idmHandler.getRoot();
        return parseWaveset(root);
    }
    
    private ComponentInterfaces parseWaveset(XmlObject root){
        XmlAttribute interfaces = root.getAttribute("interfaces");
        if(interfaces == null){
            throw new IllegalStateException("No interfaces attribute");
        }
        ComponentInterfaces.Builder builder = new ComponentInterfaces.Builder();
        XmlList interfacesList = (XmlList) interfaces.getValue();
        for(XmlObject o : interfacesList.getObjects()){
            ComponentInterface iface = parseInterface(o);
            builder.registerInterface(o.getName(), iface);
        }
        return builder.build();
    }
    
    private ComponentInterface parseInterface(XmlObject iface) {
        ComponentInterface.Builder builder = new ComponentInterface.Builder();
        for(String aName : iface.getAttributeNames()){
            XmlAttribute attribute = iface.getAttribute(aName);
            parseInterfaceAttribute(builder, attribute);
        }
        return builder.build();
    }

    private void parseInterfaceAttribute(ComponentInterface.Builder builder, XmlAttribute attr) {
       if("componentInterface".equals(attr.getName())){
           builder.setInterfaceName((String)attr.getValue());
       }
       else if("getKey".equals(attr.getName())){
           builder.setGetKey((String)attr.getValue());
       }
       else if("findKey".equals(attr.getName())){
           builder.setFindKey((String)attr.getValue());
       }
       else if("createKey".equals(attr.getName())){
           builder.setCreateKey((String)attr.getValue());
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

    private SupportedObjectTypes parseSupportedObjectTypes(XmlAttribute attr) {
        SupportedObjectTypes.Builder builder = new SupportedObjectTypes.Builder();
        XmlList list = (XmlList) attr.getValue();
        for(XmlObject obj : list.getObjects()){
            SupportedObjectTypes.Feautures.Builder featuresBuilder = new SupportedObjectTypes.Feautures.Builder();
            if(obj.getAttributesCount() != 1){
                throw new IllegalStateException("Must provide just one attribute named 'features' for " + obj.getName());
            }
            XmlAttribute features = obj.getAttribute("features");
            if(features == null){
                throw new IllegalStateException("Must provide just one attribute named 'features' for " + obj.getName() );
            }
            XmlList featuresList = (XmlList) features.getValue();
            for(XmlObject feature : featuresList.getObjects()){
                featuresBuilder.addFeature(feature.getName());
            }
            builder.addFeautures(obj.getName(), featuresBuilder.build());
        }
        return builder.build();
    }

    private List<Property> parseProperties(XmlAttribute attr) {
        List<Property> res = new ArrayList<Property>();
        XmlList list = (XmlList) attr.getValue();
        for(XmlObject o : list.getObjects()){
            Property property = null;
            if(o.getAttributesCount() == 0){
                property = new SingleProperty(o.getName());
            }
            else{
                CollectionProperty.Builder iBuilder = new CollectionProperty.Builder();
                iBuilder.setName(o.getName());
                boolean isCollectionSet = false;
                for(XmlAttribute iAttr : o.getAttributes()){
                   if("key".equals(iAttr.getName())){
                       iBuilder.setKey((String)iAttr.getValue());
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

    private DisableRule parseDisableRule(XmlAttribute attr) {
        DisableRule.Builder disableRuleBuilder = new DisableRule.Builder();
        XmlObject val = (XmlObject) attr.getValue();
        for(String aName : val.getAttributeNames()){
            XmlAttribute iAttr = val.getAttribute(aName);
            if("property".equals(iAttr.getName())){
                disableRuleBuilder.setName((String) iAttr.getValue());
            }
            else if("trueValue".equals(iAttr.getName())){
                disableRuleBuilder.setTrueValue((String) iAttr.getValue());
            }
            else if("falseValue".equals(iAttr.getName())){
                disableRuleBuilder.setFalseValue((String) iAttr.getValue());
            }
        }
        return disableRuleBuilder.build();
    }
    
}
