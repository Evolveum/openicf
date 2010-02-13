/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.identityconnectors.googleapps.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.identityconnectors.common.EqualsHashCodeBuilder;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.googleapps.GoogleAppsConnector;

/**
 *
 * Todo: This is ugly. Find a better way to create a test account based
 * on the schema.
 *
 * Assumes the group connectorstest@identric.org exists
 * 
 * @author warrenstrange
 */
public class TestAccount {

 
 
    private String accountId;
    private String password;
    private String givenName;
    private String familyName;
    private Integer quota;
    private List<String> nicknames = new ArrayList<String>();
    private List<String> groups = new ArrayList<String>();;

    /**
     * Create a unique test account fully populated..
     * The account id must be unique to avoid google apps
     * recreate problems.
     */
    public TestAccount() {


        //String x = "test" + System.currentTimeMillis() +  r.nextInt(1000);
        String x = "test" + UUID.randomUUID();

        setGivenName("First");

        setFamilyName("Last");
        setPassword("test123");
        setAccountId(x);
        setAccountId(x);
        setQuota(new Integer(25600));
       
        getNicknames().add("testalias-" + x);
       
        
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPassword(GuardedString password) {
        this.password = getPlainPassword(password);
    }

    private String getPlainPassword(GuardedString password) {
        final StringBuffer buf = new StringBuffer();
        password.access(new GuardedString.Accessor() {

            public void access(char[] clearChars) {
                buf.append(clearChars);
            }
        });
        return buf.toString();
    }

    public Set<Attribute> toAttributeSet(boolean passwdColDefined) {
        Set<Attribute> ret = new HashSet<Attribute>();
        for (Map.Entry<String, Object> entry : toMap(passwdColDefined).entrySet()) {

            if (entry.getValue() instanceof Collection) {
                ret.add(AttributeBuilder.build(entry.getKey(), (Collection) entry.getValue()));
            } else {
                ret.add(AttributeBuilder.build(entry.getKey(), entry.getValue()));
            }
        }

        ret.add(AttributeBuilder.build(Name.NAME, getAccountId()));

        return ret;
    }

    public static TestAccount fromConnectorObject(ConnectorObject obj) {
        TestAccount t = new TestAccount();
        t.getNicknames().clear();
        t.getGroups().clear();

        for (Attribute attr : obj.getAttributes()) {
            String name = attr.getName();
            if (Name.NAME.equalsIgnoreCase(name)) {
                t.setAccountId(AttributeUtil.getStringValue(attr));
            } else if (GoogleAppsConnector.ATTR_GIVEN_NAME.equalsIgnoreCase(name)) {
                t.setGivenName(AttributeUtil.getStringValue(attr));
            } else if (GoogleAppsConnector.ATTR_FAMILY_NAME.equalsIgnoreCase(name)) {
                t.setFamilyName(AttributeUtil.getStringValue(attr));
            } else if (GoogleAppsConnector.ATTR_QUOTA.equalsIgnoreCase(name)) {
                t.setQuota(AttributeUtil.getIntegerValue(attr));
            } else if (GoogleAppsConnector.ATTR_NICKNAME_LIST.equalsIgnoreCase(name)) {
                // must cast list to array of strings
                List<String> nicks = t.getNicknames();
                for (Object o : attr.getValue()) {
                    nicks.add((String) o);
                }
            }
            else if (GoogleAppsConnector.ATTR_GROUP_LIST.equalsIgnoreCase(name)) {
                // must cast list to array of strings
                List<String> groups = t.getGroups();
                for (Object o : attr.getValue()) {
                    groups.add((String) o);
                }
            }
        }
        return t;
    }

    /**
     * @param passwdColDefined
     *            if it should include the password as an attribute
     */
    Map<String, Object> toMap(boolean passwdColDefined) {
        Map<String, Object> map = new HashMap<String, Object>();
        // todo: Do we want NAME in the attr, or just UID?

        map.put(Name.NAME, getAccountId());
        map.put(Uid.NAME, getAccountId());

        if (passwdColDefined) {
            if (getPassword() == null) {
                map.put(OperationalAttributes.PASSWORD_NAME, null);
            } else {
                map.put(OperationalAttributes.PASSWORD_NAME, new GuardedString(getPassword().toCharArray()));
            }
        } 

        map.put(GoogleAppsConnector.ATTR_GIVEN_NAME, getGivenName());
        map.put(GoogleAppsConnector.ATTR_FAMILY_NAME, getFamilyName());
        map.put(GoogleAppsConnector.ATTR_QUOTA, getQuota());
        map.put(GoogleAppsConnector.ATTR_NICKNAME_LIST, getNicknames());
        map.put(GoogleAppsConnector.ATTR_GROUP_LIST, getGroups());
        return map;
    }

    EqualsHashCodeBuilder getEqualsHashCodeBuilder() {
        EqualsHashCodeBuilder bld = new EqualsHashCodeBuilder();
        bld.appendBean(this);
        return bld;
    }

    @Override
    public String toString() {
        return toMap(true).toString();
    }

    @Override
    public int hashCode() {
        return getEqualsHashCodeBuilder().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean ret = false;
        if (obj instanceof TestAccount) {
            TestAccount tstObj = (TestAccount) obj;

            ret = tstObj.getAccountId().equals(accountId) && tstObj.getFamilyName().equals(familyName) && tstObj.getGivenName().equals(givenName) && tstObj.getNicknames().equals(getNicknames());
        }
        return ret;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public List<String> getNicknames() {
        return nicknames;
    }

    public void setNicknames(List<String> nicknames) {
        this.nicknames = nicknames;
    }

    /**
     * @return the groups
     */
    public List<String> getGroups() {
        return groups;
    }

    /**
     * @param groups the groups to set
     */
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}

