/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * U.S. Government Rights - Commercial software. Government users
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity
 * Connectors are trademarks or registered trademarks of Sun
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd.
 *
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 *
 * Portions Copyrighted 2012 ForgeRock Inc.
 *
 */
package org.forgerock.openicf.connectors.googleapps;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;

import com.google.gdata.client.appsforyourdomain.AppsGroupsService;
import com.google.gdata.data.appsforyourdomain.generic.GenericEntry;

/**
 * 
 * @author warrenstrange
 */
public class GoogleAppsGroupOps {

    private final GoogleAppsConnector gc;
    private static final Log log = Log.getLog(GoogleAppsGroupOps.class);

    GoogleAppsGroupOps(GoogleAppsConnector gc) {
        this.gc = gc;
    }

    /**
     * Create a group
     * 
     * @param name
     *            group name
     * @param a
     *            - attributes accessor
     * @return Uid of newly created group
     */
    Uid createGroup(Name name, AttributesAccessor a) {
        final String groupId = a.getName().getNameValue();
        final String groupName = a.findString(GoogleAppsConnector.ATTR_GROUP_TEXT_NAME);
        final String description = a.findString(PredefinedAttributes.DESCRIPTION);
        final String permissions = a.findString(GoogleAppsConnector.ATTR_GROUP_PERMISSIONS);

        final GoogleAppsClient g = gc.getClient();

        // log.info("Extracting Attrs {0}", attrs);

        final List<String> members = a.findStringList(GoogleAppsConnector.ATTR_MEMBER_LIST);
        final List<String> owners = a.findStringList(GoogleAppsConnector.ATTR_OWNER_LIST);

        log.info("Create group({0},{1},{2},{3})", groupId, groupName, description, permissions);

        g.createGroup(groupId, groupName, description, permissions);

        if (members != null) {
            for (String member : members) {
                log.info("Adding member {0} to group {1}", member, groupId);
                g.addGroupMember(groupId, member);
            }
        }

        if (owners != null) {
            for (String owner : owners) {
                log.info("Adding member {0} to group {1}", owner, groupId);
                g.addGroupOwner(groupId, owner);
            }
        }
        return new Uid(groupId);
    }

    void delete(String id) {
        GoogleAppsClient g = gc.getClient();
        g.deleteGroup(id);
    }

    void query(String query, ResultsHandler handler, OperationOptions ops) {
        GoogleAppsClient g = gc.getClient();
        boolean fetchMembers = false; // by default members and owners are not
                                      // fetched
        boolean fetchOwners = false;

        if (ops != null) {
            String attrs[] = ops.getAttributesToGet();

            if (attrs != null) {
                List<String> alist = Arrays.asList(attrs);

                if (alist.contains(GoogleAppsConnector.ATTR_MEMBER_LIST))
                    fetchMembers = true;
                if (alist.contains(GoogleAppsConnector.ATTR_OWNER_LIST))
                    fetchOwners = true;
            }
        }

        if (query == null) { // return all groups;
            log.info("Fetching All Groups");
            Iterator i = g.getGroupIterator();
            while (i.hasNext()) {
                GenericEntry ge = (GenericEntry) i.next();
                List<String> members = null;
                List<String> owners = null;
                String groupId = ge.getProperty(AppsGroupsService.APPS_PROP_GROUP_ID);
                if (fetchMembers) {
                    members = g.getMembersAsList(groupId);
                }
                if (fetchOwners) {
                    owners = g.getOwnersAsList(groupId);
                }
                handler.handle(makeConnectorObject(ge, members, owners));

            }
        } else { // get a single group
            ConnectorObject obj = getGroup(query, fetchMembers, fetchOwners);
            log.info("ConnectorObj {0}", obj);
            if (obj != null) {
                handler.handle(obj);
            }
        }
    }

    Uid updateGroup(Uid uid, Set<Attribute> replaceAttrs, OperationOptions options) {
        AttributesAccessor a = new AttributesAccessor(replaceAttrs);
        final String groupId = uid.getUidValue();
        final String groupName = a.findString(GoogleAppsConnector.ATTR_GROUP_TEXT_NAME);
        final String description = a.findString(PredefinedAttributes.DESCRIPTION);
        final String permissions = a.findString(GoogleAppsConnector.ATTR_GROUP_PERMISSIONS);

        final GoogleAppsClient g = gc.getClient();
        final List<String> members = a.findStringList(GoogleAppsConnector.ATTR_MEMBER_LIST);
        final List<String> owners = a.findStringList(GoogleAppsConnector.ATTR_OWNER_LIST);

        log.info("updating group id:{0} name:{1} description: {2} permissions: {3}", groupId,
                groupName, description, permissions);
        g.updateGroup(groupId, groupName, description, permissions);

        // update group membership
        if (members != null) {
            List<String> currentMembers = g.getMembersAsList(groupId);

            log.info("Existing groups for group {0} are: {1}", groupId, currentMembers);

            new ChangeSetExecutor(currentMembers, members) {
                @Override
                public void doAdd(String user) {
                    log.info("Adding user {0}", user);
                    g.addGroupMember(groupId, user);

                }

                @Override
                public void doRemove(String user) {
                    log.info("Removing user {0}", user);
                    g.removeGroupMember(groupId, user);
                }
            }.execute();
        }

        // update owners
        if (owners != null) {
            List<String> currentOwners = g.getMembersAsList(groupId);

            log.info("Existing groups for group {0} are: {1}", groupId, currentOwners);

            new ChangeSetExecutor(currentOwners, owners) {
                @Override
                public void doAdd(String user) {
                    log.info("Adding user {0}", user);
                    g.addGroupOwner(groupId, user);

                }

                @Override
                public void doRemove(String user) {
                    log.info("Removing user {0}", user);
                    g.removeGroupOwner(groupId, user);
                }
            }.execute();
        }

        return uid;
    }

    /**
     * Given a google apps group entry, create a ConnectorObject.
     * 
     */
    private ConnectorObject makeConnectorObject(GenericEntry ge, List<String> members,
            List<String> owners) {

        if (ge == null) {
            return null;
        }

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

        builder.setObjectClass(ObjectClass.GROUP);

        builder.setUid(ge.getProperty(AppsGroupsService.APPS_PROP_GROUP_ID));
        builder.setName(ge.getProperty(AppsGroupsService.APPS_PROP_GROUP_ID));

        String d = ge.getProperty(AppsGroupsService.APPS_PROP_GROUP_DESC);
        String p = ge.getProperty(AppsGroupsService.APPS_PROP_GROUP_PERMISSION);
        String n = ge.getProperty(AppsGroupsService.APPS_PROP_GROUP_NAME);

        builder.addAttribute(AttributeBuilder.build(PredefinedAttributes.DESCRIPTION, d));
        builder.addAttribute(AttributeBuilder.build(GoogleAppsConnector.ATTR_GROUP_TEXT_NAME, n));
        builder.addAttribute(AttributeBuilder.build(GoogleAppsConnector.ATTR_GROUP_PERMISSIONS, p));

        if (owners != null) {
            builder.addAttribute(GoogleAppsConnector.ATTR_OWNER_LIST, owners);
        }
        if (members != null) {
            builder.addAttribute(GoogleAppsConnector.ATTR_MEMBER_LIST, members);
        }

        return builder.build();
    }

    /**
     * Retrive the group with the given id
     * 
     * @param id
     *            - the id for the group
     * @return The user object if it exists, null otherwise
     */
    private ConnectorObject getGroup(String id, boolean fetchMembers, boolean fetchOwners) {
        GenericEntry ge = null;

        GoogleAppsClient g = gc.getClient();

        log.info("Fetching google apps group {0}", id);
        ge = g.getGroupEntry(id);

        if (ge != null) {
            List<String> members = null;
            List<String> owners = null;
            if (fetchMembers) {
                members = g.getMembersAsList(id);
            }
            if (fetchOwners) {
                owners = g.getOwnersAsList(id);
            }

            return makeConnectorObject(ge, members, owners);
        }
        return null;
    }
}
