/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * $Id$
 */
package org.forgerock.openicf.salesforce;

import org.forgerock.openicf.salesforce.translators.IFieldTranslator;
import org.forgerock.openicf.salesforce.translators.TranslatorRegistry;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.engine.header.ChallengeWriter;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.*;
import org.restlet.util.Series;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Class to represent a Salesforce Connection
 * http://wiki.restlet.org/docs_2.1/13-restlet/28-restlet/392-restlet.html
 * http://boards.developerforce.com/t5/REST-API-Integration/Having-trouble-getting-Access-Token-with-username-password/td-p/278305
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class SalesforceConnection extends ClientResource {

    /**
     * Setup logging for the {@link SalesforceConnector}.
     */
    private static final Log log = Log.getLog(SalesforceConnection.class);

    /**
     * Requests that the origin server accepts the entity enclosed in the
     * request as a new subordinate of the resource identified by the request
     * URI.
     *
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2068/rfc2068">HTTP
     *      PATCH method</a>
     */
    public static final Method PATCH = new Method("PATCH");

    public static final String INSTANCE_URL = "instance_url";

    public static final String ACCESS_TOKEN = "access_token";

    public static final String CLIENT_ID = "client_id";

    public static final String CLIENT_SECRET = "client_secret";

    public static final String ERROR = "error";

    public static final String ERROR_DESC = "error_description";

    public static final String ERROR_URI = "error_uri";

    public static final String EXPIRES_IN = "expires_in";

    public static final String GRANT_TYPE = "grant_type";

    public static final String PASSWORD = "password";

    public static final String SCOPE = "scope";

    public static final String STATE = "state";

    public static final String USERNAME = "username";

    public static final String SIGNATURE = "signature";

    private ExecutorService threadPool;

    private TranslatorRegistry translatorRegistry;

    private SalesforceConfiguration configuration;

    private OAuthUser authentication = null;


    public SalesforceConnection(ClientResource resource) {
        super(resource);
    }

    public SalesforceConnection(SalesforceConfiguration configuration) {
        super(new Context(), SalesforceConfiguration.LOGIN_URL);
        this.configuration = configuration;
        this.threadPool = Executors.newFixedThreadPool(configuration.getThreadPoolSize());
        this.translatorRegistry = new TranslatorRegistry(this);

        Client client = new Client(Protocol.HTTPS);
        client.setContext(getContext());
        setNext(client);

        // Accept: application/json
        List<Preference<MediaType>> acceptedMediaTypes = new ArrayList<Preference<MediaType>>(1);
        acceptedMediaTypes.add(new Preference(MediaType.APPLICATION_JSON));
        getClientInfo().setAcceptedMediaTypes(acceptedMediaTypes);

        Form form = configuration.getAuthenticationForm();
        Representation body = null;

        try {
            body = post(form.getWebRepresentation());

            if (getStatus().isSuccess()) {
                if (body instanceof EmptyRepresentation == false) {
                    authentication = createJson(new JacksonRepresentation<Map>(body, Map.class));
                }
            }
        } catch (Exception e) {
            throw new ConnectionFailedException(e);
        } finally {
            if (body != null)
                body.release();
            release();
        }
    }

    public void test() {
        Representation body = null;
        try {
            ClientResource rc = getChild("services/data/v23.0");
            body = rc.get();
        } catch (Exception e) {
            throw new ConnectionFailedException(e);
        } finally {
            if (body != null)
                body.release();
        }
    }

    public void dispose() {
        if (threadPool != null) {
            threadPool.shutdown();
            threadPool = null;
        }
        if (translatorRegistry != null) {
            translatorRegistry.dispose();
            translatorRegistry = null;
        }
    }

    /**
     * Patches a representation. If a success status is not returned, then a
     * resource exception is thrown.
     *
     * @param entity The posted entity.
     * @return The optional result entity.
     * @throws org.restlet.resource.ResourceException
     *
     * @see <a
     *      href="http://www.w3.org/Protocols/rfc2068/rfc2068">HTTP
     *      PATCH method</a>
     */
    public Representation patch(Representation entity) throws ResourceException {
        return handle(PATCH, entity);
    }

    /**
     * {@inheritDoc}
     */
    public Representation get() throws ResourceException {
        if (Protocol.FILE.equals(getReference().getSchemeProtocol())) {
            File input = new File((new Reference(getReference(), new Reference("GET.json"))).getTargetRef().getPath(true));
            if (input.exists()) {
                return new FileRepresentation(input, MediaType.APPLICATION_JSON);
            } else {
                return new EmptyRepresentation();
            }
        }
        return super.get();
    }

    /**
     * {@inheritDoc}
     */
    public ClientResource getChild(Reference relativeRef) throws ResourceException {
        ClientResource result = null;

        if ((relativeRef != null) && relativeRef.isRelative()) {
            result = new SalesforceConnection(this);
            result.setReference(new Reference(authentication.getBaseReference(),
                    relativeRef).getTargetRef());
            // -------------------------------------
            //  Add user-defined extension headers
            // -------------------------------------
            Series<Header> additionalHeaders = (Series<Header>) result.getRequest().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
            if (additionalHeaders == null) {
                additionalHeaders = new Series<Header>(Header.class);
                result.getRequest().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, additionalHeaders);
            }
            additionalHeaders.add(HeaderConstants.HEADER_AUTHORIZATION, authentication.getAccessToken());
            additionalHeaders.add("X-PrettyPrint", "1");
        } else {
            doError(Status.CLIENT_ERROR_BAD_REQUEST,
                    "The child URI is not relative.");
        }
        return result;
    }

    @Override
    protected void redirect(Request request, Response response, List<Reference> references, int retryAttempt, Uniform next) {
        if (retryAttempt < 0) {
            for (CookieSetting cs : response.getCookieSettings()) {
                request.getCookies().add(cs.getName(), cs.getValue());
            }
        }
        super.redirect(request, response, references, retryAttempt, next);
    }

    /**
     * Converts successful JSON token body responses to OAuthUser.
     *
     * @param body Representation containing a successful JSON body element.
     * @return OAuthUser object containing accessToken, refreshToken and
     *         expiration time.
     */
    public OAuthUser createJson(JacksonRepresentation<Map> body) {
        /*
          {
            "id" : "https://login.salesforce.com/id/00Dd0000000bkONEAY/005d0000000uqvbAAA",
            "issued_at" : "1325869693249",
            "instance_url" : "https://na14.salesforce.com",
            "signature" : "dUaMkN5HSskfclyE8uol9Wn3vg6rdJLdZXK5hFkM9TE=",
            "access_token" : "00Dd0000000bkON!AQ4AQGtHVon9uQuQw28DVX6V6OP.6LRhnItGj0PpRcMO_w4giGTKbvSXBYHcKtx8sKm4lNiDJoRyA4EdwrPXArRPIIMp_IGh"
          }
        */

        Logger log = Context.getCurrentLogger();

        Map answer = body != null ? body.getObject() : null;

        if (null != answer) {
            String accessToken = null;
            if (answer.get(ACCESS_TOKEN) instanceof String) {
                accessToken = (String) answer.get(ACCESS_TOKEN);
                log.fine("AccessToken = " + accessToken);
            }

            String signature = null;
            if (answer.get(SIGNATURE) instanceof String) {
                signature = (String) answer.get(SIGNATURE);
                log.fine("Signature = " + signature);
            }

            String instanceUrl = null;
            if (answer.get(INSTANCE_URL) instanceof String) {
                instanceUrl = (String) answer.get(INSTANCE_URL);
                log.fine("InstanceUrl = " + instanceUrl);
            }

            String id = null;
            if (answer.get("id") instanceof String) {
                id = (String) answer.get("id");
                log.fine("Id = " + signature);
            }

            Date issued = null;
            if (answer.get("issued_at") instanceof String) {
                issued = new Date(Long.parseLong((String) answer.get("issued_at")));
                log.fine("Issued at = " + issued);
            }

            return new OAuthUser(id, issued, instanceUrl, signature, accessToken);
        }
        return null;
    }


    /**
     * Submit a new job to the thread pool.
     *
     * @param runnable the job to submit.
     */
    public void submitJob(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Submitted job cannot be null");
        }

        threadPool.execute(runnable);
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public boolean registerTranslator(Class<?> owner, String property,
                                      Class<? extends IFieldTranslator<?, ?>> type) {
        return translatorRegistry.register(owner, property, type);
    }


    public IFieldTranslator getTranslator(Class<?> owner, String property) {
        return translatorRegistry.getTranslator(owner, property);
    }


    class OAuthUser {

        /**
         * The id.
         */
        private final String id;

        /**
         * The issued_at.
         */
        private final Date issued;

        /**
         * The instance URL.
         */
        private final Reference instanceUrl;
        /**
         * The toke signature.
         */
        private final String signature;
        /**
         * The access token.
         */
        private final String accessToken;

        public OAuthUser(String id, Date issued, String instanceUrl, String signature, String accessToken) {
            this.id = id;
            this.issued = issued;
            this.instanceUrl = new Reference(instanceUrl);
            this.signature = signature;
            ChallengeWriter cw = new ChallengeWriter();
            cw.append(ChallengeScheme.HTTP_OAUTH.getTechnicalName()).appendSpace().append(accessToken);
            this.accessToken = cw.toString();
        }

        public Reference getBaseReference() {
            return instanceUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }
    }
}
