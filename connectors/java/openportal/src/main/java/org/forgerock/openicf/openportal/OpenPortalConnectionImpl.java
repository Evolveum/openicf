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
 * $Id$
 */
package org.forgerock.openicf.openportal;

import org.forgerock.openicf.openportal.util.ElementIdentifierType;
import org.forgerock.openicf.openportal.util.OpenPortalHandlerUtil;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.rpc.ServiceException;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import com.liferay.client.soap.portal.service.http.UserServiceSoap;
import com.liferay.client.soap.portal.service.http.UserServiceSoapServiceLocator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;

/**
 * Class to represent a OpenPortal Connection
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OpenPortalConnectionImpl implements OpenPortalConnection {

    private static final Log log = Log.getLog(OpenPortalConnectionImpl.class);
    private OpenPortalConfiguration _configuration;
    private UserServiceSoapServiceLocator locatorUser;
    private UserServiceSoap soapUser;
    private Schema schema;
    public OpenPortalConnectionImpl(OpenPortalConfiguration configuration) {
        _configuration = configuration;
        locatorUser = new UserServiceSoapServiceLocator();
        
        try {
            soapUser = locatorUser.getPortal_UserService(_configuration.getUrl());

            
        } catch (MalformedURLException ex) {
            Logger.getLogger(OpenPortalConnectionImpl.class.getName()).log(Level.SEVERE, null, ex);
        }catch (ServiceException ex) {
                Logger.getLogger(OpenPortalConnectionImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    public Uid create(ObjectClass objectClass, Set<Attribute> attributes) {
        final String method = "create";
        log.info("Entry {0}", method);
        //Validate object type
        ObjectClassInfo objInfo = schema.findObjectClassInfo(objectClass.getObjectClassValue());
        Set<AttributeInfo> objAttributes = null;
        Map<String, AttributeInfo> supportedAttributeInfoMap = null;
        Map<String, Attribute> providedAttributesMap = null;
        String uidValue = null;

        if(attributes != null){
            objAttributes = objInfo.getAttributeInfo();
            supportedAttributeInfoMap = new HashMap<String, AttributeInfo>(AttributeInfoUtil.toMap(objAttributes));
            providedAttributesMap = new HashMap<String, Attribute>(AttributeUtil.toMap(attributes));
        }

        //Check if Name is defined
        if(providedAttributesMap == null || !providedAttributesMap.containsKey(Name.NAME) || providedAttributesMap.get(Name.NAME).getValue().isEmpty()){
            throw new IllegalArgumentException(Name.NAME + " must be defined.");
        }

        Name name = AttributeUtil.getNameFromAttributes(attributes);

        //Check if entry already exists
        if(entryExists(objectClass, new Uid(name.getNameValue()), ElementIdentifierType.BY_NAME)){
            throw new AlreadyExistsException("Could not create entry. An entry with the " + Uid.NAME + " of "
                    + name.getNameValue() + " already exists.");
        }

        if(supportedAttributeInfoMap.containsKey(Uid.NAME)){
            uidValue = UUID.randomUUID().toString();
        } else {
            uidValue = name.getNameValue();
        }

        //Create object type element
       for(AttributeInfo attributeInfo : objAttributes){
           String attributeName = attributeInfo.getName();
           List<String> values = OpenPortalHandlerUtil.findAttributeValue(providedAttributesMap.get(attributeName), attributeInfo);
           if(attributeInfo.isRequired()){
               if(providedAttributesMap.containsKey(attributeName) && !values.isEmpty()){
                   for(String value : values){
                       Assertions.blankCheck(value, attributeName);
                   }
               }else{
                   throw new IllegalArgumentException("Missing required field: " + attributeName);
               }
           }
           if(attributeInfo.isMultiValued() && values.size() > 1){
               throw new IllegalArgumentException("Attribute field: " + attributeName + " is not multivalued and can not contain more than one value.");
           }
           if(!supportedAttributeInfoMap.containsKey(attributeName)){
               continue;
           }
           if(!attributeInfo.isCreateable() && providedAttributesMap.containsKey(attributeName)){
               throw new IllegalArgumentException(attributeName + " is not a creatable field.");
           }

          //Create the user...
       }

        return null;
    }

    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes) {
        return null;
    }

    public void delete(ObjectClass objectClass, Uid uid) {
    }

    public Collection<ConnectorObject> search(String query, ObjectClass objectClass) {
        return null;
    }

    public Uid authenticate(String userName, GuardedString password) {
        return null;
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
        //implementation
    }

    /**
     * Release internal resources
     */
    public void dispose() {
        //implementation
    }

    public Schema schema() {
        return null;
    }

    public Collection<ConnectorObject> allConnectorObjects(ObjectClass objectClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private boolean entryExists(ObjectClass objectClass, Uid uid, ElementIdentifierType elementIdentifierType) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
