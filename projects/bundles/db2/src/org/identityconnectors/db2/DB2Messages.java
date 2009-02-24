package org.identityconnectors.db2;

abstract class DB2Messages {

    static final String VALIDATE_FAIL = "db2.validate.fail";
    static final String JDBC_DRIVER_CLASS_NOT_FOUND = "db2.jdbcDriverClassNotFound";
    static final String USERNAME_LONG = "db2.username.long";
    static final String USERNAME_CONTAINS_ILLEGAL_CHARACTERS = "db2.username.contains.illegal.characters";
    static final String USERNAME_IS_RESERVED_WORD = "db2.username.is.reserved.word";
    static final String UNSUPPORTED_OBJECT_CLASS = "db2.unsupported.object.class";
    static final String AUTHENTICATE_INVALID_CREDENTIALS = "db2.authenticate.invalid.credentials";
    static final String NAME_IS_NULL_OR_EMPTY = "db2.name.is.null.or.empty";
    static final String CREATE_OF_USER_FAILED = "db2.create.of.user.failed";
    static final String USER_ALREADY_EXISTS = "db2.user.already.exists";
    static final String USER_NOT_EXISTS = "db2.user.not.exists";
    static final String DELETE_OF_USER_FAILED = "db2.delete.of.user.failed";
    static final String NAME_IS_NOT_UPDATABLE = "db2.name.is.not.updatable";
    static final String UPDATE_OF_USER_FAILED = "db2.update.of.user.failed";
    static final String UPDATE_UID_CANNOT_BE_NULL_OR_EMPTY = "db2.update.uid.cannot.be.null.or.empty";
    static final String SEARCH_FAILED = "db2.search.failed";
}
