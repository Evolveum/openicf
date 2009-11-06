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

import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.validation.*;

import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.peoplesoft.compintfc.*;
import org.identityconnectors.peoplesoft.compintfc.mapping.*;
import org.xml.sax.*;

/**
 * @author kitko
 *
 */
final class IDMSAXComponentInterfacesFactory implements ComponentInterfacesFactory {

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
        EntityResolver entityResolver = new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if("waveset.dtd".equals(publicId)){
                    return new InputSource(dtdURL.openStream()); 
                }
                return null;
            }
        };
        ErrorHandler errorHandler =  new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
            }
            public void fatalError(SAXParseException exception) throws SAXException {
            }
            public void error(SAXParseException exception) throws SAXException {
                if(exception.getMessage().contains("cvc-complex-type.3.2.2")){
                    return;
                }
                throw exception;
            }
        };
        try{
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(schemaURL);
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            SAXParser parser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setEntityResolver(entityResolver);
            ValidatorHandler newValidatorHandler = schema.newValidatorHandler();
            newValidatorHandler.setErrorHandler(errorHandler);
            xmlReader.setContentHandler(newValidatorHandler);
            xmlReader.parse(new InputSource(new StringReader(xml)));
            return new IDMSaxParser().parse(new InputSource(new StringReader(xml)), entityResolver);
        }
        catch(Exception e){
            throw new ConnectorException("Cannot parse idm interface mapping", e);
        }
    }
}
