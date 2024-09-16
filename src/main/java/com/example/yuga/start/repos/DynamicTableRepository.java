package com.example.yuga.start.repos;

import com.example.yuga.start.controller.NotificationPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class DynamicTableRepository {

    private final JdbcTemplate jdbcTemplate;

    public DynamicTableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Fetch all records dynamically
    public List<Map<String, Object>> findAll(String tableName) {
        String query = String.format("SELECT * FROM %s", tableName);
        return jdbcTemplate.queryForList(query);
    }

    // Insert data dynamically
    public void insertrow(String tableName, String column, String value) {
        // Properly format the SQL query
        String query = String.format("INSERT INTO %s (%s) VALUES (?)", tableName, column);

        // Execute the query with the value parameter
        jdbcTemplate.update(query, value);
    }

    // Update data dynamically
    public void updateData(String tableName, String key, String val, String pkey, String pkval) {
        // Construct the SQL query with the table name and column names directly
        String query = String.format("UPDATE %s SET %s = ? WHERE %s = ?", tableName, key, pkey);

        // Execute the update operation with the actual values for placeholders
        jdbcTemplate.update(query, val, pkval);
    }


    // Delete data dynamically
    public void deleteData(String tableName, String pkey, String pkeyval) {
        String query = String.format("DELETE FROM %s WHERE %s = ?", tableName, pkey);

        System.out.println("Executing query: " + query);
        System.out.println("With value: " + pkeyval);

        // Ensure `pkeyval` is properly handled if it's a string
        int rowsAffected = jdbcTemplate.update(query, pkeyval);

        System.out.println("Rows affected: " + rowsAffected);
    }




}
