/**
 *
 *
 * @author Rob Jackson - Nulli Secundus Inc.
 * @version 1.0
 * @since 1.0
 */
package org.identityconnectors.webtimesheet;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.identityconnectors.common.logging.Log;
import org.apache.http.HttpResponse;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Class to parse RTAPI responses
 *
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli Secundus Inc.</a>
 * @version 1.0
 * @since 1.0
 *
 */
public class RTAPIResponseParser {

    private DocumentBuilderFactory dbf;
    private DocumentBuilder db;
    private Document responseDocument;
    private static final Log log = Log.getLog(RTAPIResponseParser.class);

    /**
     * Client Constructor
     * 
     * @param res HTTP Response containing RTAPI response
     *
     *
     * */
    public RTAPIResponseParser(HttpResponse res) {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(false);
            db = dbf.newDocumentBuilder();
            responseDocument = db.parse(res.getEntity().getContent());
            log.info("Response XML: {0}", XMLtoString(responseDocument));
        } catch (SAXException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (IllegalStateException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (ParserConfigurationException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns the string referenced in the input xpath query - non-static version - used when multiple elements fetched
     * from same response
     *
     * @param xpathStr xpath string
     *
     * @return String value referenced in the xpath expression
     *
     *
     * */
    public String getRTAPIResponseString(String xpathStr) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(xpathStr);
            return (String) expr.evaluate(responseDocument, XPathConstants.STRING);
        } catch (IllegalStateException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (XPathExpressionException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns the string referenced in the input xpath query - Static version
     *
     * @param res HTTP Response containing RTAPI response
     * @param xpathStr xpath expression string
     *
     * @return String value referenced in the xpath expression
     *
     *
     * */
    public static String getRTAPIResponseString(HttpResponse res, String xpathStr) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(xpathStr);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document responseDocument = db.parse(res.getEntity().getContent());
            return (String) expr.evaluate(responseDocument, XPathConstants.STRING);
        } catch (ParserConfigurationException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (SAXException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (IllegalStateException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (XPathExpressionException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Convert a Node object to a string
     *
     * @param xml a DOM node object to transform
     *
     * @return String representation of the xml
     *
     **/
    private String XMLtoString(Node xml) {
        try {
            javax.xml.transform.TransformerFactory tranFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer megatron = tranFactory.newTransformer();
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            megatron.transform(new DOMSource(xml), new StreamResult(stringWriter));
            return stringWriter.getBuffer().toString();
        } catch (TransformerException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        }

    }
}

