/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */

package org.identityconnectors.peoplesoft.compintfc.mapping.idm;

import java.util.*;

import org.identityconnectors.common.*;
import org.identityconnectors.peoplesoft.compintfc.*;
import org.identityconnectors.peoplesoft.compintfc.mapping.*;
import org.junit.*;

/**
 * @author kitko
 *
 */
public class IDMComponentInterfacesFactoryTest {

    /**
     * Test method for {@link org.identityconnectors.peoplesoft.compintfc.mapping.idm.IDMComponentInterfacesFactory#createMapping(org.identityconnectors.peoplesoft.compintfc.PeopleSoftCompIntfcConfiguration)}.
     */
    @Test
    public void testCreateMapping() {
        PeopleSoftCompIntfcConfiguration cfg = new PeopleSoftCompIntfcConfiguration();
        String xml = IOUtil.getResourceAsString(getClass(), "PeopleSoftComponentInterfaces.xml");
        if(xml == null){
            throw new IllegalArgumentException("Cannot load PeopleSoftComponentInterfaces.xml");
        }
        cfg.setXMLMapping(xml);
        ComponentInterfaces mapping = new IDMComponentInterfacesFactory().createMapping(cfg);
        Assert.assertNotNull("ComponentInterfaces is null after parse", mapping);
        Assert.assertEquals("Interface names do not match",CollectionUtil.newSet("USER_PROFILE_8_1X","USER_PROFILE_8_4X","DELETE_USER_PROFILE","ROLE_MAINT"), mapping.getInterfaceNames());
        assert81x(mapping);
        assertRoleMaint(mapping);
    }

    private void assertRoleMaint(ComponentInterfaces mapping) {
        ComponentInterface roleMaint = mapping.getInterface("ROLE_MAINT");
        Assert.assertNotNull(roleMaint);
        Assert.assertNull("UserID", roleMaint.getCreateKey());
        Assert.assertEquals("ROLENAME", roleMaint.getFindKey());
        Assert.assertEquals("ROLENAME", roleMaint.getGetKey());
        Assert.assertEquals("ROLE_MAINT", roleMaint.getInterfaceName());
        List<Property> properties = new ArrayList<Property>();
        properties.add(new SingleProperty("DESCR"));
        properties.add(new SingleProperty("ROLESTATUS"));
        Assert.assertEquals(properties, roleMaint.getProperties());
        SupportedObjectTypes supportedObjectTypes = 
            new SupportedObjectTypes.Builder().addFeautures("Role", new SupportedObjectTypes.Feautures.Builder().addFeature("find").addFeature("get").build()).build();
        Assert.assertEquals(supportedObjectTypes, roleMaint.getSupportedObjectTypes());
        ComponentInterface tRoleMaint = new ComponentInterface.Builder().setFindKey("ROLENAME").setGetKey("ROLENAME").setInterfaceName("ROLE_MAINT")
                                        .addProperties(properties).setSupportedObjectTypes(supportedObjectTypes).build();
        Assert.assertEquals("Role main interface does not match",tRoleMaint, roleMaint);
    }

    private void assert81x(ComponentInterfaces mapping) {
        ComponentInterface i81x = mapping.getInterface("USER_PROFILE_8_1X");
        Assert.assertNotNull(i81x);
        Assert.assertEquals("UserID", i81x.getCreateKey());
        Assert.assertEquals("UserID", i81x.getFindKey());
        Assert.assertEquals("UserID", i81x.getGetKey());
        Assert.assertEquals("USER_PROFILE", i81x.getInterfaceName());
        Assert.assertNull(i81x.getSupportedObjectTypes());
        Assert.assertNotNull(i81x.getDisableRule());
        DisableRule disabledRule = new DisableRule.Builder().setFalseValue("0.0").setTrueValue("1.0").setName("AccountLocked").build();
        Assert.assertEquals(disabledRule,i81x.getDisableRule());
        List<Property> properties = createI81xProperties();
        Assert.assertEquals("Properties do not match",properties,i81x.getProperties());
        
        ComponentInterface t81x = new ComponentInterface.Builder().setCreateKey("UserID").setFindKey("UserID").setGetKey("UserID").setInterfaceName(
                "USER_PROFILE").setDisableRule(disabledRule).addProperties(properties).build();
        Assert.assertEquals("Interfaces do not match",t81x, i81x);
    }
    
    

    private List<Property> createI81xProperties() {
        List<Property> properties = new ArrayList<Property>();
        properties.add(new SingleProperty("AccountLocked"));
        properties.add(new SingleProperty("AlternateUserID"));
        properties.add(new SingleProperty("CurrencyCode"));
        properties.add(new SingleProperty("EmailAddress"));
        CollectionProperty.Builder c1 = new CollectionProperty.Builder();
        c1.setName("IDTypes").setKey("IDType").addProperty(new SingleProperty("IDType"));
        CollectionProperty c2 = new CollectionProperty.Builder().setName("Attributes").setKey("AttributeName")
                                .addProperty(new SingleProperty("AttributeName")).addProperty(new SingleProperty("AttributeValue")).build();
        c1.addProperty(c2);
        properties.add(c1.build());
        properties.add(new SingleProperty("LanguageCode"));
        properties.add(new SingleProperty("MultiLanguageEnabled"));
        properties.add(new SingleProperty("NavigatorHomePermissionList"));
        properties.add(new SingleProperty("PrimaryPermissionList"));
        properties.add(new SingleProperty("ProcessProfilePermissionList"));
        properties.add(new SingleProperty("ReassignWork"));
        properties.add(new SingleProperty("ReassignUserID"));
        properties.add(new CollectionProperty.Builder().setName("Roles").setKey("RoleName").addProperty(new SingleProperty("RoleName")).build());
        properties.add(new SingleProperty("RowSecurityPermissionList"));
        properties.add(new SingleProperty("SupervisingUserID"));
        properties.add(new SingleProperty("SymbolicID"));
        properties.add(new SingleProperty("UserDescription"));
        return properties;
    }

}
