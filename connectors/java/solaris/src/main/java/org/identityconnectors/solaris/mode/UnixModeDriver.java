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

import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.AccountAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;

/**
 * Generic abstraction that takes care of various UNIX flavours.
 *
 * Subclasses of this class contains implementation of methods that differ
 * between UNIX-like systems. The specific implementation used for executing the
 * commands is determined by the connector configuration.
 *
 * Implementation note: The goal of this interface was to add support for
 * several UNIX flavors without changing the original connector code too much
 * (although it really needs refactoring).
 *
 * @author Radovan Semancik
 * @author David Adam
 *
 */
public abstract class UnixModeDriver {

    final protected SolarisConnection conn;

    public UnixModeDriver(final SolarisConnection conn) {
        super();
        this.conn = conn;
    }

    /**
     * get the user entry for given username.
     *
     * @param username
     * @return the initialized entry, or Null in case the user was not found on
     *         the resource.
     */
    public abstract SolarisEntry buildAccountEntry(String username, Set<NativeAttribute> attrsToGet);

    /**
     * get the attributes for given block of users.
     *
     * @param blockUserNames
     * @return the SolarisEntry list initialized with the required attributes.
     */
    public abstract List<SolarisEntry> buildAccountEntries(List<String> blockUserNames,
            boolean isLast);

    public abstract String buildPasswdCommand(String username);

    public abstract void configurePasswordProperties(SolarisEntry entry, SolarisConnection conn);

    public abstract Schema buildSchema();
    
    public Set<String> getRequiredCommands(){
    	return CollectionUtil.newSet(
                // user
                "last", "useradd", "usermod", "userdel", "passwd",
                // group
                "groupadd", "groupmod", "groupdel");
    }
    
    protected void tweakAccountActivationSchema(Set<AttributeInfo> attributes) {
		String activationMode = conn.getConfiguration().getActivationMode();
		if (ActivationMode.EXPIRATION.getConfigString().equals(activationMode) 
				|| ActivationMode.LOCKING.getConfigString().equals(activationMode)) {
			attributes.add(OperationalAttributeInfos.ENABLE);
    		
    	} else if (ActivationMode.NONE.getConfigString().equals(activationMode)) {
    		// nothing to do
    		
    	} else {
    		throw new IllegalArgumentException("Unknown activation mode "+activationMode);
    	}
		
	}

	abstract public String getSudoPasswordRegexp();

	abstract public String getRenameDirScript(SolarisEntry entry, String newName);
	
	abstract public String formatDate(long daysSinceEpoch);
	
	public String buildCreateUserCommand(SolarisEntry entry, String commandSwitches){
		
        // useradd command execution
        String command = conn.buildCommand(true, "useradd", commandSwitches, entry.getName());
        return command;
	}
	
	public String buildUpdateUserCommand(SolarisEntry entry, String commandSwitches){
		return conn.buildCommand(true, "usermod", commandSwitches, entry.getName());
	}

}
