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

import org.identityconnectors.common.CollectionUtil;
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
import org.identityconnectors.solaris.attr.ConnectorAttribute;
import org.identityconnectors.solaris.attr.GroupAttribute;
import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.CommandSwitches;
import org.identityconnectors.solaris.operation.search.SolarisEntry;
import org.identityconnectors.solaris.operation.search.SolarisSearch;

public class AixModeDriver extends UnixModeDriver{

	public static final String MODE_NAME = "aix";

    private static final String TMPFILE = "/tmp/connloginsError.$$";
    private static final String SHELL_CONT_CHARS = "> ";
    private static final int CHARS_PER_LINE = 160;

    private static final Log logger = Log.getLog(AixModeDriver.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-DD");

	
	public AixModeDriver(SolarisConnection conn) {
		super(conn);
	}

	@Override
	public SolarisEntry buildAccountEntry(String username,
			Set<NativeAttribute> attrsToGet) {
		 boolean isLast = attrsToGet.contains(NativeAttribute.LAST_LOGIN);

	        conn.doSudoStart();

	        String out = null;
	        try {
	            conn.executeCommand(conn.buildCommand(false, "rm -f", TMPFILE));

	            StringBuilder scriptBuilder = new StringBuilder();
	            scriptBuilder.append(conn.buildCommand(true, "lsuser ", username));
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

	        SolarisEntry entry = buildUser(username, out);
	        return entry;
	}

	@Override
	public List<SolarisEntry> buildAccountEntries(List<String> blockUserNames,
			boolean isLast) {
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
//            String lastLoginLine = null;

            // Weed out shell continuation chars
            if (line.startsWith(SHELL_CONT_CHARS)) {
                int index = line.lastIndexOf(SHELL_CONT_CHARS);

                line = line.substring(index + SHELL_CONT_CHARS.length());
            }

            SolarisEntry entry = buildUser(currentAccount, line);
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
    private SolarisEntry buildUser(String username, String loginsLine) {
    	List<String> attributesValues = Arrays.asList(loginsLine.split(" "));
    	SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(username).addAttr(NativeAttribute.NAME, username);
    	for (String aV : attributesValues){
    		String[] vals = aV.split("=");
    		if (vals.length != 2){
    			continue;
    		}
    		
    		String attrName = vals[0];
    		String attrValue = vals[1];
    		
    		if (attrName.equals("groups")){
    			List<String> values = Arrays.asList(attrValue.split(","));
    			entryBuilder.addAttr(NativeAttribute.GROUP_PRIM, values);
    			continue;
    		}
    		
    		if (attrName.equals("id")){
    			entryBuilder.addAttr(NativeAttribute.ID, attrValue);
    			continue;
    		}
    		
    		if (attrName.equals("home")){
    			entryBuilder.addAttr(NativeAttribute.DIR, attrValue);
    			continue;
    		}
    		
    		
    		
    		ConnectorAttribute name = AccountAttribute.forAttributeName(attrName);
    		
    		if (name == null){
    			continue;
    		}
    		
    		
    		List<String> values = Arrays.asList(attrValue.split(","));
    		entryBuilder.addAttr(name.getNative(), values);
    		
    	}
    	
    	return entryBuilder.build();
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

     String getScript = conn.buildCommand(true, "lsuser") + "$user; ";
     scriptBuilder.append(getScript);
     
     if (isLast) {
         // TODO
     }
     scriptBuilder.append("done");

     return scriptBuilder.toString();
 }

	@Override
	public String buildPasswdCommand(String username) {
		return conn.buildCommand(true, "pwdadm", username);
	}

	@Override
	public void configurePasswordProperties(SolarisEntry entry,
			SolarisConnection conn) {
		 Map<NativeAttribute, String> passwdSwitches = buildPasswdSwitches(entry, conn);
	        final String cmdSwitches =
	                CommandSwitches.formatCommandSwitches(entry, conn, passwdSwitches);
	        if (cmdSwitches.length() == 0) {
	            return; // no password related attribute present in the entry.
	        }

	        try {
	            final String command = conn.buildCommand(true, "pwdadm", cmdSwitches, entry.getName());
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
	public Set<String> getRequiredCommands() {
		return CollectionUtil.newSet(
	            // user
                "last", "mkuser"); /*",usermod", "userdel", "passwd",*/
                // group
//                "groupadd", "groupmod", "groupdel");
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
	public String buildCreateUserCommand(SolarisEntry entry,
			String commandSwitches) {
		
				
		return conn.buildCommand(true, "mkuser", getAttributes(entry), entry.getName());
	}
	
	private String getAttributes(SolarisEntry entry){
		Set<Attribute> attributes = entry.getAttributeSet();
		StringBuilder buildedAttributes = new StringBuilder();
		for (Attribute attr : attributes){
			if (attr.getValue().isEmpty()){
				continue;
			}
			
			if (NativeAttribute.NAME.getName().equals(attr.getName())){
				continue;
			}
			
			Iterator<Object> values = attr.getValue().iterator();
			StringBuilder valuesBuilder = new StringBuilder(String.valueOf(values.next()));
			while (values.hasNext()){
				valuesBuilder.append(",");
				valuesBuilder.append(values.next());
			}
			
			String name = attr.getName();
			for (AccountAttribute accountAttr : AccountAttribute.values()){
				if (accountAttr.getNative().getName().equals(attr.getName())){
					name = accountAttr.getName();
					break;
				}
			}
			buildedAttributes.append(name);
			buildedAttributes.append("=");
			buildedAttributes.append(valuesBuilder);
			buildedAttributes.append(" ");
		}
		
		return buildedAttributes.toString();
	}
	
	@Override
	public String buildUpdateUserCommand(SolarisEntry entry,
			String commandSwitches) {
		return conn.buildCommand(true, "chuser", getAttributes(entry), entry.getName());
	}
	
	@Override
	public String getRenameDirScript(SolarisEntry entry, String newName) {
		 String renameDir =
		            "NEWNAME=" + newName + "; " +
		            "OLDNAME=" + entry.getName() + "; " +
		            "OLDDIR=`" + conn.buildCommand(true, "lsuser") + " $NEWNAME | cut -d: -f6`; " +
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
