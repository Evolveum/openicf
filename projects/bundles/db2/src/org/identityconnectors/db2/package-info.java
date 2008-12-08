/**
 * DB2 Connector uses DB2 database resource to manage users. DB2 uses external authentication provider and internal authorization service.
 * DB2 connector is then pretty limited and should be used with combination of underlying authorization service connector, typically
 * OS connector, LDAP. DB2 connector stores users using passed grants. 
 * 
 * See {@link org.identityconnectors.db2.DB2Configuration} and  {@link org.identityconnectors.db2.DB2Connector} for more information about DB2 resource.
 * 
 */
package org.identityconnectors.db2;

