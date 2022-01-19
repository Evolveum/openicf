package org.identityconnectors.databasetable;

public enum Quoting {

    BACKSLASH("BACKSLASH", "\\"),
    BRACKETS("BRACKETS", "[]"),
    DOUBLE("DOUBLE", "\""),
    SINGLE("SINGLE", "'"),
    NONE("NONE", ""),
    NULL(null, null);


    private String type;
    private String value;

    Quoting(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public static final Quoting compareAndFetch(String type) {

        if (BACKSLASH.type.equalsIgnoreCase(type)) {

            return BACKSLASH;
        } else if (BRACKETS.type.equalsIgnoreCase(type)) {

            return BRACKETS;
        } else if (DOUBLE.type.equalsIgnoreCase(type)) {

            return DOUBLE;
        } else if (SINGLE.type.equalsIgnoreCase(type)) {

            return SINGLE;
        } else if (SINGLE.type.equalsIgnoreCase(type)) {

            return SINGLE;
        }

        return NULL;
    }

    public static final String printAll() {

        StringBuilder builder = new StringBuilder();

        builder.append(BACKSLASH).append(";").append(BRACKETS).append(";").append(DOUBLE)
                .append(";").append(SINGLE).append(";").append(NONE);

        return builder.toString();
    }

    @Override
    public String toString() {

        return "Quote type=" + getType() + " and quote value=" + getValue();
    }

}
