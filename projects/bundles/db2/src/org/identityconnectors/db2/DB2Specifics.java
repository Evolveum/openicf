/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
package org.identityconnectors.db2;

import java.sql.*;
import java.util.*;

import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;

/**
 * Here we hide DB2 specifics constants,mappings,restrictions ...
 * @author kitko
 *
 */
class DB2Specifics {
	
	/** Classname of DB2 jcc driver , type 4 driver*/
	final static String JCC_DRIVER = "com.ibm.db2.jcc.DB2Driver";
	/** Old driver that uses local db2 client with stored aliases , type 2 driver */
	final static String APP_DRIVER = "COM.ibm.db2.jdbc.app.DB2Driver";
	final static String DEFAULT_TYPE4_PORT = "50000";
	
	
    // These names come from the DB2 SQL Reference manual.
    // None of these names are legal for starting account id
    // or password.  The prohibition is case insensitive.
    private static final Collection<String> excludedNamePrefixes = Arrays.asList("SQL", "SYS", "IBM");
    
    static final String AUTH_TYPE_DATABASE = "database";
    static final String AUTH_TYPE_INDEX = "index";
    static final String AUTH_TYPE_PACKAGE = "package";
    static final String AUTH_TYPE_SCHEMA = "schema";
    static final String AUTH_TYPE_SERVER = "server";
    static final String AUTH_TYPE_TABLE = "table";
    static final String AUTH_TYPE_TABLESPACE = "tablespace";

    // A map from authority table type to authority table.
    private static final Map<String,DB2AuthorityTable> databaseAuthTableMap = new HashMap<String,DB2AuthorityTable>();
    static {
        databaseAuthTableMap.put(AUTH_TYPE_DATABASE,
            new DB2AuthorityTable("ON DATABASE"));
        databaseAuthTableMap.put(AUTH_TYPE_INDEX,
            new DB2AuthorityTable("ON INDEX"));
        databaseAuthTableMap.put(AUTH_TYPE_PACKAGE,
            new DB2AuthorityTable("ON PACKAGE"));
        databaseAuthTableMap.put(AUTH_TYPE_SCHEMA,
            new DB2AuthorityTable("ON SCHEMA"));
        databaseAuthTableMap.put(AUTH_TYPE_SERVER,
            new DB2AuthorityTable("ON SERVER"));
        databaseAuthTableMap.put(AUTH_TYPE_TABLESPACE,
            new DB2AuthorityTable("OF TABLESPACE"));
        databaseAuthTableMap.put(AUTH_TYPE_TABLE,
            new DB2AuthorityTable("ON"));
    }
    
    static DB2AuthorityTable authType2DB2AuthorityTable(String authType){
    	return databaseAuthTableMap.get(authType);
    }
    

	/** List of db2 keywords */
	private static volatile Collection<String> excludeNames;
	
	private static Collection<String> readExcludeNames() {
		if(excludeNames == null){
			synchronized (DB2Configuration.class) {
				if(excludeNames == null){
					//We will read exclude names from resource named "exclude.names"
					String names = IOUtil.getResourceAsString(DB2Configuration.class, "exclude.names");
					if(names == null){
						throw new IllegalStateException("Cannot load exclude names for DB2 connector");
					}
					excludeNames = new HashSet<String>();
					StringTokenizer tokenizer = new StringTokenizer(names,",\n",false);
					while(tokenizer.hasMoreTokens()){
						excludeNames.add(tokenizer.nextToken());
					}
				}
			}
		}
		return excludeNames;
	}
	
	static Collection<String> getExcludeNames(){
		return Collections.unmodifiableCollection(readExcludeNames());
	}
	
	static Collection<String> getExcludedNamePrefixes(){
		return Collections.unmodifiableCollection(excludedNamePrefixes);
	}
	
	static boolean isValidName(String name){
		return !getExcludeNames().contains(name) && !includesPrefix(name,excludedNamePrefixes);
	}
	
    /**
     *  Utility method for determining whether the passed target string
     *  begins with any of the passed prefixes.
     */
    private static boolean includesPrefix(String target, Collection<String> prefixes) {
        for (String prefix : prefixes) {
            if (target.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     *  Returns whether a passed string contains characters that DB2 deems
     *  to be illegal for use in account ids and passwords.  These characters
     *  come from the DB2 SQL Reference Manual.
     */
    static boolean containsIllegalDB2Chars(char[] target) {
        for (int i = 0; i < target.length; i++) {
            char c = target[i];
            // this hard coded rule is fast and simple for now.
            // if the complexity of the criteria ever increases,
            // it would be better to check in a Set or some such.
            if (!((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '@'
                    || c == '#'
                    || c == '$'))
            {
                return true;
            }
        }
        return false;
    }
    
    
	static void testConnection(Connection connection){
		//We will execute very simple " select 1 from sysibm.dual "
		PreparedStatement st = null;
		ResultSet rs = null;
		try{
			st = connection.prepareStatement("select 1 from sysibm.dual");
			rs = st.executeQuery();
		}
		catch(SQLException e){
			if("08001".equals(e.getSQLState()) && -4499 == e.getErrorCode()){
				throw new IllegalStateException("DB2 connection is stale",e);
			}
			else{
				throw new IllegalStateException("Unknown DB2 error while testing connection",e); 
			}
		}
		finally{
			SQLUtil.closeQuietly(rs);
			SQLUtil.closeQuietly(st);
		}
	}
	
	static Connection createType4Connection(String driver,String host,String port,String subprotocol,String database,String user,GuardedString password){
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("jdbc:").append(subprotocol);
		if(host != null && host.length() > 0){
			urlBuilder.append("://").append(host);
		}
		if(port != null){
			urlBuilder.append(":").append(port);
		}
		urlBuilder.append("/").append(database);
		return SQLUtil.getDriverMangerConnection(driver, urlBuilder.toString(), user, password);
	}
	
	static Connection createType2Connection(String driver,String aliasName,String subprotocol,String user,GuardedString password){
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("jdbc:").append(subprotocol).append(':');
		urlBuilder.append(aliasName);
		return SQLUtil.getDriverMangerConnection(driver, urlBuilder.toString(), user, password);
	}
	
	static Connection createDataSourceConnection(String dsName,Hashtable<?,?> env){
		return SQLUtil.getDatasourceConnection(dsName,env);
	}
	
	static Connection createDataSourceConnection(String dsName,String user,GuardedString password,Hashtable<?,?> env){
		return SQLUtil.getDatasourceConnection(dsName,user,password,env);
	}
	
    
}
