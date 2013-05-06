/**
 *
 *
 * @author Robert Jackson - nulli.com
 * @version 1.0
 */
package org.forgerock.openicf.connectors.webtimesheet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Set;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Class to represent a WebTimeSheet Connection using new RepliConnect API
 * (Replaces RTAPI API). Apache HTTP Client is used to send XML requests to the
 * Web TimeSheet server. <br/></br/> Web TimeSheet (WTS) refers to a suite of
 * products offered by <a href='http://www.replicon.com'>Replicon Inc.</a>
 * <br/><br/> RepliConnect API Reference found <a
 * href='http://www.replicon.com/repliconnect'>here</a> <br/><br/> Currently,
 * client supports Create/Update/Delete/Searching of user objects and Searching
 * of Department objects <br/><br/>
 *
 * @author Robert Jackson - <a href='http://www.nulli.com'>Nulli</a>
 */
public class RepliConnectClient {

    /**
     * Setup logging for the {@link RepliConnectClient}.
     */
    private static final Log log = Log.getLog(RepliConnectClient.class);

    //private DefaultHttpClient _client;
    private final String _uri;
    private final UsernamePasswordCredentials credentials;
    //private HttpPost _httpPost;
    private final HttpHost _targetHost;
    private final BasicHttpContext _httpContext;


    /**
     * Client Constructor
     * 
     * @param cfg
     *            the WTS service configuration
     * 
     * @throws ConnectorIOException
     */
    public RepliConnectClient(WebTimeSheetConfiguration cfg) throws ConnectorIOException {
        
        _uri = cfg.getWtsURI();
        credentials = new UsernamePasswordCredentials(cfg.getAdminUid(), getPlainPassword(cfg.getAdminPassword()));

        _targetHost = new HttpHost(cfg.getWtsHost(), cfg.getWtsPort(), "https"); //hard-coded to HTTPS for now.. assume everyone would use it

        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();

        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(_targetHost, basicAuth);


        // Add AuthCache to the execution context
        _httpContext = new BasicHttpContext();
        _httpContext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        log.info("Creating POST action for {0}", _targetHost.getSchemeName() + "://" + _targetHost.toHostString() + _uri);

        //Connect();
    }

    private String getPlainPassword(final GuardedString password) {
        if (password == null) {
            return null;
        }
        final StringBuffer buf = new StringBuffer();
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                buf.append(clearChars);
            }
        });
        return buf.toString();
    }

    /**
     * Release internal resources
     */
    public void dispose() {
    }


    /*
     * Performs a simple test to ensure that the application is registered and authorized with the Web TimeSheet Service
     */
    public void testConnection() {
        //fetch self

        JSONObject query = new JSONObject();
                try {
                    query.put("Action", "Query");
                    query.put("DomainType", "Replicon.Domain.User");
                    query.put("QueryType", "UserByLoginName");
                    query.put("Args", new JSONArray().put(credentials.getUserName()));
                }
                catch (JSONException ex) {
                    log.error("Unable to prepare JSON query", ex);
                }


        JSONObject res = this.getUser(query.toString());

    }

    /*
     * Lists users with optional query
     *
     * @param query User query string
     *
     **/
    public JSONObject listUsers(String queryString) {


        JSONObject query = new JSONObject();
        try {
            query.put("Action", "Query");
            query.put("DomainType", "Replicon.Domain.User");
            query.put("QueryType", "UserAll");
            query.put("Args", new JSONArray());

        }
        catch (JSONException ex) {
            log.error("Unable to prepare JSON request", ex);
        }

        return this.call(query);


    }

    /*
     * Gets a single user record
     *
     * @param query User query string
     *
     **/
    public JSONObject getUser(String queryString) {


        JSONObject query = new JSONObject();
        try {
            /*
            query.put("Action", "Query");
            query.put("DomainType", "Replicon.Domain.User");
            query.put("QueryType", "UserByLoginName");
            query.put("Args", new JSONArray().put(queryString));
*/
JSONTokener jt = new JSONTokener(queryString);
                    query = new JSONObject(jt);

        }
        catch (JSONException ex) {
            log.error("Unable to prepare JSON request", ex);
        }

        return this.call(query);


    }

     /*
     * Creates a new user record
     *
     * @param attrs Set of Attributes for new user
     * @param deptId id of the PrimaryDepartment for new user
     *
     **/
    public Uid createUser(Set<Attribute> attrs, String deptId) {

        JSONObject user = new JSONObject();

        try {
            user.put("Action", "Create");
            user.put("Type", "Replicon.Domain.User");
            JSONObject op = new JSONObject();
            op.put("__operation", "SetProperties");

            JSONObject dept = new JSONObject();
            dept.put("__type", "Replicon.Domain.Department");
            dept.put("Identity", deptId);

            op.put("PrimaryDepartment", dept);

            Iterator aitr = attrs.iterator();
            while (aitr.hasNext()) {
                Attribute attr = (Attribute)aitr.next();
                if (!(attr.getName().startsWith("__"))) {
                    op.put(attr.getName(), attr.getValue().get(0));
                } else if (attr.getName().equalsIgnoreCase("__PASSWORD__")) {
                    op.put("Password", attr.getValue().get(0));
                }
            }
            JSONArray ops = new JSONArray();
            ops.put(op);
            user.put("Operations", ops);



        }
        catch (JSONException ex) {
            log.error("Unable to prepare JSON request", ex);
        }

        String newuid = null;
        JSONObject newuser = this.call(user);
        try {
                JSONArray users = newuser.getJSONArray("Value");
                newuid = users.getJSONObject(0).getString("Identity");

            }
            catch (JSONException ex) {
                /* ignore */
            }

        return new Uid(newuid);

    }

    /*
     * Updates a user record
     *
     * @param attrs Set of Attributes for new user
     * @param uid id of the user
     *
     **/
    public Uid updateUser(String uid, Set<Attribute> attrs) {

        JSONObject user = new JSONObject();


        try {
            user.put("Action", "Edit");
            user.put("Type", "Replicon.Domain.User");
            user.put("Identity", uid);
            JSONObject op = new JSONObject();
            op.put("__operation", "SetProperties");

            Iterator aitr = attrs.iterator();
            while (aitr.hasNext()) {
                Attribute attr = (Attribute)aitr.next();
                op.put(attr.getName(), attr.getValue().get(0));
            }
            JSONArray ops = new JSONArray();
            ops.put(op);
            user.put("Operations", ops);

        }
        catch (JSONException ex) {
            log.error("Unable to prepare JSON request", ex);
        }

        JSONObject result = this.call(user);
        String updateduid = null;
        try {
                JSONArray users = result.getJSONArray("Value");
                updateduid = users.getJSONObject(0).getString("Identity");

            }
            catch (JSONException ex) {
                /* ignore */
            }

        return new Uid(updateduid);

    }

    /*
     * Deletes a user record
     *
     * @param uid user account to delete
     *
     **/
    public void deleteUser(String uid) {

        JSONObject delete = new JSONObject();

        try {
            delete.put("Action", "Delete");
            delete.put("Type", "Replicon.Domain.User");
            delete.put("Identity", uid);
        }
        catch (JSONException ex) {
            log.error("Unable to prepare JSON request", ex);
        }

        this.call(delete);
        //add some error handling

    }

    protected JSONObject call(JSONObject command) throws RuntimeException {
        log.info("Posting request: {0}", command.toString());
        HttpPost postAction;
        DefaultHttpClient client = new DefaultHttpClient();

        try {
            client = new DefaultHttpClient();
        }
        catch (java.lang.NoClassDefFoundError ex) {
            throw new ConnectorIOException("Missing Apache HTTPClient Library (or dependency)");
        }

        client.getCredentialsProvider().setCredentials(
                new AuthScope(_targetHost.getHostName(), _targetHost.getPort()),
                credentials);


        postAction = new HttpPost(_targetHost.getSchemeName() + "://" + _targetHost.toHostString() + _uri);
        log.info("Constructed URL: {0}", _targetHost.getSchemeName() + "://" + _targetHost.toHostString() + _uri);
        postAction.addHeader("Content-type", "application/JSON");
        // User mode is new but makes objects read-only - don't want that
        // need to get Replicon Support to unhide the "Can view all system data" permission
        // http://www.replicon.com/kb-2934
        //postAction.addHeader("X-Replicon-Security-Context", "User");
        try {
            postAction.setEntity(new StringEntity(command.toString()));

            log.info("Added payload: {0}", command.toString());

            HttpResponse res = client.execute(postAction, _httpContext);

            int statusCode = res.getStatusLine().getStatusCode();

            log.info("HTTP Response Code: {0}", statusCode);

            if (statusCode != 200) {
                throw new org.identityconnectors.framework.common.exceptions.ConnectionFailedException("HTTP code:" + statusCode);
            } else {
                //res.getEntity().writeTo(System.out);
                try {
                    Reader reader = new InputStreamReader(res.getEntity().getContent());
                    JSONTokener jt = new JSONTokener(reader);
                    JSONObject response = new JSONObject(jt);
                    log.info("JSONResponse: {0}", response.toString());
                    log.info("RepliConnect call Status: {0}", response.getString("Status"));
                    if (response.getString("Status").equalsIgnoreCase("Exception")) {
                        throw new RuntimeException("Error in RepliConnect Call: " + response.getString("Message"));
                    }
                    return response;
                }
                catch (JSONException ex) {
                    log.error("Unable to Tokenize JSON response", ex);
                }

            }
            return null;
        }
        catch (IOException ex) {
            log.error("Error Occured Posting Request", ex);
            throw new RuntimeException(ex);
        }
    }
}
