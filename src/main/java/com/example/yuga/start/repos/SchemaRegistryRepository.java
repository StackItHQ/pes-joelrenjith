package com.example.yuga.start.repos;

import com.example.yuga.start.service.SqlNotificationPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SchemaRegistryRepository {

    private final JdbcTemplate jdbcTemplate;

    public SchemaRegistryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public boolean tableExists(String tableName) {
        String query = "SELECT COUNT(*) FROM schema_registry WHERE table_name = ?";
        int count = jdbcTemplate.queryForObject(query, new Object[]{tableName}, Integer.class);
        System.out.println("count="+count);
        return count > 0;
    }

    public String gsheetcolName(String tableName,String sqlcolName){
        String query = "SELECT gsheetcol FROM schema_registry WHERE table_name = ? and column_name = ?";
        String gsheetcol = jdbcTemplate.queryForObject(query, new Object[]{tableName,sqlcolName},String.class );
        return gsheetcol;
    }

    public List<String> getColumnsForTable(String tableName) {
        String query = "SELECT column_name FROM schema_registry WHERE table_name = ?";
        return jdbcTemplate.queryForList(query, new Object[]{tableName}, String.class);
    }

    public void insertSchema(String tableName, Map<String,String> columns){
        int i =0;
        for (Map.Entry<String, String> column : columns.entrySet()) {
            String gsheetcol = getGoogleSheetColumn(i++);
            String insertMetadataSql = "INSERT INTO schema_registry (table_name, column_name, column_type,gsheetcol) VALUES (?, ?, ?,?)";
            jdbcTemplate.update(insertMetadataSql, tableName, column.getKey(), column.getValue(),gsheetcol);
        }

        System.out.println("Table created and schema registered: " + tableName);
    }
    public String getGoogleSheetColumn(int index) {
        StringBuilder column = new StringBuilder();
        while (index >= 0) {
            column.append((char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }
        return column.reverse().toString();  // Return column letter only, without row number
    }

    public Map<String, String> getGSheetColumnMapping(SqlNotificationPayload payload) {
        // Extract the table name
        String tableName = extractTableName(payload);
        System.out.println("Extracted table name: " + tableName);

        // Query to get the column mappings
        String query = "SELECT column_name, gsheetcol " +
                "FROM schema_registry " +
                "WHERE table_name = ? AND gsheetcol IS NOT NULL";

        // Execute the query and build the mapping
        Map<String, String> gsheetMapping = new HashMap<>();
        jdbcTemplate.query(query, new Object[]{tableName}, (rs) -> {
            String columnName = rs.getString("column_name");
            String gsheetCol = rs.getString("gsheetcol");
            String value = payload.getData().get(columnName);
            System.out.println("Mapping found: column=" + columnName + ", gsheetCol=" + gsheetCol + ", value=" + value);
            if (value != null) {
                gsheetMapping.put(gsheetCol, value);
            }
        });

        System.out.println("Final GSheet mapping: " + gsheetMapping);
        return gsheetMapping;
    }

    private String extractTableName(SqlNotificationPayload payload) {
        // First, try to extract from the type field
        String[] typeParts = payload.getType().split(" ", 2);
        if (typeParts.length > 1) {
            return typeParts[1].toLowerCase().trim();
        }

        // If that fails, use a default table name or another method to determine it
        return "";  // Default to 'boys' if we can't extract it from the type
    }

    public void insertIntoSchemaLink(String link, String tableName) {
        String query = "INSERT INTO schema_link (link, table_name) VALUES (?, ?)";

        jdbcTemplate.update(query, link, tableName);
        System.out.println("updates link");
    }

   public List<String>  getTableandprimarykeyfomschemalink(String link){
       String query = "SELECT table_name FROM schema_link WHERE link = ?";
       String table_name = jdbcTemplate.queryForObject(query, new Object[]{link},String.class );
       System.out.println("table is "+table_name);
       List<String> details = new ArrayList<>();
       details.add(table_name);
       query = "SELECT column_name " +
               "FROM schema_registry " +
               "WHERE table_name = ? " +
               "AND column_type LIKE '%PRIMARY KEY%'";

       String pkey = jdbcTemplate.queryForObject(query, new Object[]{table_name},String.class );
       details.add(pkey);
       return details;

   }

   public String getLink(String table_name){
       String query = "SELECT link FROM schema_link WHERE table_name = ?";
       String link = jdbcTemplate.queryForObject(query, new Object[]{table_name},String.class );
       return link;
   }




}
