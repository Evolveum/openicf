package org.identityconnectors.racf;

import static org.identityconnectors.racf.RacfConstants.*;

import java.io.IOException;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.BeforeClass;

public class RacfLdapConnectorTests extends RacfConnectorTestBase {
    public static void main(String[] args) {
        RacfLdapConnectorTests tests = new RacfLdapConnectorTests();
        try {
            tests.testCreate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @BeforeClass
    public static void beforeClass() {
        HOST_NAME         = TestHelpers.getProperty("LDAP_HOST_NAME", null);
        SYSTEM_PASSWORD   = TestHelpers.getProperty("LDAP_SYSTEM_PASSWORD", null);
        SUFFIX            = TestHelpers.getProperty("LDAP_SUFFIX", null);
        SYSTEM_USER       = TestHelpers.getProperty("LDAP_SYSTEM_USER", null);
       
        SYSTEM_USER_LDAP  = "racfid="+SYSTEM_USER+",profileType=user,"+SUFFIX;
        
        Assert.assertNotNull("HOST_NAME must be specified", HOST_NAME);
        Assert.assertNotNull("SYSTEM_PASSWORD must be specified", SYSTEM_PASSWORD);
        Assert.assertNotNull("SYSTEM_USER must be specified", SYSTEM_USER);
        Assert.assertNotNull("SUFFIX must be specified", SUFFIX);
    }

    protected void initializeCommandLineConfiguration(RacfConfiguration config) throws IOException {
    }
    
    protected void initializeLdapConfiguration(RacfConfiguration config) {
        config.setSupportedSegments(RacfConfiguration.SEGMENT_TSO);
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setUseSsl(USE_SSL);
        config.setHostPortNumber(HOST_LDAP_PORT);
        config.setSuffix(SUFFIX);
        config.setLdapPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setLdapUserName(SYSTEM_USER_LDAP);
    }

    protected String getInstallationDataAttributeName() {
        return ATTR_LDAP_DATA;
    }
    protected String getDefaultGroupName() {
        return ATTR_LDAP_DEFAULT_GROUP;
    }
    protected String getAttributesAttributeName() {
        return ATTR_LDAP_ATTRIBUTES;
    }
    protected String getOwnerAttributeName(){
        return ATTR_LDAP_OWNER;
    }
    protected String getSupgroupAttributeName(){
        return ATTR_LDAP_SUP_GROUP;
    }
    protected String getGroupMembersAttributeName(){
        return ATTR_LDAP_GROUP_USERIDS;
    }
    protected String getGroupsAttributeName(){
        return ATTR_LDAP_GROUPS;
    }
    protected String getGroupConnOwnersAttributeName(){
        return ATTR_LDAP_GROUP_OWNERS;
    }
    protected String getTsoSizeName(){
        return ATTR_LDAP_TSO_LOGON_SIZE;
    }
    protected Uid makeUid(String name, ObjectClass objectClass) {
        return new Uid("racfid="+name+",profileType="+(objectClass.is(ObjectClass.ACCOUNT_NAME)?"USER":"GROUP")+","+"dc=zos1578,dc=exemple,dc=fr");
    }

}
