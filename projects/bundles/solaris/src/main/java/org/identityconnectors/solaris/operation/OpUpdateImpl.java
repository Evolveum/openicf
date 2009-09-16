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
package org.identityconnectors.solaris.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.solaris.SolarisConfiguration;
import org.identityconnectors.solaris.SolarisConnector;
import org.identityconnectors.solaris.SolarisUtil;
import org.identityconnectors.solaris.command.CommandUtil;
import org.identityconnectors.solaris.command.MatchBuilder;
import org.identityconnectors.solaris.command.closure.ClosureFactory;

/**
 * Implementation of update SPI operation
 * 
 * @author David Adam
 */
public class OpUpdateImpl extends AbstractOp {

    /** mutex acquire constants */
    private static final String tmpPidMutexFile = "/tmp/WSlockuid.$$";
    private static final String pidMutexFile = "/tmp/WSlockuid";
    private static final String pidFoundFile = "/tmp/WSpidfound.$$";

    /** These objectclasses are valid for update operation. */
    final ObjectClass[] acceptOC = { ObjectClass.ACCOUNT, ObjectClass.GROUP };

    public OpUpdateImpl(Log log, SolarisConnector conn) {
        super(log, conn, OpUpdateImpl.class);
    }

    /** main update method */
    public Uid update(ObjectClass objclass, Uid uid,
            Set<Attribute> replaceAttributes, OperationOptions options) {

        SolarisUtil.controlObjectClassValidity(objclass, acceptOC, getClass());

        // Read only list of attributes
        final Map<String, Attribute> attrMap = new HashMap<String, Attribute>(
                AttributeUtil.toMap(replaceAttributes));

        Uid name = (Uid) attrMap.get(Uid.NAME);
        final String accountId = name.getUidValue();

        doSudoStart();

        /*
         * ACCOUNT
         */
        if (objclass.is(ObjectClass.ACCOUNT_NAME)) {
            final String commandSwitches = CommandUtil.prepareCommand(replaceAttributes, ObjectClass.ACCOUNT);

            if (commandSwitches.length() > 0) {
                try {
                    // First acquire the "mutex" for uid creation
                    getConnection().send(getAcquireMutexScript());
                    MatchBuilder builder = new MatchBuilder();
                    builder.addRegExpMatch(getConfiguration()
                            .getRootShellPrompt(), ClosureFactory.newNullClosure());
                    builder.addRegExpMatch("ERROR", ClosureFactory.newConnectorException("acquiring mutex failed"));

                    getConnection().expect(builder.build());

                    // perform the UPDATE
                    builder = new MatchBuilder();
                    builder.addRegExpMatch(getConfiguration().getRootShellPrompt(), ClosureFactory.newNullClosure());
                    builder.addRegExpMatch("ERROR", ClosureFactory.newConnectorException("ERROR occured during update [usermod]"));
                    builder.addRegExpMatch("command not found", ClosureFactory.newConnectorException("usermod command is not found"));
                    builder.addRegExpMatch("not allowed to execute", ClosureFactory.newConnectorException("not allowed to execute usermod"));

                    getConnection().send(getConnection().buildCommand("usermod", commandSwitches, accountId));
                    getConnection().expect(builder.build());

                    // Release the uid "mutex"
                    getConnection().send(getMutexReleaseScript());
                    getConnection().waitFor(getConfiguration().getRootShellPrompt());
                } catch (Exception ex) {
                    throw ConnectorException.wrap(ex);
                }
            }

            // PASSWORD UPDATE
            GuardedString passwd = SolarisUtil.getPasswordFromMap(attrMap);
            if (passwd != null) {
                handlePasswdUpdate(accountId, passwd);
            }

        } else if (objclass.is(ObjectClass.GROUP_NAME)) {
            /*
             * GROUP
             */
            throw new UnsupportedOperationException("Update on Group is not yet implemented");
        }

        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        Uid newUid = null; // TODO

        doSudoReset();
        return newUid;
    }

    private void handlePasswdUpdate(String accountId, GuardedString passwd) {
        try {
            MatchBuilder builder = new MatchBuilder();
            builder.addCaseInsensitiveRegExpMatch("ew password:", ClosureFactory.newNullClosure());
            builder.addRegExpMatch("Permission denied", ClosureFactory.newConnectorException("permission denied when executing passwd"));
            builder.addRegExpMatch("command not found", ClosureFactory.newConnectorException("passwd command not found"));
            builder.addRegExpMatch("not allowed to execute", ClosureFactory.newConnectorException("user is not allowed to execute passwd"));
            //for nonexisting UID (note: match not present in the adapter)
            builder.addCaseInsensitiveRegExpMatch("User unknown", ClosureFactory.newUnknownUidException(String.format("Unknown Uid: '%s'", accountId)));

            getConnection().send(getConnection().buildCommand("passwd -r files", accountId));
            getConnection().expect(builder.build());

            SolarisUtil.sendPassword(passwd, getConnection());

            getConnection().waitForCaseInsensitive("ew password:");
            SolarisUtil.sendPassword(passwd, getConnection());

            getConnection().waitFor(getConfiguration().getRootShellPrompt());
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    private String getAcquireMutexScript() {
        Long timeout = getConfiguration().getMutexAcquireTimeout();
        String rmCmd = getConnection().buildCommand("rm");
        String catCmd = getConnection().buildCommand("cat");

        if (timeout < 1) {
            timeout = SolarisConfiguration.DEFAULT_MUTEX_ACQUIRE_TIMEOUT;
        }

        String pidMutexAcquireScript =
            "TIMEOUT=" + timeout + "; " +
            "echo $$ > " + tmpPidMutexFile + "; " +
            "while test 1; " +
            "do " +
              "ln -n " + tmpPidMutexFile + " " + pidMutexFile + " 2>/dev/null; " +
              "rc=$?; " +
              "if [ $rc -eq 0 ]; then\n" +
                "LOCKPID=`" + catCmd + " " +  pidMutexFile + "`; " +
                "if [ \"$LOCKPID\" = \"$$\" ]; then " +
                  rmCmd + " -f " + tmpPidMutexFile + "; " +
                  "break; " +
                "fi; " +
              "fi\n" +
              "if [ -f " + pidMutexFile + " ]; then " +
                "LOCKPID=`" + catCmd + " " + pidMutexFile + "`; " +
                "if [ \"$LOCKPID\" = \"$$\" ]; then " +
                  rmCmd + " -f " + pidMutexFile + "\n" +
                "else " +
                  "ps -ef | while read REPLY\n" +
                  "do " +
                    "TESTPID=`echo $REPLY | awk '{ print $2 }'`; " +
                    "if [ \"$LOCKPID\" = \"$TESTPID\" ]; then " +
                      "touch " + pidFoundFile + "; " +
                      "break; " +
                    "fi\n" +
                  "done\n" +
                  "if [ ! -f " + pidFoundFile + " ]; then " +
                    rmCmd + " -f " + pidMutexFile + "; " +
                  "else " +
                    rmCmd + " -f " + pidFoundFile + "; " +
                  "fi\n" +
                "fi\n" +
              "fi\n" +
              "TIMEOUT=`echo | awk 'BEGIN { n = '$TIMEOUT' } { n -= 1 } END { print n }'`\n" +
              "if [ $TIMEOUT = 0 ]; then " +
                "echo \"ERROR: failed to obtain uid mutex\"; " +
                rmCmd + " -f " + tmpPidMutexFile + "; " +
                "break; " +
              "fi\n" +
              "sleep 1; " +
            "done";

        return pidMutexAcquireScript;
    }

    private String getMutexReleaseScript() {
        String rmCmd = getConnection().buildCommand("rm");
        String pidMutexReleaseScript =
            "if [ -f " + pidMutexFile + " ]; then " +
              "LOCKPID=`cat " + pidMutexFile + "`; " +
              "if [ \"$LOCKPID\" = \"$$\" ]; then " +
                rmCmd + " -f " + pidMutexFile + "; " +
              "fi; " +
            "fi";
        return pidMutexReleaseScript;
    }
}