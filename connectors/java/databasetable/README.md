
## Table of Contents
1. [Introduction](#introduction)
2. [Capabilities and Features](#capabilities-and-features)
3. [JSON Mode](#json-mode)
4. [Custom SQL Query Mode](#custom-sql-query-mode)
5. [Basic Mode](#basic-mode)
6. [JDBC Driver](#jdbc-driver)
7. [Limitations](#limitations)
8. [Build](#build)

# Introduction
### databasetable-connector
Identity connector for generic relational database table.

# Capabilities and Features
- Schema: YES
- Provisioning: YES
- Live Synchronization: YES - Using last modification timestamps
- Password: YES
- Activation: SIMULATED - Configured capability
- Script execution: No

# Connector Modes
- Connector supports 3 different modes
- Modes can be changed via configuration in resource
- Three supported modes are: `JSON`, `CUSTOM_SQL_QUERY` and `BASIC`

## JSON Mode
- JSON Mode supports arbitrary number of ObjectClasses and their attributes
- When using JSON mode each object class needs to be defined in JSON file
- JSON File should be properly secured and protected with appropriate permissions
- Database, connector mode and credentials need to be defined in configuration as usual
- Every ObjectClass defined in JSON File must have the following parameters:

```
    {
      "objectClassName": "AnyObjectClass",
      "configurationProperties": {
        "Table": "TableName",
        "Key Column": "KeyColumnName",
        "Enable writing empty string": true,
        "Change Log Column (Sync)": "ChangeLogColumn",
        "Sync Order Column": "SyncOrderColumn",
        "Sync Order Asc": true,
        "Suppress Password": true
      }
    }
```
- For better understanding use `JSON_example.json` in the `samples` folder 

## Custom SQL Query Mode
- In custom SQL Query mode the connectors expects SQL Query file.
- Where clause is supported
- Sql Query can be in-line or with each part in new line
- Using aggregate functions is supported
- Key column, database, connector mode and credentials need to be defined in configuration as usual 
- Example `SQL_query.sql` can be found inside the `samples` folder
- Columns and table need to be defined in the sql query as shown:
```
SELECT column1, column2 FROM table
```
or
```
SELECT column1 as col1, column2 as col2 FROM table
```

## Basic Mode
- Configuration requires specifying the key column, database, connector mode, database table and credentials.
- An example configuration file `resource_basic.xml` can be found inside the `samples` folder.
- 

## JDBC Driver
- Supported JDBC Drivers are:
  - MySQL Connector/J 5.1.6
  - Microsoft SQL Server 2005 JDBC Driver 1.2
  - Oracle Database 10g Release 2 (10.2.0.2) JDBC Driver
  - Apache Derby 10.4 
- JDBC driver and it respective Url template:

| jdbcDriver                                                                          | jdbcUrlTemplate                        |
|-------------------------------------------------------------------------------------|:---------------------------------------|
| oracle.jdbc.driver.OracleDriver, org.apache.derby.jdbc.EmbeddedDriver (for testing) | jdbc:oracle:thin:@%h:%p:%d             |
| com.mysql.cj.jdbc.Driver, com.mysql.jdbc.Driver                                     | jdbc:mysql://%h:%p/%d                  |
| org.postgresql.Driver                                                               | jdbc:postgresql://%h:%p/%d             |
| com.microsoft.sqlserver.jdbc.SQLServerDriver                                        | jdbc:sqlserver://%h:%p;databaseName=%d;|

## Limitations
There are few limitation for specific connector modes.
### SQL QUERY
- Does not support create, update or delete

### BASIC
- Does not support multiple database tables

## Build
```
mvn clean install
```
## Build without Tests
```
mvn clean install -DskipTests=True
```
After successful build, you can find connector-databasetable-1.5.2.0-SNAPSHOT.jar in target directory.

## TODO
- An update to support new associations in version 4.9, this will enable the connector to handle traditional database relations.

# Status
- Tested only on MySQL 9.0.0
- Tested with MidPoint version 4.8.1. 