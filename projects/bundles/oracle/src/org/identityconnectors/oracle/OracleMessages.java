package org.identityconnectors.oracle;

/** All constants for oracle messages that will be translated */
class OracleMessages {
	static final String MSG_CONNECTOR_DISPLAY = "oracle.connector.display";
	static final String MSG_ATTRIBUTE_IS_MISSING = "oracle.attribute.is.missing";
	static final String MSG_HOST_DISPLAY = "oracle.host.display";
	static final String MSG_HOST_HELP = "oracle.host.help";
	static final String MSG_PORT_DISPLAY = "oracle.port.display";
	static final String MSG_PORT_HELP = "oracle.port.help";
	static final String MSG_DRIVER_DISPLAY = "oracle.driver.display";
	static final String MSG_DRIVER_HELP = "oracle.driver.help";
	static final String MSG_DATABASE_DISPLAY = "oracle.database.display";
	static final String MSG_DATABASE_HELP = "oracle.database.help";
	static final String MSG_USER_DISPLAY = "oracle.user.display";
	static final String MSG_USER_HELP = "oracle.user.help";
	static final String MSG_PASSWORD_DISPLAY = "oracle.password.display";
	static final String MSG_PASSWORD_HELP = "oracle.password.help";
	static final String MSG_DATASOURCE_DISPLAY = "oracle.datasource.display";
	static final String MSG_DATASOURCE_HELP = "oracle.datasource.help";
	static final String MSG_DSJNDIENV_DISPLAY = "oracle.dsjndienv.display";
	static final String MSG_DSJNDIENV_HELP = "oracle.dsjndienv.help";
	static final String MSG_URL_DISPLAY = "oracle.url.display";
	static final String MSG_URL_HELP = "oracle.url.help";
	static final String MSG_CS_DISPLAY = "oracle.cs.display";
	static final String MSG_CS_HELP = "oracle.cs.help";
	
	static final String ORACLE_EXTRA_ATTRS_POLICY_DISPLAY = "oracle.extra.attrs.policy.display";
	static final String ORACLE_EXTRA_ATTRS_POLICY_HELP = "oracle.extra.attrs.policy.help";
	static final String MSG_SOURCE_TYPE_DISPLAY = "oracle.source.type.display";
	static final String MSG_SOURCE_TYPE_HELP = "oracle.source.type.help";
	
	static final String MSG_USER_AND_PASSWORD_MUST_BE_SET_BOTH_OR_NONE = "oracle.user.and.password.must.be.set.both.or.none";
	static final String MSG_INVALID_AUTH = "oracle.invalid.auth";
	static final String MSG_INVALID_BRACKET = "oracle.invalid.bracket";
	static final String MSG_ILLEGAL_RIGHT_BRACKET = "oracle.illegal.right.bracket";
	static final String MSG_NO_EQ_CHAR_IN_ENTRY = "oracle.no.eq.char.in.entry";
	static final String MSG_CHAR_EQ_IS_AT_START_OF_ENTRY = "oracle.char.eq.is.at.start.of.entry";
	static final String MSG_ELEMENTS_FOR_NORMALIZER_NOT_RECOGNIZED = "oracle.elements.for.normalizer.not.recognized";
	static final String MSG_ELEMENTS_FOR_CSBUILDER_NOT_RECOGNIZED = "oracle.elements.for.csbuilder.not.recognized";
	static final String MSG_ERROR_TEST_USER_EXISTENCE = "oracle.error.test.user.existence";
	static final String MSG_USER_RECORD_NOT_FOUND = "oracle.user.record.not.found";
	static final String MSG_UPDATE_OF_USER_FAILED = "oracle.update.of.user.failed";
	static final String MSG_ADDATTRIBUTEVALUES_FOR_USER_FAILED = "oracle.addAttributeValues.for.user.failed"; 
	static final String MSG_REMOVEATTRIBUTEVALUES_FOR_USER_FAILED = "oracle.removeAttributeValues.for.user.failed";
	static final String MSG_UPDATE_ATTRIBUTE_NOT_SUPPORTED = "oracle.update.attribute.not.supported";
	static final String MSG_ADDATTRIBUTEVALUES_ATTRIBUTE_NOT_SUPPORTED = "oracle.addAttributeValues.attribute.not.supported";
	static final String MSG_REMOVEATTRIBUTEVALUES_ATTRIBUTE_NOT_SUPPORTED = "oracle.removeAttributeValues.attribute.not.supported";
	static final String MSG_UPDATE_NO_ATTRIBUTES = "oracle.update.no.attributes";
	static final String MSG_ERROR_EXECUTING_SEARCH = "oracle.error.executing.search";
	static final String MSG_SEARCH_ATTRIBUTE_NOT_SUPPORTED_FOR_ATTRIBUTESTOGET = "oracle.search.attribute.not.supported.for.attributesToGet";
	static final String MSG_SEARCH_ATTRIBUTE_NOT_SUPPORTED_FOR_SEARCHBY = "oracle.search.attribute.not.supported.for.searchBy";
	static final String MSG_DELETE_OF_USER_FAILED = "oracle.delete.of.user.failed";
	static final String MSG_MUST_SPECIFY_PASSWORD_FOR_UNEXPIRE = "oracle.must.specify.password.for.unexpire";
	static final String MSG_CREATE_NO_ATTRIBUTES = "oracle.create.no.attributes";
	static final String MSG_CREATE_ATTRIBUTE_NOT_SUPPORTED = "oracle.create.attribute.not.supported";
	static final String MSG_CREATE_OF_USER_FAILED = "oracle.create.of.user.failed";
	static final String MSG_MISSING_DEFAULT_TABLESPACE_FOR_QUOTA = "oracle.missing.default.tablespace.for.quota";
	static final String MSG_MISSING_TEMPORARY_TABLESPACE_FOR_QUOTA = "oracle.missing.temporary.tablespace.for.quota";
	static final String MSG_CANNOT_SET_GLOBALNAME_FOR_NOT_GLOBAL_AUTHENTICATION = "oracle.cannot.set.globalname.for.not.global.authentication";
	static final String MSG_CANNOT_SET_PASSWORD_FOR_NOT_LOCAL_AUTHENTICATION = "oracle.cannot.set.password.for.not.local.authentication";
	static final String MSG_CANNOT_EXPIRE_PASSWORD_FOR_NOT_LOCAL_AUTHENTICATION = "oracle.cannot.expire.password.for.not.local.authentication";
	static final String MSG_MISSING_GLOBALNAME_FOR_GLOBAL_AUTHENTICATION = "oracle.missing.globalname.for.global.authentication";
	static final String MSG_BOOLEAN_ATTRIBUTE_HAS_INVALID_VALUE = "oracle.boolean.attribute.has.invalid.value";
	static final String MSG_ATTRIBUTE_IS_EMPTY = "oracle.attribute.is.empty";
	static final String MSG_SET_DRIVER_OR_URL = "oracle.set.driver.or.url";
	static final String MSG_CANNOT_LOAD_DRIVER = "oracle.cannot.load.driver";
	static final String MSG_THIN_CONNECTION_ERROR = "oracle.thin.connection.error";
	static final String MSG_OCI_CONNECTION_ERROR = "oracle.oci.connection.error";
	static final String MSG_CUSTOM_CONNECTION_ERROR = "oracle.custom.connection.error";
	static final String MSG_DATASOURCE_CONNECTION_ERROR = "oracle.datasource.connection.error";
	static final String MSG_ENABLE_LOCK_ATTR_VALUE_CONFLICT_FALSE = "oracle.enable.lock.attr.value.conflict.false";
	static final String MSG_ENABLE_LOCK_ATTR_VALUE_CONFLICT_TRUE = "oracle.enable.lock.attr.value.conflict.true";
	static final String MSG_INVALID_SOURCE_TYPE = "oracle.invalid.source.type";
	static final String MSG_CS_MUST_SPECIFY_ONE_ARRAY_ELEMENT = "oracle.cs.must.specify.one.array.element";
	static final String MSG_EAP_MUST_SPECIFY_ONE_ARRAY_ELEMENT = "oracle.eap.must.specify.one.array.element";
	static final String ORACLE_EAP_INVALID_ELEMENTS_IN_MAP = "oracle.eap.invalid.elements.in.map";
	static final String ORACLE_EAP_CANNOT_RESOLVE_SPI_OPERATION = "oracle.eap.cannot.resolve.spi.operation";
	
	
	
}
