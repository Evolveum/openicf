/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 *
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */
package org.identityconnectors.solaris.mode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.AttrUtil;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.CommandSwitches;
import org.identityconnectors.solaris.operation.search.AuthsCommand;
import org.identityconnectors.solaris.operation.search.LastCommand;
import org.identityconnectors.solaris.operation.search.LoginsCommand;
import org.identityconnectors.solaris.operation.search.ProfilesCommand;
import org.identityconnectors.solaris.operation.search.RolesCommand;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.identityconnectors.solaris.operation.search.SolarisSearch;

/**
 * Driver for solaris-specific user management commands.
 *
 * Partially copied from the original (hard-coded) Solaris connector code and
 * modified.
 *
 * @see UnixModeDriver
 *
 * @author David Adam
 * @author Radovan Semancik
 *
 */
public class SolarisModeDriver extends UnixModeDriver {

    public static final String MODE_NAME = "solaris";

    private static final String TMPFILE = "/tmp/connloginsError.$$";
    private static final String SHELL_CONT_CHARS = "> ";
    private static final int CHARS_PER_LINE = 160;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/DD/YYYY");

    public SolarisModeDriver(final SolarisConnection conn) {
        super(conn);
    }

    @Override
    public List<SolarisEntry> buildAccountEntries(List<String> blockUserNames, boolean isLast) {
        conn.doSudoStart();
        String out = null;
        try {
            conn.executeCommand(conn.buildCommand(false, "rm -f", TMPFILE));

            String getUsersScript = buildGetUserScript(blockUserNames, isLast);
            out =
                    conn.executeCommand(getUsersScript, conn.getConfiguration()
                            .getBlockFetchTimeout());

            conn.executeCommand(conn.buildCommand(false, "rm -f", TMPFILE));
        } finally {
            conn.doSudoReset();
        }

        List<SolarisEntry> fetchedEntries = processOutput(out, blockUserNames, isLast);
        if (fetchedEntries.size() != blockUserNames.size()) {
            throw new RuntimeException("ERROR: expecting to return " + blockUserNames.size()
                    + " instead of " + fetchedEntries.size());
            // TODO possibly compare by content.
        }

        return fetchedEntries;
    }

    /** retrieve account info from the output. */
    private List<SolarisEntry> processOutput(String out, List<String> blockUserNames, boolean isLast) {
        // SVIDRA# getUsersFromCaptureList(CaptureList captureList, ArrayList
        // users)()

        List<String> lines = Arrays.asList(out.split("\n"));
        Iterator<String> it = lines.iterator();
        int captureIndex = 0;
        List<SolarisEntry> result = new ArrayList<SolarisEntry>(blockUserNames.size());

        while (it.hasNext()) {
            final String currentAccount = blockUserNames.get(captureIndex);
            String line = it.next();
            String lastLoginLine = null;

            // Weed out shell continuation chars
            if (line.startsWith(SHELL_CONT_CHARS)) {
                int index = line.lastIndexOf(SHELL_CONT_CHARS);

                line = line.substring(index + SHELL_CONT_CHARS.length());
            }

            if (isLast) {
                if (!it.hasNext()) {
                    throw new ConnectorException(String.format(
                            "User '%s' is missing last login time.", currentAccount));
                }

                lastLoginLine = "";

                while (lastLoginLine.length() < 3) {
                    lastLoginLine = it.next();
                }
            } // if (isLast)

            SolarisEntry entry = buildUser(currentAccount, line, lastLoginLine);
            if (entry != null) {
                result.add(entry);
            }

            captureIndex++;
        } // while (it.hasNext())

        return result;
    }

    /**
     * build user based on the content given.
     *
     * @param loginsLine
     * @param lastLoginLine
     * @return the build user.
     */
    private SolarisEntry buildUser(String username, String loginsLine, String lastLoginLine) {
        if (lastLoginLine == null) {
            return LoginsCommand.getEntry(loginsLine, username);
        } else {
            SolarisEntry.Builder entryBuilder =
                    new SolarisEntry.Builder(username).addAttr(NativeAttribute.NAME, username);
            // logins
            SolarisEntry entry = LoginsCommand.getEntry(loginsLine, username);
            entryBuilder.addAllAttributesFrom(entry);

            // last
            Attribute attribute = LastCommand.parseOutput(username, lastLoginLine);
            entryBuilder.addAttr(NativeAttribute.LAST_LOGIN, attribute.getValue());

            return entryBuilder.build();
        }
    }

    private String buildGetUserScript(List<String> blockUserNames, boolean isLast) {
        // make a list of users, separated by space.
        StringBuilder connUserList = new StringBuilder();
        int charsThisLine = 0;
        for (String user : blockUserNames) {
            final int length = user.length();
            // take care that line meets the limit on 160 chars per line
            if ((charsThisLine + length + 3) > CHARS_PER_LINE) {
                connUserList.append("\n");
                charsThisLine = 0;
            }

            connUserList.append(user);
            connUserList.append(" ");
            charsThisLine += length + 1;
        }

        StringBuilder getUsersScript = new StringBuilder();
        getUsersScript.append("WSUSERLIST=\"");
        getUsersScript.append(connUserList.toString() + "\n\";");
        getUsersScript.append("for user in $WSUSERLIST; do ");

        String getScript = null;
        if (isLast) {
            // @formatter:off
            getScript =
                conn.buildCommand(true, "logins") + " -oxma -l $user 2>>" + TMPFILE + "; " +
                "LASTLOGIN=`" + conn.buildCommand(true, "last") + " -1 $user`; " +
                "if [ -z \"$LASTLOGIN\" ]; then " +
                     "echo \"wtmp begins\" ; " +
                "else " +
                     "echo $LASTLOGIN; " +
                "fi; ";
            // @formatter:on
        } else {
            getScript = conn.buildCommand(true, "logins") + " -oxma -l $user 2>>" + TMPFILE + "; ";
        }
        getUsersScript.append(getScript);
        getUsersScript.append("done");

        return getUsersScript.toString();
    }

    @Override
    public SolarisEntry buildAccountEntry(String username, Set<NativeAttribute> attrsToGet) {
        /**
         * bunch of boolean flags says if the command is needed to be launched
         * (based on attributes to get)
         */
        boolean isLogins = LoginsCommand.isLoginsRequired(attrsToGet);
        boolean isProfiles = attrsToGet.contains(NativeAttribute.PROFILES);
        boolean isAuths = attrsToGet.contains(NativeAttribute.AUTHS);
        boolean isLast = attrsToGet.contains(NativeAttribute.LAST_LOGIN);
        boolean isRoles = attrsToGet.contains(NativeAttribute.ROLES);

        // if (conn.isNis()) {
        // return buildNISUser(username);
        // }
        SolarisEntry.Builder entryBuilder =
                new SolarisEntry.Builder(username).addAttr(NativeAttribute.NAME, username);

        // we need to execute Logins command always, to figure out if the user
        // exists at all.
        SolarisEntry loginsEntry = LoginsCommand.getAttributesFor(username, conn);

        // Null indicates that the user was not found.
        if (loginsEntry == null) {
            return null;
        }

        if (isLogins) {
            entryBuilder.addAllAttributesFrom(loginsEntry);
        }
        if (isProfiles) {
            final Attribute profiles = ProfilesCommand.getProfilesAttributeFor(username, conn);
            entryBuilder.addAttr(NativeAttribute.PROFILES, profiles.getValue());
        }
        if (isAuths) {
            final Attribute auths = AuthsCommand.getAuthsAttributeFor(username, conn);
            entryBuilder.addAttr(NativeAttribute.AUTHS, auths.getValue());
        }
        if (isLast) {
            final Attribute last = LastCommand.getLastAttributeFor(username, conn);
            entryBuilder.addAttr(NativeAttribute.LAST_LOGIN, last.getValue());
        }
        if (isRoles) {
            final Attribute roles = RolesCommand.getRolesAttributeFor(username, conn);
            entryBuilder.addAttr(NativeAttribute.ROLES, roles.getValue());
        }
        return entryBuilder.build();

    }

    @Override
    public String buildPasswdCommand(String username) {
        return conn.buildCommand(true, "passwd -r files", username);
    }

    @Override
    public void configurePasswordProperties(SolarisEntry entry, SolarisConnection conn) {
        Map<NativeAttribute, String> passwdSwitches = buildPasswdSwitches(entry, conn);
        final String cmdSwitches =
                CommandSwitches.formatCommandSwitches(entry, conn, passwdSwitches);
        if (cmdSwitches.length() == 0) {
            return; // no password related attribute present in the entry.
        }

        try {
            final String command = conn.buildCommand(true, "passwd", cmdSwitches, entry.getName());
            final String out = conn.executeCommand(command);
            final String loweredOut = out.toLowerCase();
            if (loweredOut.contains("usage:") || loweredOut.contains("password aging is disabled")
                    || loweredOut.contains("command not found")) {
                throw new ConnectorException(
                        "Error during configuration of password related attributes. Buffer content: <"
                                + out + ">");
            }
        } catch (Exception ex) {
            throw ConnectorException.wrap(ex);
        }
    }

    private Map<NativeAttribute, String> buildPasswdSwitches(SolarisEntry entry,
            SolarisConnection conn) {
        Map<NativeAttribute, String> passwdSwitches =
                new EnumMap<NativeAttribute, String>(NativeAttribute.class);
        passwdSwitches.put(NativeAttribute.PWSTAT, "-f");
        // passwdSwitches.put(NativeAttribute.PW_LAST_CHANGE, null); // this is
        // not used attribute (see LoginsCommand and its SVIDRA counterpart).
        // TODO erase this comment.
        passwdSwitches.put(NativeAttribute.MIN_DAYS_BETWEEN_CHNG, "-n");
        passwdSwitches.put(NativeAttribute.MAX_DAYS_BETWEEN_CHNG, "-x");
        passwdSwitches.put(NativeAttribute.DAYS_BEFORE_TO_WARN, "-w");

        String lockFlag = null;
        Attribute lock = entry.searchForAttribute(NativeAttribute.LOCK);
        if (lock != null) {
            Object lockValue = AttributeUtil.getSingleValue(lock);
            if (lockValue == null) {
                throw new IllegalArgumentException("missing value for attribute LOCK");
            }
            boolean isLock = (Boolean) lockValue;
            if (isLock) {
                lockFlag = "-l";
            } else {
                // *unlocking* differs in Solaris 8,9 and in Solaris 10+:
                lockFlag = (conn.isVersionLT10()) ? "-df" : "-u";
                passwdSwitches.put(NativeAttribute.LOCK, lockFlag);
            }
        }
        if (lockFlag != null) {
            passwdSwitches.put(NativeAttribute.LOCK, lockFlag);
        }
        return passwdSwitches;
    }

    @Override
    public Schema buildSchema() {
        final SchemaBuilder schemaBuilder = new SchemaBuilder(SolarisConnector.class);

        /*
         * GROUP
         */
        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
        // attributes.add(Name.INFO);
        for (GroupAttribute attr : GroupAttribute.values()) {
            switch (attr) {
            case USERS:
                attributes.add(AttributeInfoBuilder.build(attr.getName(), String.class, EnumSet
                        .of(Flags.MULTIVALUED)));
                break;
            case GROUPNAME:
                attributes.add(AttributeInfoBuilder.build(attr.getName(), String.class, EnumSet
                        .of(Flags.REQUIRED)));
                break;
            case GID:
                attributes.add(AttributeInfoBuilder.build(attr.getName(), int.class, EnumSet
                        .of(Flags.NOT_RETURNED_BY_DEFAULT)));
                break;

            default:
                attributes.add(AttributeInfoBuilder.build(attr.getName()));
                break;
            }
        }

        // GROUP supports no authentication:
        final ObjectClassInfo ociInfoGroup =
                new ObjectClassInfoBuilder().setType(ObjectClass.GROUP_NAME).addAllAttributeInfo(
                        attributes).build();
        schemaBuilder.defineObjectClass(ociInfoGroup);
        schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, ociInfoGroup);
        schemaBuilder.removeSupportedObjectClass(ResolveUsernameOp.class, ociInfoGroup);

        /*
         * ACCOUNT
         */
        attributes = new HashSet<AttributeInfo>();
        attributes.add(OperationalAttributeInfos.PASSWORD);
        for (AccountAttribute attr : AccountAttribute.values()) {
            String attrName = attr.getName();
            AttributeInfo newAttr = null;
            switch (attr) {
                case NAME:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, String.class, EnumSet
                                    .of(Flags.REQUIRED));
                    break;
                case MIN:
                case MAX:
                case WARN:
                case INACTIVE:
                case UID:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, int.class, EnumSet
                                    .of(Flags.NOT_RETURNED_BY_DEFAULT));
                    break;
                case PASSWD_FORCE_CHANGE:
                case LOCK:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, boolean.class, EnumSet
                                    .of(Flags.NOT_RETURNED_BY_DEFAULT));
                    break;
                case SECONDARY_GROUP:
                case ROLES:
                case AUTHORIZATION:
                case PROFILE:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, String.class, EnumSet.of(
                                    Flags.MULTIVALUED, Flags.NOT_RETURNED_BY_DEFAULT));
                    break;
                case TIME_LAST_LOGIN:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, String.class, EnumSet.of(
                                    Flags.NOT_UPDATEABLE, Flags.NOT_CREATABLE,
                                    Flags.NOT_RETURNED_BY_DEFAULT));
                    break;
                default:
                    newAttr = AttributeInfoBuilder.build(attrName);
                    break;
            }

            if (newAttr != null) {
                attributes.add(newAttr);
            }
        }
        
        tweakAccountActivationSchema(attributes);
        
        final ObjectClassInfo ociInfoAccount =
                new ObjectClassInfoBuilder().setType(ObjectClass.ACCOUNT_NAME).addAllAttributeInfo(
                        attributes).build();
        schemaBuilder.defineObjectClass(ociInfoAccount);

        /*
         * SHELL
         */
        attributes = new HashSet<AttributeInfo>();
        attributes.add(AttributeInfoBuilder.build(SolarisSearch.SHELL.getObjectClassValue(),
                String.class, EnumSet.of(Flags.MULTIVALUED, Flags.NOT_RETURNED_BY_DEFAULT,
                        Flags.NOT_UPDATEABLE)));
        final ObjectClassInfo ociInfoShell =
                new ObjectClassInfoBuilder().addAllAttributeInfo(attributes).setType(
                        SolarisSearch.SHELL.getObjectClassValue()).build();
        schemaBuilder.defineObjectClass(ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(CreateOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(UpdateOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(DeleteOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(SchemaOp.class, ociInfoShell);
        schemaBuilder.removeSupportedObjectClass(ResolveUsernameOp.class, ociInfoShell);

        return schemaBuilder.build();
    }

	@Override
	public String getSudoPasswordRegexp() {
		return "^[pP]assword[^:]*:";
	}

	@Override
	public String getRenameDirScript(SolarisEntry entry, String newName) {
		// @formatter:off
        String renameDir =
            "NEWNAME=" + newName + "; " +
            "OLDNAME=" + entry.getName() + "; " +
            "OLDDIR=`" + conn.buildCommand(true, "logins") + " -ox -l $NEWNAME | cut -d: -f6`; " +
            "OLDBASE=`basename $OLDDIR`; " +
            "if [ \"$OLDNAME\" = \"$OLDBASE\" ]; then\n" +
              "PARENTDIR=`dirname $OLDDIR`; " +
              "NEWDIR=`echo $PARENTDIR/$NEWNAME`; " +
              "if [ ! -s $NEWDIR ]; then " +
                conn.buildCommand(true, "chown") + " $NEWNAME $OLDDIR; " +
                conn.buildCommand(true, "mv") + " -f $OLDDIR $NEWDIR; " +
                "if [ $? -eq 0 ]; then\n" +
                  conn.buildCommand(true, "usermod") + " -d $NEWDIR $NEWNAME; " +
                "fi; " +
              "fi; " +
            "fi";
        // @formatter:off
        return renameDir;
	}
	
	@Override
	public String formatDate(long daysSinceEpoch) {
		return DATE_FORMAT.format(daysSinceEpoch*(24*60*60*1000));
	}

}
