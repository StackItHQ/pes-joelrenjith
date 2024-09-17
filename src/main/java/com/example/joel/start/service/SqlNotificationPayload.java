package com.example.joel.start.service;

import org.json.JSONObject;
import java.util.HashMap;

public class SqlNotificationPayload {
    private String tableName;
    private String operation;
    private HashMap<String, String> data;
    private String primaryKey;

    public static SqlNotificationPayload fromNotification(String notification) {
        SqlNotificationPayload payload = new SqlNotificationPayload();

        String[] parts = notification.split(" - ");
        for (String part : parts) {
            String[] keyValue = part.split(": ", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                switch (key) {
                    case "Table":
                        payload.setTableName(value);
                        break;
                    case "Operation":
                        payload.setOperation(value);
                        break;
                    case "Data":
                        payload.setData(parseJson(value));
                        break;
                    case "PrimaryKey":
                        payload.setPrimaryKey(parseJson(value).get("id"));
                        break;
                }
            }
        }

        return payload;
    }

    private static HashMap<String, String> parseJson(String jsonString) {
        HashMap<String, String> map = new HashMap<>();
        try {
            JSONObject json = new JSONObject(jsonString);
            for (String key : json.keySet()) {
                map.put(key, json.getString(key));
            }
        } catch (Exception e) {
            // Log the error or handle it as needed
//            e.printStackTrace();
        }
        return map;
    }

    @Override
    public String toString() {
        return "SqlNotificationPayload{" +
                "tableName='" + tableName + '\'' +
                ", operation='" + operation + '\'' +
                ", data=" + data +
                ", primaryKey='" + primaryKey + '\'' +
                '}';
    }

    // Getters and setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public HashMap<String, String> getData() { return data; }
    public void setData(HashMap<String, String> data) { this.data = data; }

    public String getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(String primaryKey) { this.primaryKey = primaryKey; }
}