/**
 *
 *
 * @author Rob Jackson - Nulli Secundus Inc.
 * @version 1.0
 * @since 1.0
 */
package org.identityconnectors.webtimesheet;

import java.io.IOException;
import javax.xml.transform.TransformerException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.identityconnectors.common.logging.Log;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Class to build RTAPI requests
 *
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli Secundus Inc.</a>
 * @version 1.0
 * @since 1.0
 *
 */
public class RTAPIRequestBuilder {

    private String _rtapiVer;
    private DocumentBuilderFactory dbf;
    private DocumentBuilder db;
    private Document requestDocument;
    private static final Log log = Log.getLog(RTAPIRequestBuilder.class);

    /**
     * Client Constructor
     * 
     * @param version RTAPI version of request
     *
     * @throws ConnectorIOException
     *
     * */
    public RTAPIRequestBuilder(String version) {
        _rtapiVer = version;
        this.init();
    }

    /**
     * Client Constructor
     *
     * @param version RTAPI version of request
     *
     * @throws ConnectorIOException
     *
     * */
    public RTAPIRequestBuilder(String version, String ActionType) {
        _rtapiVer = version;
        this.init();
        this.addCommand("1", ActionType);
    }

    /**
     * Creates default Request document
     *
     *
     * */
    public void init() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(false);
            db = dbf.newDocumentBuilder();
            requestDocument = db.newDocument();
            requestDocument.setXmlVersion("1.0");
            ProcessingInstruction rtapiPI = requestDocument.createProcessingInstruction("rtapi", "version=\"" + _rtapiVer + "\"");
            requestDocument.appendChild(rtapiPI);
            Element requestElement = requestDocument.createElement("Request");
            requestElement.setAttribute("actionOnError", "continue");
            requestDocument.appendChild(requestElement);
            requestDocument.normalize();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }

    }

    /**
     * Release internal resources
     */
    public void dispose() {
    }

    /**
     * returns the xml request in string form
     */
    public String getRequestString() {
        try {
            javax.xml.transform.TransformerFactory tranFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer megatron = tranFactory.newTransformer();
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            megatron.transform(new DOMSource(requestDocument), new StreamResult(stringWriter));
            log.info("getRequestString: {0}", stringWriter.getBuffer().toString());
            return stringWriter.getBuffer().toString();
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * returns the xml request in DOM Document form
     */
    public Document getRequestDocument() {
        return requestDocument;
    }

    /**
     * Add a request command
     *
     * @param commandId id of command to add - will corespond to response
     * @param commandType name of the command to add
     */
    public void addCommand(String commandId, String commandType) {
        Element commandElement = requestDocument.createElement(commandType);
        commandElement.setAttribute("commandId", commandId);
        requestDocument.getElementsByTagName("Request").item(0).appendChild(commandElement);

    }

    /**
     * Add command argument
     *
     * @param commandId id of command argument is being added to
     * @param argName name of the argument to add
     * @param argValue value of the argument to add
     */
    public void addCommandArg(String commandId,
            String argName,
            String argValue,
            Boolean addIfEmpty) {
        if ((argValue != null) || (addIfEmpty)) {
            try {
                Element argumentElement = requestDocument.createElement(argName);
                if (argValue != null) {
                    argumentElement.setTextContent(argValue);
                }
                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("/Request/*");

                //need to add xpath query to filter on commandId

                requestDocument.normalizeDocument();
                Element commandElement = (Element) expr.evaluate(requestDocument, XPathConstants.NODE);
                if (commandElement != null) {
                    commandElement.appendChild(argumentElement);
                }
            } catch (XPathExpressionException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void addCommandArg(String commandId,
            String argName,
            String argValue) {
        addCommandArg(commandId, argName, argValue, false);
    }

    /**
     * Add command argument
     *
     * @param commandId id of command argument is being added to
     * @param argName name of the argument to add
     * @param argument DOM Element argument to add
     *
     */
    public void addCommandArg(
            String commandId,
            String argName,
            Element argument,
            Boolean addIfEmpty) {
        if ((argument != null) || (addIfEmpty)) {
            try {
                Element argumentElement = requestDocument.createElement(argName);
                if (argument != null) {
                    argumentElement.appendChild(argument);
                }
                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("/Request/*");
                //need to add xpath query to filter on commandId
                Element commandElement = (Element) expr.evaluate(requestDocument, XPathConstants.NODE);
                if (commandElement != null) {
                    commandElement.appendChild(argumentElement);
                }
            } catch (XPathExpressionException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void addCommandArg(
            String commandId,
            String argName,
            Element argument) {
        addCommandArg(commandId, argName, argument, false);
    }

    /**
     * Create Argument Element
     *
     * @param argName name of the argument to create
     * @param argValue value of the argument to create
     */
    public Element createArgElement(String argName, String argValue) {
        if (argValue != null) {
            Element argumentElement = requestDocument.createElement(argName);
            argumentElement.setTextContent(argValue);
            return argumentElement;
        }
        return null;
    }

    /**
     * Create Argument Element
     *
     * @param argName name of the argument to create
     * @param childElements elements that will be children of new element
     */
    public Element createArgElement(String argName, Element[] childElements) {
        if ((childElements != null) && (childElements.length > 0)) {
            Element argumentElement = requestDocument.createElement(argName);
            for (int i = 0; i < childElements.length; i++) {
                argumentElement.appendChild(childElements[i]);
            }
            return argumentElement;
        }
        return null;
    }

    /**
     * Create Argument Element
     *
     * @param argName name of the argument to create
     * @param childElement element that will be child of new element
     */
    public Element createArgElement(String argName, Element childElement) {
        if (childElement != null) {
            Element argumentElement = requestDocument.createElement(argName);
            argumentElement.appendChild(childElement);
            return argumentElement;
        }
        return null;
    }

    /**
     * Create Argument Element
     *
     * @param argXml xml argument to create
     */
    public Element createArgElement(String argXml) {
        log.info("createArgElement: {0}", argXml);
        if (argXml != null) {
            try {
                return (Element) requestDocument.importNode(db.parse(new java.io.ByteArrayInputStream(argXml.getBytes())).getDocumentElement(), true);
            } catch (SAXException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }
}

