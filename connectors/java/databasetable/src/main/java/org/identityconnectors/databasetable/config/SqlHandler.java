package org.identityconnectors.databasetable.config;

import org.identityconnectors.databasetable.DatabaseTableConfiguration;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SqlHandler {

    private String table;
    private String whereClause;
    private Set<String> columns;
    private String sqlQuery;
    public SqlHandler(DatabaseTableConfiguration config) {
        this.parseSql(config);
    }
    public void parseSql(DatabaseTableConfiguration config) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(config.getSqlFilePath()));

            // Read the SQL file content
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sqlBuilder.append(line).append("\n");
            }

            // Extract parts of the SQL Query
            this.setSqlQuery(sqlBuilder.toString());
            this.setTableName(sqlBuilder.toString());
            this.setColumns(sqlBuilder.toString());
            this.setWhereClause(sqlBuilder.toString());

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setSqlQuery(String sqlQuery) {
        sqlQuery = sqlQuery.replaceAll("\\s+", " ").trim();
        this.sqlQuery = sqlQuery;
    }

    private void setTableName(String createViewSQL) {
        String fromKeyword = " FROM ";

        // Normalize SQL query to handle multi-line and case insensitivity
        createViewSQL = createViewSQL.replaceAll("\\s+", " ").trim();

        int startIndex = createViewSQL.indexOf(fromKeyword) + fromKeyword.length();
        if (startIndex == -1) {
            throw new RuntimeException("FROM keyword does not exist.");
        }
        int endIndex = createViewSQL.indexOf(" ", startIndex);
        if (endIndex == -1) {
            endIndex = createViewSQL.length(); // Handle end of statement
        }
        this.table = createViewSQL.substring(startIndex, endIndex).trim();
    }

    private void setColumns(String sqlQuery) {
        String selectKeyword = "SELECT ";
        String fromKeyword = " FROM";

        // Normalize SQL query to handle multi-line and case insensitivity
        sqlQuery = sqlQuery.replaceAll("\\s+", " ").trim();

        int startIndex = sqlQuery.indexOf(selectKeyword) + selectKeyword.length();
        if (startIndex == -1 + selectKeyword.length()) {
            throw new RuntimeException("SELECT keyword does not exist.");
        }

        int endIndex = sqlQuery.indexOf(fromKeyword, startIndex);
        if (endIndex == -1) {
            throw new RuntimeException("Error finding the end of the column list (start of the FROM clause).");
        }

        String columnsString = sqlQuery.substring(startIndex, endIndex).trim();
        String[] columnsArray = columnsString.split(",\\s*");
        this.columns = new HashSet<>(Arrays.asList(columnsArray));
    }

    private void setWhereClause(String createViewSQL) {
        String whereKeyword = " WHERE ";

        // Normalize SQL query to handle multi-line and case insensitivity
        createViewSQL = createViewSQL.replaceAll("\\s+", " ").trim();

        int startIndex = createViewSQL.indexOf(whereKeyword);
        if (startIndex == -1) {
            this.whereClause = null;;
        } else {
            startIndex += whereKeyword.length();
            int endIndex = createViewSQL.indexOf(";", startIndex);
            if (endIndex == -1) {
                endIndex = createViewSQL.length();
            }

            this.whereClause = createViewSQL.substring(startIndex, endIndex).trim();
        }
    }

    public String getSqlQuery() {
        return this.sqlQuery;
    }

    public String getTable() {
        return this.table;
    }

    public Set<String> getColumns() {
        return this.columns;
    }

    public String getWhereClause() {
        return this.whereClause;
    }
}
