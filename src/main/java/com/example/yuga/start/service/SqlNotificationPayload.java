package com.example.yuga.start.service;

import org.json.JSONObject;
import java.util.HashMap;

public class SqlNotificationPayload {
    private String type;
    private HashMap<String, String> data;
    private String pkey;
    private String tableName;
    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public void setData(HashMap<String, String> data) {
        this.data = data;
    }

    public String getPkey() {
        return pkey;
    }

    public void setPkey(String pkey) {
        this.pkey = pkey;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    // Method to parse notification and fill SqlNotificationPayload
    public static SqlNotificationPayload fromNotification(String notification) {
        SqlNotificationPayload payload = new SqlNotificationPayload();

        String[] parts = notification.split(" - ", 4); // Split into 4 parts maximum
        if (parts.length >= 3) {
            // Extract table name
            String[] tableParts = parts[0].split(":", 2);
            if (tableParts.length == 2) {
                payload.setTableName(tableParts[1].trim());
            }

            // Extract operation type
            String[] operationParts = parts[1].split(":", 2);
            if (operationParts.length == 2) {
                payload.setType(operationParts[1].trim());
            }

            // Extract data and primary key
            String dataAndPkPart = parts[parts.length - 1]; // Last part contains data and primary key
            String[] dataParts = dataAndPkPart.split("PrimaryKey:", 2);
            if (dataParts.length == 2) {
                String dataJson = dataParts[0].replace("Data:", "").trim();
                String pkJson = dataParts[1].trim();

                HashMap<String, String> dataMap = new HashMap<>();

                // Parse Data JSON
                JSONObject dataObject = new JSONObject(dataJson);
                dataObject.keys().forEachRemaining(key -> dataMap.put(key, dataObject.getString(key)));

                // Parse PrimaryKey JSON
                JSONObject pkObject = new JSONObject(pkJson);
                if (pkObject.length() > 0) {
                    String pkKey = pkObject.keys().next();
                    String pkValue = pkObject.getString(pkKey);
                    payload.setPkey(pkValue);
                    // Also add the primary key to the data map if it's not already there
                    if (!dataMap.containsKey(pkKey)) {
                        dataMap.put(pkKey, pkValue);
                    }
                }

                payload.setData(dataMap);
            }
        }

        return payload;
    }

    @Override
    public String toString() {
        return "SqlNotificationPayload{" +
                "type='" + type + '\'' +
                ", data=" + data +
                ", pkey='" + pkey + '\'' +
                '}';
    }
}
