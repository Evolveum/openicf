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
package org.identityconnectors.parser.factory.patternparser;

import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.parser.factory.OutputParser;
import org.identityconnectors.patternparser.MapTransform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * OutputParser based on the MapTransform class
 * 
 * @author hetrick
 *
 */
public class MapTransformParser implements OutputParser {
    private MapTransform _transform;
    
    public MapTransformParser(String parserDefinition) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document document = parser.parse(new InputSource(new StringReader(parserDefinition)));
            NodeList elements = document.getChildNodes();
            for (int i = 0; i < elements.getLength(); i++) {
                if (elements.item(i) instanceof Element) {
                    _transform = new MapTransform((Element) elements.item(i));
                }
            }
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.identityconnectors.racf.OutputParser#parse(java.lang.String)
     */
    public Map<String, Object> parse(String input) {
        try {
            return (Map<String, Object>)_transform.transform(input);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }
}
