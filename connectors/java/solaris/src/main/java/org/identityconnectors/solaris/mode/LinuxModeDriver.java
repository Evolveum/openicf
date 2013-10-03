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
 * Portions Copyrighted 2008-2009 Sun Microsystems
 */
package org.identityconnectors.solaris.mode;

import java.text.DateFormat;
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

import org.identityconnectors.common.logging.Log;
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
import org.identityconnectors.solaris.operation.search.GetentPasswordCommand;
import org.identityconnectors.solaris.operation.search.GetentShadowCommand;
import org.identityconnectors.solaris.operation.search.IdCommand;
import org.identityconnectors.solaris.operation.search.PasswdCommand;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.identityconnectors.solaris.operation.search.SolarisSearch;

/**
 * Driver for linux-specific user management commands.
 *
 * Partially copied from the original (hard-coded) Solaris connector code and
 * modified for the linux-specific commands. It is using combination of
 * 'getent', 'id' and 'passwd' commands instead the solaris-specific 'logins'
 * command.
 *
 * The code is not ideal. But the goal was to do minimal modifications to
 * original Solaris connector.
 *
 * @see UnixModeDriver
 *
 * @author Radovan Semancik
 *
 */
public class LinuxModeDriver extends UnixModeDriver {

    public static final String MODE_NAME = "linux";

    private static final String TMPFILE = "/tmp/connloginsError.$$";
    private static final String SHELL_CONT_CHARS = "> ";
    private static final int CHARS_PER_LINE = 160;

    private static final Log logger = Log.getLog(LinuxModeDriver.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-DD");

    public LinuxModeDriver(final SolarisConnection conn) {
        super(conn);
    }

    @Override
    public List<SolarisEntry> buildAccountEntries(List<String> blockUserNames, boolean isLast) {
        conn.doSudoStart();

        String out = null;
        try {
            conn.executeCommand(conn.buildCommand(false, "rm -f", TMPFILE));

            String getUsersScript = buildGetUserScript(blockUserNames, isLast);
            logger.info("Script: {0}", getUsersScript);
            out =
                    conn.executeCommand(getUsersScript, conn.getConfiguration()
                            .getBlockFetchTimeout());
            logger.info("OUT: {0}", out);

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

        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("WSUSERLIST=\"");
        scriptBuilder.append(connUserList.toString() + "\n\";");
        scriptBuilder.append("for user in $WSUSERLIST; do ");

        scriptBuilder.append(buildGetUserScriptLine(true, "getent", "passwd $user"));
        scriptBuilder.append(buildGetUserScriptLine(true, "id", "-Grn $user"));
        scriptBuilder.append(buildGetUserScriptLine(true, "passwd", "-S $user"));
        if (isLast) {
            // TODO
        }
        scriptBuilder.append("done");

        return scriptBuilder.toString();
    }

    private String buildGetUserScriptLine(boolean needSudo, String command, String args) {
        return conn.buildCommand(needSudo, command) + (args == null ? "" : " " + args) + " 2>>"
                + TMPFILE + "; ";
    }

    /** Retrieve account info from the output. */
    private List<SolarisEntry> processOutput(String out, List<String> blockUserNames, boolean isLast) {

        List<String> lines = Arrays.asList(out.split("\n"));
        Iterator<String> it = lines.iterator();
        int captureIndex = 0;
        List<SolarisEntry> result = new ArrayList<SolarisEntry>(blockUserNames.size());

        while (it.hasNext()) {
            final String accountUsername = blockUserNames.get(captureIndex);
            String linePwent = readLine(it, accountUsername, "pwent");
            String lineId = readLine(it, accountUsername, "id");
            String linePasswd = readLine(it, accountUsername, "passwd");

            String lineLastLogin = null;
            if (isLast) {
                lineLastLogin = readLine(it, accountUsername, "last login");
            } // if (isLast)

            SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(accountUsername);

            SolarisEntry getentEntry = GetentPasswordCommand.getEntry(linePwent, accountUsername);
            entryBuilder.addAllAttributesFrom(getentEntry);

            SolarisEntry idEntry = IdCommand.getEntry(lineId, accountUsername);
            entryBuilder.addAllAttributesFrom(idEntry);

            SolarisEntry passwdEntry = PasswdCommand.getEntry(linePasswd, accountUsername);
            entryBuilder.addAllAttributesFrom(passwdEntry);

            SolarisEntry entry = entryBuilder.build();
            if (entry != null) {
                result.add(entry);
            }

            captureIndex++;
        } // while (it.hasNext())

        return result;
    }

    private String readLine(Iterator<String> lineIterator, String username, String lineType) {
        if (!lineIterator.hasNext()) {
            throw new ConnectorException(String.format("User '%s' is missing %s time.", username,
                    lineType));
        }
        String line = lineIterator.next();
        String noContLine = weedOutShellContChars(line);
        return weedOutControlChars(noContLine);
    }

    private String weedOutShellContChars(String line) {
        // Weed out shell continuation chars
        if (line.startsWith(SHELL_CONT_CHARS)) {
            int index = line.lastIndexOf(SHELL_CONT_CHARS);
            line = line.substring(index + SHELL_CONT_CHARS.length());
        }
        return line;
    }

    private String weedOutControlChars(String line) {
        if (line.endsWith("\r")) {
            return line.substring(0, line.length() - 1);
        } else {
            return line;
        }
    }

    @Override
    public SolarisEntry buildAccountEntry(String username, Set<NativeAttribute> attrsToGet) {
        // TODO: this has a lot of common code with the buildAccountEntries.
        // Refactor.

        boolean isLast = attrsToGet.contains(NativeAttribute.LAST_LOGIN);

        conn.doSudoStart();

        String out = null;
        try {
            conn.executeCommand(conn.buildCommand(false, "rm -f", TMPFILE));

            StringBuilder scriptBuilder = new StringBuilder();
            scriptBuilder.append(buildGetUserScriptLine(true, "getent", "passwd " + username));
            scriptBuilder.append(buildGetUserScriptLine(true, "id", "-Grn " + username));
            scriptBuilder.append(buildGetUserScriptLine(true, "passwd", "-S " + username));
            scriptBuilder.append(buildGetUserScriptLine(true, "getent", "shadow " + username));
            if (isLast) {
                // TODO
            }
            String script = scriptBuilder.toString();
            logger.info("Script: {0}", script);

            out = conn.executeCommand(script, conn.getConfiguration().getBlockFetchTimeout());
            logger.info("OUT: {0}", out);

            conn.executeCommand(conn.buildCommand(false, "rm -f", TMPFILE));
        } finally {
            conn.doSudoReset();
        }

        List<String> lines = Arrays.asList(out.split("\n"));
        Iterator<String> it = lines.iterator();
        String linePwent = readLine(it, username, "pwent");
        String lineId = readLine(it, username, "id");
        String linePasswd = readLine(it, username, "passwd");
        String lineShadow = readLine(it, username, "shadow");

        SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(username);

        SolarisEntry getentEntry = GetentPasswordCommand.getEntry(linePwent, username);
        entryBuilder.addAllAttributesFrom(getentEntry);

        SolarisEntry idEntry = IdCommand.getEntry(lineId, username);
        entryBuilder.addAllAttributesFrom(idEntry);

        SolarisEntry passwdEntry = PasswdCommand.getEntry(linePasswd, username);
        entryBuilder.addAllAttributesFrom(passwdEntry);
        
        SolarisEntry shadowEntry = GetentShadowCommand.getEntry(lineShadow, username);
        entryBuilder.addAllAttributesFrom(shadowEntry);
        
        SolarisEntry entry = entryBuilder.build();

        return entry;
    }

    @Override
    public String buildPasswdCommand(String username) {
        return conn.buildCommand(true, "passwd", username);
    }

    @Override
    public void configurePasswordProperties(SolarisEntry entry, SolarisConnection conn) {
        // Linux has to handle lock/unlock in a separate command
        Map<NativeAttribute, String> passwdSwitches = buildPasswdSwitchesLock(entry, conn);
        String cmdSwitches = CommandSwitches.formatCommandSwitches(entry, conn, passwdSwitches);
        if (!cmdSwitches.isEmpty()) {
            try {
                final String command =
                        conn.buildCommand(true, "passwd", cmdSwitches, entry.getName());
                final String out = conn.executeCommand(command);
                final String loweredOut = out.toLowerCase();
                if (loweredOut.contains("usage:")
                        || loweredOut.contains("password aging is disabled")
                        || loweredOut.contains("command not found")) {
                    throw new ConnectorException(
                            "Error during configuration of password related attributes. Buffer content: <"
                                    + out + ">");
                }
            } catch (Exception ex) {
                throw ConnectorException.wrap(ex);
            }
        }

        passwdSwitches = buildPasswdSwitches(entry, conn);
        cmdSwitches = CommandSwitches.formatCommandSwitches(entry, conn, passwdSwitches);
        if (!cmdSwitches.isEmpty()) {
            try {
                final String command =
                        conn.buildCommand(true, "passwd", cmdSwitches, entry.getName());
                final String out = conn.executeCommand(command);
                final String loweredOut = out.toLowerCase();
                if (loweredOut.contains("usage:")
                        || loweredOut.contains("password aging is disabled")
                        || loweredOut.contains("command not found")) {
                    throw new ConnectorException(
                            "Error during configuration of password related attributes. Buffer content: <"
                                    + out + ">");
                }
            } catch (Exception ex) {
                throw ConnectorException.wrap(ex);
            }
        }

    }

    private Map<NativeAttribute, String> buildPasswdSwitches(SolarisEntry entry,
            SolarisConnection conn) {
        Map<NativeAttribute, String> passwdSwitches =
                new EnumMap<NativeAttribute, String>(NativeAttribute.class);
        // Immediately expire password. This forces password change.
        passwdSwitches.put(NativeAttribute.PWSTAT, "-e");

        passwdSwitches.put(NativeAttribute.MIN_DAYS_BETWEEN_CHNG, "-n");
        passwdSwitches.put(NativeAttribute.MAX_DAYS_BETWEEN_CHNG, "-x");
        passwdSwitches.put(NativeAttribute.DAYS_BEFORE_TO_WARN, "-w");
        return passwdSwitches;
    }

    private Map<NativeAttribute, String> buildPasswdSwitchesLock(SolarisEntry entry,
            SolarisConnection conn) {
        Map<NativeAttribute, String> passwdSwitches =
                new EnumMap<NativeAttribute, String>(NativeAttribute.class);
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
                lockFlag = "-u";
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

            default:
                attributes.add(AttributeInfoBuilder.build(attr.getName()));
                break;
            } // switch
        } // for

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
                case EXPIRE:
                case UID:
                    newAttr = AttributeInfoBuilder.build(attrName, long.class);
                    break;
                case PASSWD_FORCE_CHANGE:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, boolean.class, EnumSet
                                    .of(Flags.NOT_RETURNED_BY_DEFAULT));
                    break;
                case SECONDARY_GROUP:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, String.class, EnumSet
                                    .of(Flags.MULTIVALUED));
                    break;
                case ROLES:
                case AUTHORIZATION:
                case PROFILE:
                    newAttr = null;
                    break;
                case TIME_LAST_LOGIN:
                    newAttr =
                            AttributeInfoBuilder.build(attrName, String.class, EnumSet.of(
                                    Flags.NOT_UPDATEABLE, Flags.NOT_RETURNED_BY_DEFAULT));
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
		return "\\[sudo\\].*[pP]assword[^:]*:";
	}
	
	@Override
	public String getRenameDirScript(SolarisEntry entry, String newName) {
		// @formatter:off
        String renameDir =
            "NEWNAME=" + newName + "; " +
            "OLDNAME=" + entry.getName() + "; " +
            "OLDDIR=`" + conn.buildCommand(true, "getent") + " passwd $NEWNAME | cut -d: -f6`; " +
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
