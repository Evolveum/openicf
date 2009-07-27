package org.identityconnectors.racf;

import static org.identityconnectors.racf.RacfConstants.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.BeforeClass;
import org.junit.Test;

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
        config.setUserObjectClasses(new String[]{"racfUser", "SAFTsoSegment"});
        config.setGroupObjectClasses(new String[]{"racfGroup"});

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
        return ATTR_LDAP_CONNECT_OWNER;
    }
    protected String getTsoSizeName(){
        return ATTR_LDAP_TSO_LOGON_SIZE;
    }
    protected Uid makeUid(String name, ObjectClass objectClass) {
        return new Uid("racfid="+name+",profileType="+(objectClass.is(ObjectClass.ACCOUNT_NAME)?"USER,":"GROUP,")+SUFFIX);
    }
    
    @Test//@Ignore
    public void testModifyUser() throws Exception {
        RacfConfiguration config = createConfiguration();
        RacfConnector connector = createConnector(config);
        try {
            displayConnectorObject(getUser(makeUid("IDM01", ObjectClass.ACCOUNT).getUidValue(), connector));
//            displayUser(getUser("CICSUSER", connector));
            // Delete the user
            deleteUser(TEST_USER_UID, connector);
    
            Set<Attribute> attrs = fillInSampleUser(TEST_USER);
            connector.create(ObjectClass.ACCOUNT, attrs, null);
            ConnectorObject user = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                changed.add(AttributeBuilder.build(getInstallationDataAttributeName(), "modified data"));
                List<Object> attributes = new LinkedList<Object>();
                attributes.add("SPECIAL");
                attributes.add("OPERATIONS");
                Attribute attributesAttr = AttributeBuilder.build(getAttributesAttributeName(), attributes);
                changed.add(attributesAttr);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
                assertAttribute(attributesAttr, object);
            }
            if (false) // temporarily disable test
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                Attribute disableDate = AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, new Date("11/12/2010").getTime());
                changed.add(disableDate);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
                assertAttribute(disableDate, object);
            }
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                Attribute size = AttributeBuilder.build(getTsoSizeName(), Integer.valueOf(1000)); 
                changed.add(size);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector, new String[] {getTsoSizeName()});
                assertAttribute(size, object);
            }
            if (false) // temporarily disable test
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                Attribute enableDate = AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, new Date("11/15/2010").getTime());
                changed.add(AttributeBuilder.build(getInstallationDataAttributeName(), "modified data"));
                changed.add(enableDate);
                changed.add(user.getUid());
                connector.update(ObjectClass.ACCOUNT, changed, null);
                ConnectorObject object = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
                assertAttribute(enableDate, object);
            }
            {
                Set<Attribute> changed = new HashSet<Attribute>();
                //
                changed.add(AttributeBuilder.build(getInstallationDataAttributeName(), "modified data"));
                List<Object> attributes = new LinkedList<Object>();
                attributes.add("SPECIAL");
                attributes.add("OPERATOR");
                changed.add(AttributeBuilder.build(getAttributesAttributeName(), attributes));
                changed.add(user.getUid());
                try {
                    connector.update(ObjectClass.ACCOUNT, changed, null);
                    Assert.fail("Command should have failed");
                } catch (IllegalArgumentException ce) {
                    System.out.println(ce);
                } catch (ConnectorException ce) {
                    System.out.println(ce);
                }
            }
    
            ConnectorObject changedUser = getUser(makeUid(TEST_USER, ObjectClass.ACCOUNT).getUidValue(), connector);
            //Attribute racfInstallationData = changedUser.getAttributeByName("racfinstallationdata");
            Attribute racfInstallationData = changedUser.getAttributeByName(getInstallationDataAttributeName());
            displayConnectorObject(changedUser);
            Assert.assertTrue(AttributeUtil.getStringValue(racfInstallationData).trim().equalsIgnoreCase("modified data"));
            displayConnectorObject(getUser(makeUid("IDM01", ObjectClass.ACCOUNT).getUidValue(), connector));
            displayConnectorObject(getUser(makeUid("IDM01", ObjectClass.ACCOUNT).getUidValue(), connector));
        } finally {
            connector.dispose();
        }
    }

}
