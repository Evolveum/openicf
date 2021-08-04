package org.identityconnectors.databasetable.mapping.misc;

import java.util.Objects;

public class SQLColumnTypeInfo {

    private final String typeName;
    private final Integer typeCode;

    public SQLColumnTypeInfo(String typeName, Integer typeCode) {
        this.typeName = typeName;
        this.typeCode = typeCode;
    }

    public SQLColumnTypeInfo(Integer typeCode) {
        this(null, typeCode);
    }

    public String getTypeName() {
        return typeName;
    }

    public Integer getTypeCode() {
        return typeCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        final SQLColumnTypeInfo that = (SQLColumnTypeInfo) o;
        return Objects.equals(this.getTypeName(), that.getTypeName()) &&
                this.getTypeCode().equals(that.getTypeCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTypeName(), this.getTypeCode());
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        if (getTypeName() != null) {
            ret.append(getTypeName());
            ret.append("=");
        }

        if (getTypeCode() != null) {
            ret.append(getTypeCode());
            ret.append("=");
        }
        return ret.toString();
    }
}
