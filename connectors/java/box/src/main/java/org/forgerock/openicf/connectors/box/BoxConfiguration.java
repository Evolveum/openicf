/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
 */
package org.forgerock.openicf.connectors.box;

import static org.identityconnectors.common.security.SecurityUtil.decrypt;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.authorization.OAuthWebViewData;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import sun.plugin.dom.exception.InvalidStateException;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Box Connector.
 * 
 */
public class BoxConfiguration extends AbstractConfiguration implements StatefulConfiguration {

    /**
     * Setup logging for the {@link BoxConfiguration}.
     */
    private static final Log logger = Log.getLog(BoxConfiguration.class);

    /**
     * Client Id of the application registered at Box.com
     */
    private String clientId;

    /**
     * Client Secret of the application registered at Box.com
     */
    private GuardedString clientSecret;

    private GuardedString refreshToken;

    private BoxClient boxClient;

    @ConfigurationProperty(order = 1, displayMessageKey = "clientId.display",
            groupMessageKey = "basic.group", helpMessageKey = "clientId.help", required = true)
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "clientSecret.display",
            groupMessageKey = "basic.group", helpMessageKey = "clientSecret.help",
            confidential = true, required = true)
    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString secret) {
        this.clientSecret = secret;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "refreshToken.display",
            groupMessageKey = "basic.group", helpMessageKey = "refreshToken.help",
            confidential = true, required = true)
    public GuardedString getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(GuardedString refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        Assertions.blankCheck(getClientId(), "clientId");
        Assertions.nullCheck(getClientSecret(), "clientSecret");
        Assertions.nullCheck(getRefreshToken(), "refreshToken");
    }

    // **** THIS MUST BE THREAD SAFE !!!

    BoxClient getBoxClient() {
        if (null == this.boxClient) {
            synchronized (this) {
                try {
                    boxClient =
                            new BoxClient(getClientId(), decrypt(getClientSecret()), null, null,
                                    null);

                    Map<String, Object> map = new HashMap<String, Object>(1);
                    map.put(BoxOAuthToken.FIELD_REFRESH_TOKEN, decrypt(getRefreshToken()));

                    boxClient.getOAuthDataController().setOAuthData(new BoxOAuthToken(map));
                    boxClient.getOAuthDataController().refresh();

                    if (boxClient.isAuthenticated()) {
                        logger.info("Client is authenticated");
                    } else {
                        logger.error("Client is NOT authenticated");
                    }

                } catch (AuthFatalFailureException e) {
                    logger.error(e.getMessage(), e);
                    throw new IllegalStateException(e.getMessage(), boxClient
                            .getOAuthDataController().getRefreshFailException());
                } catch (final Exception e) {
                    logger.error(e, "Cannot get connection to the box service.");
                    return null;
                }
            }
        }
        return boxClient;

    }

    @Override
    public void release() {
        synchronized (this) {
            if (null != boxClient) {
                boxClient = null;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 3) {

            BoxClient client = new BoxClient(args[0], args[1], null, null, null);

            OAuthWebViewData webView = new OAuthWebViewData(client.getOAuthDataController());
            webView.setRedirectUrl(args[2]);

            browse(webView.buildUrl());

            String code;
            if (true) {
                code = getCode();
            } else {
                do {
                    System.out.print("Please enter code: ");
                    code = new Scanner(System.in).nextLine();
                } while (code.isEmpty());

            }
            BoxOAuthToken oauth =
                    client.getOAuthManager().createOAuth(code, webView.getClientId(),
                            webView.getClientSecret(), webView.getRedirectUrl());

            System.out.println(client.getJSONParser().convertBoxObjectToJSONStringQuietly(oauth));
        } else {
            System.err.println("Usage: box [client_id] [client_secret] [redirect_uri]");
        }
    }

    /**
     * Open a browser at the given URL using {@link Desktop} if available, or
     * alternatively output the URL to {@link System#out} for command-line
     * applications.
     * 
     * @param uri
     *            URL to browse
     */
    public static void browse(URI uri) {
        System.out.println("Please open the following address in your browser:");
        System.out.println("  " + uri);
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    System.out
                            .println("Attempting to open that address in the default browser now...");
                    desktop.browse(uri);
                }
            }
        } catch (IOException e) {
            logger.warn("Unable to open browser", e);
        } catch (InternalError e) {
            logger.warn("Unable to open browser", e);
        }
    }

    private static String getCode() throws IOException {
        ServerSocket serverSocket = new ServerSocket(8088);
        Socket socket = serverSocket.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while (true) {
            String code = "";
            try {
                BufferedWriter out =
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Content-Type: text/html\r\n");
                out.write("\r\n");

                code = in.readLine();
                String match = "code";
                int loc = code.indexOf(match);

                if (loc > 0) {
                    int httpstr = code.indexOf("HTTP") - 1;
                    code = code.substring(code.indexOf(match), httpstr);
                    String parts[] = code.split("=");
                    code = parts[1];
                    out.write(String.format(HTML_PAGE, "", code));
                } else {
                    out.write(String.format(HTML_PAGE, "not found in the URL!", ""));
                }
                out.close();
                return code;
            } catch (IOException e) {
                System.exit(1);
                break;
            }
        }
        return "";
    }

    private static final String HTML_PAGE =
            "<!DOCTYPE html> <head> <title>OpenICF Box Connector OAuth2 Code | Box</title> <link rel=\"stylesheet\" href=\"https://e1.boxcdn.net/_assets/css/section_templ_login_views_components_center_container-aXC4sq.css\" media=\"screen\"> </head> <body id=\"site_body\"> <div id=\"envelope-background\"> <div class=\"center_container single-width\"> <div class=\"container_body\"> <div class=\"container_header ptl\"> <div class=\"title-logo sprite_signup_login_box_logo\"> </div> <div class=\"title_text\">OAuth2 Code %s</div> <div class=\"title_subtext small pts\"><strong>%s</strong></div> <div class=\"title_subtext small pts\">Now return to command line to see the output of the Box.com Connector Configuration.</div> </div> </div> </div> </div> </body></html>";
}
