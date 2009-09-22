package org.identityconnectors.racf;

import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
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
