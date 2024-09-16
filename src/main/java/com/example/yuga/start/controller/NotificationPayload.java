package com.example.yuga.start.controller;

public class NotificationPayload {
    private String sheetid;
    private String columnName;
    private String oldValue;
    private String newValue;
    private String id; // Use Integer to handle null values
    private String timestamp;

    // Getters and Setters


    public NotificationPayload() {
    }

    public String getSheetid() {
        return sheetid;
    }

    public void setSheetid(String sheetid) {
        this.sheetid = sheetid;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "NotificationPayload{" +
                "sheetid='" + sheetid + '\'' +
                ", columnName='" + columnName + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", id=" + id +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
