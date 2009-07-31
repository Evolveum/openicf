/**
 *
 *
 * @author Rob Jackson - Nulli Secundus Inc.
 * @version 1.0
 * @since 1.0
 */
package org.identityconnectors.webtimesheet;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.xml.transform.TransformerException;
import org.apache.http.client.HttpClient.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.client.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.identityconnectors.framework.common.objects.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.identityconnectors.common.logging.Log;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.identityconnectors.framework.common.exceptions.*;

/**
 * Class to represent a WebTimeSheet Connection.  Apache HTTP Client is used to send XML requests to the Web TimeSheet server.
 * <br/></br/>
 * Web TimeSheet (WTS) refers to a suite of products offered by <a href='http://www.replicon.com'>Replicon Inc.</a>
 * <br/><br/>
 * Currently, client supports Create/Update/Delete/Searching of user objects and Searching of Department objects
 * <br/><br/>
 * TODO: create common request builder class that uses DOM to build xml rather than building string by hand
 *
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli Secundus Inc.</a>
 * @version 1.0
 * @since 1.0
 *
 */
public class RTAPIClient {

    /*
     * Globally set the RTAPI version for requests.  Note that this version may need to be increased and regression tested
     * when 8.3 compatibility is changed.  Current as of WTS v8.8
     */
    private String _rtapiVer = "8.3";
    private DefaultHttpClient _client;
    private String _baseUrl;
    private String _appName;
    private String _appPassword;
    private String _adminUID;
    private String _adminPassword;
    private String _banner;
    /**
     * Setup logging for the {@link WebTimeSheetConnector}.
     */
    private static final Log log = Log.getLog(RTAPIClient.class);

    /**
     * Client Constructor
     * 
     * @param url URL of WTS service
     * @param appName Name used when registering/authenticating with WTS
     * @param appPwd Application Password
     * @param uid UserID of account used to make modifications
     * @param pwd Password of user
     *
     * @throws ConnectorIOException
     *
     * */
    public RTAPIClient(String url, String appName, String appPwd, String uid, String pwd) throws ConnectorIOException {

        _baseUrl = url;
        _appName = appName;
        _appPassword = appPwd;
        _adminUID = uid;
        _adminPassword = pwd;
        try {
            _client = new DefaultHttpClient();
        } catch (java.lang.NoClassDefFoundError ex) {
            throw new ConnectorIOException("Missing Apache HTTPClient Library (or dependency)");
        }
        Connect();
    }

    /**
     * Release internal resources
     */
    public void dispose() {
    }

    /*
     * returns the instance of the web client
     */
    public HttpClient getClient() {
        return _client;
    }

    /*
     * Performs a simple test to ensure that the application is registered and authorized with the Web TimeSheet Service
     */
    public void testConnection() {
        try {
            HttpPost testUrl = new HttpPost(_baseUrl);

            String helloRequest =
                    "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'><HelloAction commandId='1'><ApplicationName>" + _appName + "</ApplicationName></HelloAction></Request>";
            testUrl.addHeader("Content-type", "application/xml");
            testUrl.setEntity(new StringEntity(helloRequest));
            HttpResponse res = _client.execute(testUrl);

            int statusCode = res.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException("HTTP Status: " + statusCode);
            }

            log.info("Connector Test Status Code: {0}", statusCode);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(false);
            DocumentBuilder db;

            db =
                    dbf.newDocumentBuilder();
            Document doc;

            doc =
                    db.parse(res.getEntity().getContent());

            log.info("Starting to Parse Results");

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("Response/HelloActionRs/ApplicationStatus");
            Node appStatus = (Node) expr.evaluate(doc, XPathConstants.NODE);
            log.info("XMLResponse: {0}", this.XMLtoString(doc));

            log.info("Application Status: {0}", appStatus.getTextContent());

            expr =
                    xpath.compile("Response/HelloActionRs/Banner");
            if (!appStatus.getTextContent().equalsIgnoreCase("Authorized")) {
                throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException("App Status: " + appStatus.getTextContent());

            }

        } catch (SAXException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (ParserConfigurationException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (XPathExpressionException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        }

    }

    /*
     * Connects to the Web TimeSheet Service - Registers if needed
     */
    public void Connect() {
        try {
            HttpPost testUrl = new HttpPost(_baseUrl);

            String helloRequest =
                    "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'><HelloAction commandId='1'><ApplicationName>" + _appName + "</ApplicationName></HelloAction></Request>";
            testUrl.addHeader("Content-type", "application/xml");
            testUrl.setEntity(new StringEntity(helloRequest));
            HttpResponse res = _client.execute(testUrl);

            int statusCode = res.getStatusLine().getStatusCode();
            log.info("Connector Test Status Code: {0}", statusCode);

            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(false);
                dbf.setNamespaceAware(false);
                DocumentBuilder db;

                db =
                        dbf.newDocumentBuilder();
                Document doc;

                doc =
                        db.parse(res.getEntity().getContent());
                //_client.getConnectionManager().shutdown();

                log.info("Starting to Parse Results");

                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("Response/HelloActionRs/ApplicationStatus");
                Node appStatus = (Node) expr.evaluate(doc, XPathConstants.NODE);
                log.info("XMLResponse: {0}", this.XMLtoString(doc));

                log.info("Application Status: {0}", appStatus.getTextContent());

                expr =
                        xpath.compile("Response/HelloActionRs/Banner");
                Node banner = (Node) expr.evaluate(doc, XPathConstants.NODE);
                _banner =
                        banner.getTextContent();

                log.info("Banner: {0}", _banner);

                String hashedPassword = GetHashedPassword(_banner, _appPassword);
                log.info("Hashed Password: {0}", hashedPassword);

                if (appStatus.getTextContent().equalsIgnoreCase("Pending")) {
                    throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException("Application registration pending - please Authorize '" + _appName + "' in WTS 'Integration Setup'");
                }

                if (appStatus.getTextContent().equalsIgnoreCase("Unrecognized")) {
                    log.info("Need to register App with WTS");
                    String regRequest =
                            "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'><RegisterApplicationAction commandId='1'><Password>" + _appPassword + "</Password></RegisterApplicationAction></Request>";
                    log.info("Register Request: {0}", regRequest);
                    testUrl.setEntity(new StringEntity(regRequest));
                    res =
                            _client.execute(testUrl);
                    doc =
                            db.parse(res.getEntity().getContent());
                    //_client.getConnectionManager().shutdown();
                    log.info("XMLResponse: {0}", this.XMLtoString(doc));
                    throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException("Application requested registration - please Authorize '" + _appName + "' in WTS 'Integration Setup'");


                } else if (appStatus.getTextContent().equalsIgnoreCase("Authorized")) {
                    log.info("Authorized - Logging App into WTS");
                    String loginRequest =
                            "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'><LoginAction commandId='1'><UserRef><LoginName>" + _adminUID + "</LoginName></UserRef><Password>" + _adminPassword + "</Password><ApplicationPassword>" + hashedPassword + "</ApplicationPassword></LoginAction></Request>";
                    log.info("Login Request: {0}", loginRequest);
                    testUrl.setEntity(new StringEntity(loginRequest));
                    res =
                            _client.execute(testUrl);
                    doc =
                            db.parse(res.getEntity().getContent());
                    //_client.getConnectionManager().shutdown();
                    log.info("XMLResponse: {0}", this.XMLtoString(doc));

                    expr =
                            xpath.compile("Response/LoginActionRs/@statusMessage");
                    String loginResult = (String) expr.evaluate(doc, XPathConstants.STRING);
                    log.info("Login Result: {0}", loginResult);

                    if (!loginResult.equalsIgnoreCase("Success")) {
                        throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(loginResult);
                    }

                }



            } catch (SAXException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            } catch (ParserConfigurationException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            } catch (XPathExpressionException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            }

            if (statusCode != 200) {
                throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException();
            }

        } catch (IOException ex) {
            throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(ex);
        }

    }

    /*
     * Lists users with optional query
     *
     * @param query User query string
     *
     **/
    public NodeList listUsers(
            String query) {
        try {
            HttpPost url = new HttpPost(_baseUrl);


            String userQueryRequest =
                    "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'>" +
                    "<UserQuery detailedInformation='true'>" +
                    "<SearchParam>" + query + "</SearchParam>" +
                    "</UserQuery></Request>";
            log.info("request: {0}", userQueryRequest);


            url.addHeader("Content-type", "application/xml");
            url.setEntity(new StringEntity(userQueryRequest));
            HttpResponse res = _client.execute(url);

            int statusCode = res.getStatusLine().getStatusCode();
            log.info("userQueryRequest Status Code: {0}", statusCode);

            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(false);
                dbf.setNamespaceAware(false);
                DocumentBuilder db;

                db =
                        dbf.newDocumentBuilder();
                Document doc;

                doc =
                        db.parse(res.getEntity().getContent());
                //_client.getConnectionManager().shutdown();
                log.info("XMLResponse: {0}", this.XMLtoString(doc));
                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("Response/UserQueryRs/User");
                return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            } catch (SAXException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            } catch (ParserConfigurationException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            } catch (XPathExpressionException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            }

        } catch (IOException ex) {
            throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(ex);
        }

    }

    /**
     * Lists departments with optional, query, parent and scope
     * if parent is not specified and scope is onelevel or null, only the root department is returned (used for
     * building org tree)
     * if scope is subtree (regardless of parent) - all Departments are returned (usefull for creating list of
     * departments user can be assigned to)
     *
     * @param query Department Query String
     * @param scope Scope String (onelevel, subtree)
     * @param parent id of parent to query
     *
     **/
    public NodeList listDepartments(
            String query, String scope, String parent) {

        try {
            HttpPost url = new HttpPost(_baseUrl);

            log.info("Scope: {0} Parent: {1}", scope, parent);

            if ((parent == null) && ((scope == null) || (scope.equalsIgnoreCase("onelevel")))) {
                //default return only root department
                query = "<Id>1</Id>";
            } else if (parent != null) {
                //return departments in specified container
                query = "<And>" + query + "<ParentDepartmentRef><Id>" + parent + "</Id></ParentDepartmentRef></And>";

            }


            String userQueryRequest =
                    "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'>" +
                    "<DepartmentQuery detailedInformation='true'>" +
                    "<SearchParam>" + query + "</SearchParam>" +
                    "</DepartmentQuery>ParentDepartmentRef/></Request>";
            log.info("request: {0}", userQueryRequest);


            url.addHeader("Content-type", "application/xml");
            url.setEntity(new StringEntity(userQueryRequest));
            HttpResponse res = _client.execute(url);

            int statusCode = res.getStatusLine().getStatusCode();
            log.info("userQueryRequest Status Code: {0}", statusCode);

            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(false);
                dbf.setNamespaceAware(false);
                DocumentBuilder db;

                db =
                        dbf.newDocumentBuilder();
                Document doc;

                doc =
                        db.parse(res.getEntity().getContent());
                //_client.getConnectionManager().shutdown();
                log.info("XMLResponse: {0}", this.XMLtoString(doc));
                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("Response/DepartmentQueryRs/Department");
                return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            } catch (SAXException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            } catch (ParserConfigurationException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            } catch (XPathExpressionException ex) {
                log.error("Exception: {0}", ex.getMessage());
                throw new RuntimeException(ex);
            }

        } catch (IOException ex) {
            throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(ex);
        }

    }

    /**
     * Create a new user in WTS
     *
     * @param FirstName First Name
     * @param LastName Last Name
     * @param LoginName Login Name
     * @param Password Password
     * @param Department Department
     * @param Email Email address
     * @param EmployeeId Employee Id
     * @param Enabled True=Enabled, False=Disabled
     *
     * @return UID of new User
     *
     **/
    public Uid createUser(
            String FirstName, String LastName, String LoginName, String Password, String Department, String Email, String EmployeeId, Boolean Enabled) {
        try {
            HttpPost url = new HttpPost(_baseUrl);


            String userCreateRequest =
                    "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'>" +
                    "<UserAdd commandId='1'>" +
                    "<FirstName>" + FirstName + "</FirstName>" +
                    "<LastName>" + LastName + "</LastName>" +
                    "<LoginName>" + LoginName + "</LoginName>" +
                    "<Password>" + Password + "</Password>" +
                    "<InternalEmail>" + Email + "</InternalEmail>" +
                    "<EmployeeId>" + EmployeeId + "</EmployeeId>" +
                    "<Enabled>" + Enabled.toString() + "</Enabled>" +
                    "<PrimaryDepartmentRef><Id>" + Department + "</Id></PrimaryDepartmentRef>" +
                    "</UserAdd>" +
                    "</Request>";
            log.info("request: {0}", userCreateRequest);


            url.addHeader("Content-type", "application/xml");
            url.setEntity(new StringEntity(userCreateRequest));
            HttpResponse res = _client.execute(url);

            int statusCode = res.getStatusLine().getStatusCode();
            log.info("userCreateRequest Status Code: {0}", statusCode);

            if (statusCode != 200) {
                throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException();
            } else {

                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setValidating(false);
                    dbf.setNamespaceAware(false);
                    DocumentBuilder db;

                    db =
                            dbf.newDocumentBuilder();
                    Document doc;

                    doc =
                            db.parse(res.getEntity().getContent());
                    //_client.getConnectionManager().shutdown();
                    log.info("XMLResponse: {0}", this.XMLtoString(doc));
                    XPath xpath = XPathFactory.newInstance().newXPath();
                    XPathExpression expr = xpath.compile("Response/UserAddRs/User/Id");
                    String Id = (String) expr.evaluate(doc, XPathConstants.STRING);
                    log.info("User {0} Created", Id);

                    if (Id.length() > 0) {
                        return new Uid(Id);
                    } else {
                        expr = xpath.compile("Response/UserAddRs/@statusMessage");
                        String message = (String) expr.evaluate(doc, XPathConstants.STRING);
                        throw new org.identityconnectors.framework.common.exceptions.ConnectorIOException(message);
                    }

                } catch (SAXException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (ParserConfigurationException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (XPathExpressionException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                }

            }

        } catch (IOException ex) {
            throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(ex);
        }

    }

    /**
     * Update and existing user in WTS
     *
     * @param uid Numeric id of existing user
     * @param FirstName New First Name
     * @param LastName New Last Name
     * @param LoginName New Login Name
     * @param Password New Password
     * @param Department New Department
     * @param Email New Email address
     * @param EmployeeId New Employee Id
     * @param Enabled New True=Enabled, False=Disabled
     *
     * @return UID of updated User
     *
     **/
    public Uid updateUser(Uid uid, String FirstName, String LastName, String LoginName, String Password, String Department, String Email, String EmployeeId, Boolean Enabled) {
        try {
            HttpPost url = new HttpPost(_baseUrl);
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\"?><?rtapi version=\"");
            sb.append(_rtapiVer);
            sb.append("\"?><Request actionOnError='continue'>");
            sb.append("<UserMod commandId='1'>");
            sb.append("<UserRef><Id>");
            sb.append(uid.getUidValue());
            sb.append("</Id></UserRef>");
            if (FirstName != null) {
                sb.append("<FirstName>");
                sb.append(FirstName);
                sb.append("</FirstName>");
            }
            if (LastName != null) {
                sb.append("<LastName>");
                sb.append(LastName);
                sb.append("</LastName>");
            }
            if (LoginName != null) {
                sb.append("<LoginName>");
                sb.append(LoginName);
                sb.append("</LoginName>");
            }
            if (Password != null) {
                sb.append("<Password>");
                sb.append(Password);
                sb.append("</Password>");
            }
            if (Email != null) {
                sb.append("<Email>");
                sb.append(Email);
                sb.append("</Email>");
            }
            if (EmployeeId != null) {
                sb.append("<EmployeeId>");
                sb.append(EmployeeId);
                sb.append("</EmployeeId>");
            }
            if (Enabled != null) {
                sb.append("<Enabled>");
                sb.append(Enabled.toString());
                sb.append("</Enabled>");
            }
            if (Department != null) {
                sb.append("<PrimaryDepartmentRef><Id>");
                sb.append(Department);
                sb.append("</Id></PrimaryDepartmentRef>");
            }
            sb.append("</UserMod></Request>");

            String userUpdateRequest = sb.toString();

            log.info("request: {0}", userUpdateRequest);

            url.addHeader("Content-type", "application/xml");
            url.setEntity(new StringEntity(userUpdateRequest));
            HttpResponse res = _client.execute(url);

            int statusCode = res.getStatusLine().getStatusCode();
            log.info("userCreateRequest Status Code: {0}", statusCode);

            if (statusCode != 200) {
                throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException();
            } else {

                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setValidating(false);
                    dbf.setNamespaceAware(false);
                    DocumentBuilder db;

                    db =
                            dbf.newDocumentBuilder();
                    Document doc;

                    doc =
                            db.parse(res.getEntity().getContent());
                    //_client.getConnectionManager().shutdown();
                    log.info("XMLResponse: {0}", this.XMLtoString(doc));
                    XPath xpath = XPathFactory.newInstance().newXPath();
                    XPathExpression expr = xpath.compile("Response/User/Id");
                    String Id = (String) expr.evaluate(doc, XPathConstants.STRING);
                    log.info("User {0} Modified", Id);

                    if (Id.length() > 0) {
                        return new Uid(Id);
                    } else {
                        expr = xpath.compile("Response/UserModRs/@statusMessage");
                        String message = (String) expr.evaluate(doc, XPathConstants.STRING);
                        throw new org.identityconnectors.framework.common.exceptions.ConnectorIOException(message);
                    }

                } catch (SAXException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (ParserConfigurationException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (XPathExpressionException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                }

            }

        } catch (IOException ex) {
            throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(ex);
        }

    }

    /**
     * Delete user in WTS
     *
     **/
    public void deleteUser(String id) {
        try {
            HttpPost url = new HttpPost(_baseUrl);
            String userDeleteRequest =
                    "<?xml version=\"1.0\"?><?rtapi version=\"" + _rtapiVer + "\"?><Request actionOnError='continue'>" +
                    "<UserDel commandId='1'>" +
                    "<UserRef><Id>" + id + "</Id></UserRef>" +
                    "</UserDel>" +
                    "</Request>";
            log.info("request: {0}", userDeleteRequest);


            url.addHeader("Content-type", "application/xml");
            url.setEntity(new StringEntity(userDeleteRequest));
            HttpResponse res = _client.execute(url);

            int statusCode = res.getStatusLine().getStatusCode();
            log.info("userDeleteRequest Status Code: {0}", statusCode);

            if (statusCode != 200) {
                throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException();
            } else {

                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setValidating(false);
                    dbf.setNamespaceAware(false);
                    DocumentBuilder db;

                    db =
                            dbf.newDocumentBuilder();
                    Document doc;

                    doc =
                            db.parse(res.getEntity().getContent());
                    //_client.getConnectionManager().shutdown();
                    log.info("XMLResponse: {0}", this.XMLtoString(doc));
                    XPath xpath = XPathFactory.newInstance().newXPath();
                    XPathExpression expr = xpath.compile("Response/UserDelRs/@statusMessage");
                    String message = (String) expr.evaluate(doc, XPathConstants.STRING);
                    if (!message.equalsIgnoreCase("Success")) {
                        throw new org.identityconnectors.framework.common.exceptions.ConnectorIOException(message);
                    }

                } catch (SAXException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (ParserConfigurationException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                } catch (XPathExpressionException ex) {
                    log.error("Exception: {0}", ex.getMessage());
                    throw new RuntimeException(ex);
                }

            }

        } catch (IOException ex) {
            throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException(ex);
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

    /**
     * Creates hashed password used to authenticate application to WTS
     *
     * @param banner Banner string recieved from RTAPI hello request
     * @param password Application Password
     *
     * @return Hashed Application Password
     *
     **/
    private static String GetHashedPassword(String banner, String password) {
        //StringBuilder pwd = new StringBuilder();
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update((banner + password).getBytes());
            byte[] hash = digest.digest();
            return convertToHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Exception: {0}", ex.getMessage());
            throw new RuntimeException(ex);
        }

    }

    /**
     * Converts Byte Array to Hex String
     *
     * @param data Hashed Password bytes
     *
     * @return Hashed Application Password
     *
     **/
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i <
                data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }

                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }

        return buf.toString();
    }
}