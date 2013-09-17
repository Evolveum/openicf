/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013. ForgeRock Inc. All rights reserved.
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
 *
 */

/* +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+
 */
import org.identityconnectors.contract.data.groovy.Lazy
import org.identityconnectors.common.security.GuardedString

// Connector WRONG configuration for ValidateApiOpTests
connector.i1.wrong.host=""
connector.i2.wrong.login=""
connector.i3.wrong.password=new GuardedString("".toCharArray())


configuration{
    // TODO
    ssl = false
    host="SECDEVCGY003.network.dev:7002"
    remoteUser="CmdClient_s2kxf5af"
    password=new GuardedString("juhhGiR08jRKZqo8tzO9s5BZ7cgooi".toCharArray())
}

testsuite {
    // path to bundle jar - property is set by ant - leave it as it is
    bundleJar=System.getProperty("bundleJar")
    bundleName=System.getProperty("bundleName")
    bundleVersion=System.getProperty("bundleVersion")
    connectorName=System.getProperty("org.identityconnectors.rsaconnector")

    // ValidateApiOpTests:
    Validate.iterations="3"

    // AuthenticationApiOpTests:
    Authentication.__ACCOUNT__.username=Lazy.get("CmdClient_s2kxf5af")
    Authentication.__ACCOUNT__.wrong.password=new GuardedString("bogus".toCharArray())
} // testsuite

connector {
    // Connector Properties
    // with Suncor's DEV env
    InitialNamingFactory = "weblogic.jndi.WLInitialContextFactory";
    NamingProviderUrl = "t3s://SECDEVCGY003.network.dev:7002";  //e.g., "t3s://local1:7002"
    CmdclientUser = "CmdClient_s2kxf5af";
    GuardedString CmdClientUserPwd = new GuardedString("juhhGiR08jRKZqo8tzO9s5BZ7cgooi".toCharArray());
    GuardedString RsaSslClientIdStorePwd = null;
    GuardedString RsaSslClientIdKeyPwd = null;
    ImsSslClientProviderUrl = null;  // e.g., t3s://local1:7022
    ImsSslClientIdentityKeystoreFilename = null; // e.g., "client-identity.jks"
    ImsSslClientIdentityKeyAlias = "client-identity";
    ImsSslClientRootCaAlias = "root-ca";
    ImsSoapClientProviderUrl = null; // e.g., "https://local1:7002/ims-ws/services/CommandServer"
    ImsHttpinvokerClientProviderUrl = "https://SECDEVCGY003.network.dev:7002/ims-ws/httpinvoker/CommandServer"
}

HOST="0.0.0.0"