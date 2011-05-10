/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * -----------
 *
 */

/* TestNG Connector configuration */

HOST_NAME="__configureme__"
SYSTEM_PASSWORD="__configureme__"
SYSTEM_PASSWORD2="__configureme__"
SUFFIX="__configureme__"
SYSTEM_USER="__configureme__"
SYSTEM_USER2="__configureme__"



/* +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */

import org.identityconnectors.contract.data.groovy.Lazy
import org.identityconnectors.common.security.GuardedString


testsuite {
    // path to bundle jar - property is set by TestNG - leave it as it is
    bundleJar=System.getProperty("bundleJar")
    bundleName=System.getProperty("bundleName")
    bundleVersion=System.getProperty("bundleVersion")
    connectorName="org.identityconnectors.racf.RacfConnector"    

 
} // testsuite

connector {
/*
    //Shared Configuration
	hostNameOrIpAddr="__configureme__"
	useSsl=Boolean.FALSE
	hostTelnetPortNumber=23
	
	//CommandLine Configuration
	userNames=new String[] {"__configureme__"}
	passwords=new GuardedString[] {new GuardedString("__configureme__".toCharArray())}
	segmentNames=new String[] {"ACCOUNT.RACF","ACCOUNT.TSO","ACCOUNT.NETVIEW","ACCOUNT.CICS","ACCOUNT.OMVS","ACCOUNT.CATALOG","ACCOUNT.OMVS","GROUP.RACF" })
	connectionClassName="org.identityconnectors.rw3270.hod.HodConnection"
	
	//LDAP Configuration
	userObjectClasses=new String[]{"racfUser", "SAFTsoSegment"}
	groupObjectClasses=new String[]{"racfGroup"}
	suffix="__configureme__"
	LdapPassword=new GuardedString("__configureme__".toCharArray()
	ldapUserName="__configureme__"
	activeSyncPrivateKey=new String[] {
	                "-----BEGIN RSA PRIVATE KEY-----",
	                "MIIBOwIBAAJBAJqhUhK9V3s1ebqdFtxbXEvsxZa6m75y11p9qSJHgysNzK1wkSjz",
	                "xAufs1zcOptQcSSNG/tjt1BPUKt5SE4z2WkCAwEAAQJAOPYgU8LoDP0gAHyJxVbq",
	                "YxWvm9zWLowDhNQxj+0kBqGWGoRZOxgY1MdJv8mrnq3JnzfxlPcIuiPoVELeM2Kg",
	                "uQIhAMuiAuSIHnuQgZFRXolQ4G626VI7MzYwJCC+u/VMxsEjAiEAwmVClSg1wimN",
	                "ENANMO/oUYXdICnBcS+kyb5YZCOKcgMCIQDFM3lPrc6vZStE+qLtoigmr/ZWj0Qy",
	                "Bv8FwxCtJpQYNwIhAKlzKPnpxgqMu6lnIciBp2nAnUMXAscN97/fyx7nGBxPAiAM",
	                "uYmFLvmZg6MevmsCNl+KjZ4vNAO2SHrvgjFaZoC7tw==",
	                "-----END RSA PRIVATE KEY-----",
	        }
	activeSyncCertificate=new String[] {
	                "-----BEGIN CERTIFICATE-----",
	                "MIICVjCCAgCgAwIBAgIJAPsvnrb/wsffMA0GCSqGSIb3DQEBBAUAMFMxCzAJBgNV",
	                "BAYTAmZyMRMwEQYDVQQIEwpTb21lLVN0YXRlMQ8wDQYDVQQHEwZSZW5uZXMxEDAO",
	                "BgNVBAoTB2V4ZW1wbGUxDDAKBgNVBAMTA0lkbTAeFw0wNzAzMTMxOTM1MjNaFw0x",
	                "MDAzMTIxOTM1MjNaMFMxCzAJBgNVBAYTAmZyMRMwEQYDVQQIEwpTb21lLVN0YXRl",
	                "MQ8wDQYDVQQHEwZSZW5uZXMxEDAOBgNVBAoTB2V4ZW1wbGUxDDAKBgNVBAMTA0lk",
	                "bTBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQCaoVISvVd7NXm6nRbcW1xL7MWWupu+",
	                "ctdafakiR4MrDcytcJEo88QLn7Nc3DqbUHEkjRv7Y7dQT1CreUhOM9lpAgMBAAGj",
	                "gbYwgbMwHQYDVR0OBBYEFIpNz/LDpZXCZJad9kr4tBT9E3mBMIGDBgNVHSMEfDB6",
	                "gBSKTc/yw6WVwmSWnfZK+LQU/RN5gaFXpFUwUzELMAkGA1UEBhMCZnIxEzARBgNV",
	                "BAgTClNvbWUtU3RhdGUxDzANBgNVBAcTBlJlbm5lczEQMA4GA1UEChMHZXhlbXBs",
	                "ZTEMMAoGA1UEAxMDSWRtggkA+y+etv/Cx98wDAYDVR0TBAUwAwEB/zANBgkqhkiG",
	                "9w0BAQQFAANBAEc+BJtYMMn2Owmgt3w7lpUnrAPXHVyGsijK5k/cn0qqqkMDlBzq",
	                "/YiOz5RLMjhmH51rxn8E6jChoJ7i5JrHZa4=",
	                "-----END CERTIFICATE-----",
	        }
*/		
}
