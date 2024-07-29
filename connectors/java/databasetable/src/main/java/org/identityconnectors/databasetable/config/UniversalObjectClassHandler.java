package org.identityconnectors.databasetable.config;

import org.identityconnectors.databasetable.DatabaseTableConfiguration;


public class UniversalObjectClassHandler extends DatabaseTableConfiguration {
    private DatabaseTableConfiguration config;
    private String objectClassName;

    private String table;
    private String keyColumn;
    private boolean enableWritingEmptyString;
    private String changeLogColumnSync;
    private String syncOrderColumn;
    private boolean syncOrderAsc;
    private boolean suppressPassword;
    private boolean allNative;
    private boolean nativeTimeStamp;

    public DatabaseTableConfiguration getConfig() {
        return config;
    }

    public void setConfig(DatabaseTableConfiguration config) {
        this.config = config;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public void setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public void setTable(String table) {
        this.table = table;
    }

    @Override
    public String getKeyColumn() {
        return keyColumn;
    }

    @Override
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public boolean getEnableWritingEmptyString() {
        return enableWritingEmptyString;
    }

    public void setEnableWritingEmptyString(boolean enableWritingEmptyString) {
        this.enableWritingEmptyString = enableWritingEmptyString;
    }

    public String getChangeLogColumnSync() {
        return changeLogColumnSync;
    }

    public void setChangeLogColumnSync(String changeLogColumnSync) {
        this.changeLogColumnSync = changeLogColumnSync;
    }

    @Override
    public String getSyncOrderColumn() {
        return syncOrderColumn;
    }

    @Override
    public void setSyncOrderColumn(String syncOrderColumn) {
        this.syncOrderColumn = syncOrderColumn;
    }

    @Override
    public Boolean getSyncOrderAsc() {
        return syncOrderAsc;
    }

    public void setSyncOrderAsc(Boolean syncOrderAsc) {
        this.syncOrderAsc = syncOrderAsc;
    }

    @Override
    public boolean getSuppressPassword() {
        return suppressPassword;
    }

    public void setSuppressPassword(boolean suppressPassword) {
        this.suppressPassword = suppressPassword;
    }

    public boolean getAllNative() {
        return allNative;
    }

    public void setAllNative(boolean allNative) {
        this.allNative = allNative;
    }

    public boolean getNativeTimeStamp() {
        return nativeTimeStamp;
    }

    public void setNativeTimeStamp(boolean nativeTimeStamp) {
        this.nativeTimeStamp = nativeTimeStamp;
    }
}
