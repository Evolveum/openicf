package org.identityconnectors.oracle;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

/** Constants for Oracle connector */
final class OracleConstants {
	static final String ORACLE_AUTHENTICATION_ATTR_NAME = "oracleAuthentication";
	static final String ORACLE_GLOBAL_ATTR_NAME = "oracleGlobalName";
	static final String ORACLE_ROLES_ATTR_NAME = "oracleRoles";
	static final String ORACLE_PRIVS_ATTR_NAME = "oraclePrivs";
	static final String ORACLE_PROFILE_ATTR_NAME = "oracleProfile";
	static final String ORACLE_DEF_TS_ATTR_NAME = "oracleDefaultTS";
	static final String ORACLE_TEMP_TS_ATTR_NAME = "oracleTempTS";
	static final String ORACLE_DEF_TS_QUOTA_ATTR_NAME = "oracleDefaultTSQuota";
	static final String ORACLE_TEMP_TS_QUOTA_ATTR_NAME = "oracleTempTSQuota";
	static final String ORACLE_AUTH_LOCAL = "LOCAL";
	static final String ORACLE_AUTH_EXTERNAL = "EXTERNAL";
	static final String ORACLE_AUTH_GLOBAL = "GLOBAL";
	static final String NO_CASCADE = "noCascade";
	static final Collection<String> ALL_ATTRIBUTE_NAMES;
	
	static {
        Collection<String> tmp = new HashSet<String>();
        tmp.addAll(Arrays.asList(
				OracleConstants.ORACLE_AUTHENTICATION_ATTR_NAME, OracleConstants.ORACLE_GLOBAL_ATTR_NAME,
				OracleConstants.ORACLE_ROLES_ATTR_NAME, OracleConstants.ORACLE_PRIVS_ATTR_NAME,
				OracleConstants.ORACLE_PROFILE_ATTR_NAME, OracleConstants.ORACLE_DEF_TS_ATTR_NAME,
				OracleConstants.ORACLE_TEMP_TS_ATTR_NAME, OracleConstants.ORACLE_DEF_TS_QUOTA_ATTR_NAME,
				OracleConstants.ORACLE_TEMP_TS_QUOTA_ATTR_NAME,
				OperationalAttributes.PASSWORD_EXPIRED_NAME,OperationalAttributes.PASSWORD_EXPIRATION_DATE_NAME,
				OperationalAttributes.ENABLE_NAME,OperationalAttributes.DISABLE_DATE_NAME,
				Name.NAME,OperationalAttributes.PASSWORD_NAME,OperationalAttributes.LOCK_OUT_NAME
				));
        ALL_ATTRIBUTE_NAMES = Collections.unmodifiableCollection(tmp);
	}

	private OracleConstants(){}
}
