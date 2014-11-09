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
 * 
 * Portions Copyrighted 2013 Forgerock
 */
package org.identityconnectors.ldap.modify;

import static org.identityconnectors.common.CollectionUtil.isEmpty;
import static org.identityconnectors.common.CollectionUtil.newSet;
import static org.identityconnectors.common.CollectionUtil.nullAsEmpty;
import static org.identityconnectors.ldap.LdapUtil.checkedListByFilter;
import static org.identityconnectors.ldap.LdapUtil.quietCreateLdapName;
import static org.identityconnectors.ldap.LdapUtil.escapeDNValueOfJNDIReservedChars;
import static org.identityconnectors.ldap.LdapUtil.normalizeLdapString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.NameAlreadyBoundException;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.ldap.GroupHelper;
import org.identityconnectors.ldap.LdapConnection;
import org.identityconnectors.ldap.LdapModifyOperation;
import org.identityconnectors.ldap.LdapConstants;
import org.identityconnectors.ldap.GroupHelper.GroupMembership;
import org.identityconnectors.ldap.GroupHelper.Modification;
import org.identityconnectors.ldap.LdapAuthenticate;
import org.identityconnectors.ldap.LdapUtil;
import org.identityconnectors.ldap.schema.GuardedPasswordAttribute;
import org.identityconnectors.ldap.schema.GuardedPasswordAttribute.Accessor;
import org.identityconnectors.ldap.search.LdapSearches;

public class LdapUpdate extends LdapModifyOperation {

    private final ObjectClass oclass;
    private final OperationOptions options;
    private final Uid uid;

    public LdapUpdate(LdapConnection conn, ObjectClass oclass, Uid uid, OperationOptions options) {
        super(conn);
        this.oclass = oclass;
        this.uid = uid;
        this.options = options;
    }

    public Uid update(Set<Attribute> attrs) {
        String entryDN = escapeDNValueOfJNDIReservedChars(LdapSearches.getEntryDN(conn, oclass, uid));
        PosixGroupMember posixMember = new PosixGroupMember(entryDN);
        LdapContext runAsContext = null;

        // Extract the Name attribute if any, to be used to rename the entry later.
        Set<Attribute> updateAttrs = attrs;
        Name newName = (Name) AttributeUtil.find(Name.NAME, attrs);
        String newEntryDN = null;
        if (newName != null) {
            updateAttrs = newSet(attrs);
            updateAttrs.remove(newName);
            newEntryDN = conn.getSchemaMapping().getEntryDN(oclass, newName);
        }

        List<String> ldapGroups = getStringListValue(updateAttrs, LdapConstants.LDAP_GROUPS_NAME);
        List<String> posixGroups = getStringListValue(updateAttrs, LdapConstants.POSIX_GROUPS_NAME);

        Pair<Attributes, GuardedPasswordAttribute> attrToModify = getAttributesToModify(updateAttrs);
        Attributes ldapAttrs = attrToModify.first;

        // If we are removing all POSIX ref attributes, check they are not used
        // in POSIX groups. Note it is OK to update the POSIX ref attribute instead of
        // removing them -- we will update the groups to refer to the new attributes.
        Set<String> newPosixRefAttrs = getAttributeValues(GroupHelper.getPosixRefAttribute(), quietCreateLdapName(newEntryDN != null ? newEntryDN : entryDN), ldapAttrs);
        if (newPosixRefAttrs != null && newPosixRefAttrs.isEmpty()) {
            checkRemovedPosixRefAttrs(posixMember.getPosixRefAttributes(), posixMember.getPosixGroupMemberships());
        }

        if (StringUtil.isNotBlank(options.getRunAsUser())) {
            String dn = new LdapAuthenticate(conn, oclass, options.getRunAsUser(), options).getDn();
            runAsContext = conn.getRunAsContext(dn, options.getRunWithPassword());
        }
        
        try {
            // Rename the entry if needed.
            String oldEntryDN = null;
            if ((newName != null) && (!normalizeLdapString(entryDN).equalsIgnoreCase(normalizeLdapString(newEntryDN)))) {
                if (newPosixRefAttrs != null && conn.getConfiguration().isMaintainPosixGroupMembership() || posixGroups != null) {
                    posixMember.getPosixRefAttributes();
                }
                oldEntryDN = entryDN;
                if (runAsContext == null) {
                    conn.getInitialContext().rename(oldEntryDN, newEntryDN);
                } else {
                    runAsContext.rename(oldEntryDN, newEntryDN);
                }
                entryDN = newEntryDN;
            }
            // Update the attributes.
            modifyAttributes(entryDN, attrToModify, DirContext.REPLACE_ATTRIBUTE, runAsContext);

            // Update the LDAP groups.
            Modification<GroupMembership> ldapGroupMod = new Modification<GroupMembership>();
            if (oldEntryDN != null && conn.getConfiguration().isMaintainLdapGroupMembership()) {
                Set<GroupMembership> members = groupHelper.getLdapGroupMemberships(oldEntryDN);
                ldapGroupMod.removeAll(members);
                for (GroupMembership member : members) {
                    ldapGroupMod.add(new GroupMembership(entryDN, member.getGroupDN()));
                }
            }
            if (ldapGroups != null) {
                Set<GroupMembership> members = groupHelper.getLdapGroupMemberships(entryDN);
                ldapGroupMod.removeAll(members);
                ldapGroupMod.clearAdded(); // Since we will be replacing with the new groups.
                for (String ldapGroup : ldapGroups) {
                    ldapGroupMod.add(new GroupMembership(entryDN, ldapGroup));
                }
            }
            groupHelper.modifyLdapGroupMemberships(ldapGroupMod, runAsContext);

            // Update the POSIX groups.
            Modification<GroupMembership> posixGroupMod = new Modification<GroupMembership>();
            if (newPosixRefAttrs != null && conn.getConfiguration().isMaintainPosixGroupMembership()) {
                Set<String> removedPosixRefAttrs = new HashSet<String>(posixMember.getPosixRefAttributes());
                removedPosixRefAttrs.removeAll(newPosixRefAttrs);
                Set<GroupMembership> members = posixMember.getPosixGroupMembershipsByAttrs(removedPosixRefAttrs);
                posixGroupMod.removeAll(members);
                if (!members.isEmpty()) {
                    String firstPosixRefAttr = getFirstPosixRefAttr(entryDN, newPosixRefAttrs);
                    for (GroupMembership member : members) {
                        posixGroupMod.add(new GroupMembership(firstPosixRefAttr, member.getGroupDN()));
                    }
                }
            }
            if (posixGroups != null) {
                Set<GroupMembership> members = posixMember.getPosixGroupMemberships();
                posixGroupMod.removeAll(members);
                posixGroupMod.clearAdded(); // Since we will be replacing with the new groups.
                if (!posixGroups.isEmpty()) {
                    String firstPosixRefAttr = getFirstPosixRefAttr(entryDN, newPosixRefAttrs);
                    for (String posixGroup : posixGroups) {
                        posixGroupMod.add(new GroupMembership(firstPosixRefAttr, posixGroup));
                    }
                }
            }
            groupHelper.modifyPosixGroupMemberships(posixGroupMod, runAsContext);
        } catch (NameAlreadyBoundException e) {
            throw new AlreadyExistsException(e);
        } catch (NamingException e) {
            throw new ConnectorException(e);
        } finally {
            if (runAsContext != null) {
                try {
                    runAsContext.close();
                } catch (NamingException e) {
                }
            }
        }

        return conn.getSchemaMapping().createUid(oclass, entryDN);
    }

    public Uid addAttributeValues(Set<Attribute> attrs) {
        String entryDN = LdapSearches.findEntryDN(conn, oclass, uid);
        PosixGroupMember posixMember = new PosixGroupMember(entryDN);
        LdapContext runAsContext = null;

        if (StringUtil.isNotBlank(options.getRunAsUser())) {
            String dn = new LdapAuthenticate(conn, oclass, options.getRunAsUser(), options).getDn();
            runAsContext = conn.getRunAsContext(dn, options.getRunWithPassword());
        }

        Pair<Attributes, GuardedPasswordAttribute> attrsToModify = getAttributesToModify(attrs);
        modifyAttributes(entryDN, attrsToModify, DirContext.ADD_ATTRIBUTE, runAsContext);

        List<String> ldapGroups = getStringListValue(attrs, LdapConstants.LDAP_GROUPS_NAME);
        if (!isEmpty(ldapGroups)) {
            groupHelper.addLdapGroupMemberships(entryDN, ldapGroups, runAsContext);
        }

        List<String> posixGroups = getStringListValue(attrs, LdapConstants.POSIX_GROUPS_NAME);
        if (!isEmpty(posixGroups)) {
            Set<String> posixRefAttrs = posixMember.getPosixRefAttributes();
            String posixRefAttr = getFirstPosixRefAttr(entryDN, posixRefAttrs);
            groupHelper.addPosixGroupMemberships(posixRefAttr, posixGroups, runAsContext);
        } 

        return uid;
    }

    public Uid removeAttributeValues(Set<Attribute> attrs) {
        String entryDN = LdapSearches.findEntryDN(conn, oclass, uid);
        PosixGroupMember posixMember = new PosixGroupMember(entryDN);
        LdapContext runAsContext = null;

        if (StringUtil.isNotBlank(options.getRunAsUser())) {
            String dn = new LdapAuthenticate(conn, oclass, options.getRunAsUser(), options).getDn();
            runAsContext = conn.getRunAsContext(dn, options.getRunWithPassword());
        }

        Pair<Attributes, GuardedPasswordAttribute> attrsToModify = getAttributesToModify(attrs);
        Attributes ldapAttrs = attrsToModify.first;

        Set<String> removedPosixRefAttrs = getAttributeValues(GroupHelper.getPosixRefAttribute(), null, ldapAttrs);
        if (!isEmpty(removedPosixRefAttrs)) {
            checkRemovedPosixRefAttrs(removedPosixRefAttrs, posixMember.getPosixGroupMemberships());
        }

        modifyAttributes(entryDN, attrsToModify, DirContext.REMOVE_ATTRIBUTE, runAsContext);

        List<String> ldapGroups = getStringListValue(attrs, LdapConstants.LDAP_GROUPS_NAME);
        if (!isEmpty(ldapGroups)) {
            groupHelper.removeLdapGroupMemberships(entryDN, ldapGroups, runAsContext);
        }

        List<String> posixGroups = getStringListValue(attrs, LdapConstants.POSIX_GROUPS_NAME);
        if (!isEmpty(posixGroups)) {
            Set<GroupMembership> members = posixMember.getPosixGroupMembershipsByGroups(posixGroups);
            groupHelper.removePosixGroupMemberships(members, runAsContext);
        }

        return uid;
    }

    private void checkRemovedPosixRefAttrs(Set<String> removedPosixRefAttrs, Set<GroupMembership> memberships) {
        for (GroupMembership membership : memberships) {
            if (removedPosixRefAttrs.contains(membership.getMemberRef())) {
                throw new ConnectorException(conn.format("cannotRemoveBecausePosixMember", GroupHelper.getPosixRefAttribute()));
            }
        }
    }

    private Pair<Attributes, GuardedPasswordAttribute> getAttributesToModify(Set<Attribute> attrs) {
        BasicAttributes ldapAttrs = new BasicAttributes();
        GuardedPasswordAttribute pwdAttr = null;
        for (Attribute attr : attrs) {
            javax.naming.directory.Attribute ldapAttr = null;
            if (attr.is(Uid.NAME)) {
                throw new IllegalArgumentException("Unable to modify an object's uid");
            } else if (attr.is(Name.NAME)) {
                // Such a change would have been handled in update() above.
                throw new IllegalArgumentException("Unable to modify an object's name");
            } else if (LdapConstants.isLdapGroups(attr.getName())) {
                // Handled elsewhere.
            } else if (LdapConstants.isPosixGroups(attr.getName())) {
                // Handled elsewhere.
            } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
                pwdAttr = conn.getSchemaMapping().encodePassword(oclass, attr);
            } else {
                ldapAttr = conn.getSchemaMapping().encodeAttribute(oclass, attr);
            }
            if (ldapAttr != null) {
                javax.naming.directory.Attribute existingAttr = ldapAttrs.get(ldapAttr.getID());
                if (existingAttr != null) {
                    try {
                        NamingEnumeration<?> all = ldapAttr.getAll();
                        while (all.hasMoreElements()) {
                            existingAttr.add(all.nextElement());
                        }
                    } catch (NamingException e) {
                        throw new ConnectorException(e);
                    }
                } else {
                    ldapAttrs.put(ldapAttr);
                }
            }
        }
        return new Pair<Attributes, GuardedPasswordAttribute>(ldapAttrs, pwdAttr);
    }

    private void modifyAttributes(final String entryDN, Pair<Attributes, GuardedPasswordAttribute> attrs, final int ldapModifyOp, final LdapContext context) {
        final List<ModificationItem> modItems = new ArrayList<ModificationItem>(attrs.first.size());
        NamingEnumeration<? extends javax.naming.directory.Attribute> attrEnum = attrs.first.getAll();
        while (attrEnum.hasMoreElements()) {
            modItems.add(new ModificationItem(ldapModifyOp, attrEnum.nextElement()));
        }

        if (attrs.second != null) {
            attrs.second.access(new Accessor() {
                public void access(javax.naming.directory.Attribute passwordAttr) {
                    hashPassword(passwordAttr, entryDN);
                    // Password self service - we assume user is not admin.
                    // and the target dn is the same as the "Run as" dn
                    if (context == null || !LdapUtil.isSameDistinguishedName(entryDN, context)) {
                        modItems.add(new ModificationItem(ldapModifyOp, passwordAttr));
                    } else {
                        // We may have different implementation of Password Self service depending on the target directory
                        switch (conn.getServerType()) {
                            case MSAD_LDS:
                            case MSAD:
                                // Password change has to be done in 2 operations. Remove old, Add new
                                BasicAttribute oldPasswordAttr = new BasicAttribute("unicodePwd", SecurityUtil.decrypt(options.getRunWithPassword()).getBytes());
                                hashPassword(oldPasswordAttr, entryDN);
                                modItems.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, oldPasswordAttr));
                                modItems.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, passwordAttr));
                                break;
                            default:
                                modItems.add(new ModificationItem(ldapModifyOp, passwordAttr));
                        }
                    }
                    modifyAttributes(entryDN, modItems, context);
                }
            });
        } else {
            modifyAttributes(entryDN, modItems, context);
        }
    }

    private void modifyAttributes(String entryDN, List<ModificationItem> modItems, LdapContext context) {
        try {
            if (context == null) {
                conn.getInitialContext().modifyAttributes(entryDN, modItems.toArray(new ModificationItem[modItems.size()]));
            } else {
                context.modifyAttributes(entryDN, modItems.toArray(new ModificationItem[modItems.size()]));
            }
        } catch (NameNotFoundException e) {
            throw (UnknownUidException) new UnknownUidException(uid, oclass).initCause(e);
        } catch (InvalidAttributeValueException e) {
            // Need to investigate the content of the error message
            String message = e.getMessage().toLowerCase();
            switch (conn.getServerType()) {
                case MSAD:
                case MSAD_LDS:
                    if (message.contains("ldap: error code 19 ")) {
                        if (message.contains("(unicodepwd)")) {
                            throw new ConnectorException("New password does not comply with password policy");
                        }
                    }
                    break;
                default:
            }
        } catch (NoPermissionException e) {
            throw new ConnectorException("Insufficient Access Rights to perform");
        } catch (NamingException e) {
            throw new ConnectorException(e);
        }
    }

    private List<String> getStringListValue(Set<Attribute> attrs, String attrName) {
        Attribute attr = AttributeUtil.find(attrName, attrs);
        if (attr != null) {
            return checkedListByFilter(nullAsEmpty(attr.getValue()), String.class);
        }
        return null;
    }
}
