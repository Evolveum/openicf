package org.identityconnectors.databasetable.mapping.misc;

public class JoinDef {
    private final String type;     // "JOIN", "LEFT JOIN", "INNER JOIN", ...
    private final String table;    // reálna tabuľka
    private final String alias;    // môže byť null
    private final String onClause; // surový ON výraz

    public JoinDef(String type, String table, String alias, String onClause) {
        this.type = type;
        this.table = table;
        this.alias = alias;
        this.onClause = onClause;
    }
    public String getType() { return type; }
    public String getTable() { return table; }
    public String getAlias() { return alias; }
    public String getOnClause() { return onClause; }

    @Override
    public String toString() {
        return "JoinDef{" +
                "type='" + type + '\'' +
                ", table='" + table + '\'' +
                ", alias='" + alias + '\'' +
                ", onClause='" + onClause + '\'' +
                '}';
    }
}