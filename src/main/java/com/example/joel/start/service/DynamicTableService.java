package com.example.joel.start.service;

import com.example.joel.start.controller.NotificationPayload;
import com.example.joel.start.repos.DynamicTableRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import static java.lang.String.format;

import java.util.Map;

@Service
public class DynamicTableService {

    private final DynamicTableRepository dynamicTableRepository;
    private final SchemaRegistryService schemaRegistryService;
    private final JdbcTemplate jdbcTemplate;

    public DynamicTableService(DynamicTableRepository dynamicTableRepository, SchemaRegistryService schemaRegistryService, JdbcTemplate jdbcTemplate) {
        this.dynamicTableRepository = dynamicTableRepository;
        this.schemaRegistryService = schemaRegistryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createTable(String sheetid, String tableName, Map<String, String> columns) {
        // Validate that the table does not exist in the registry
        if(schemaRegistryService.validateTableExists(tableName)){
            System.out.println("table"+tableName+"already exist");
            return;
        }

        // Build SQL for creating the table
        StringBuilder createTableSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        createTableSql.append(tableName).append(" (");

        // Append each column definition
        columns.forEach((columnName, columnType) ->
                createTableSql.append(columnName).append(" ").append(columnType).append(", ")
        );

        // Remove the last comma and space, then close the parenthesis
        createTableSql.setLength(createTableSql.length() - 2);
        createTableSql.append(")");
        System.out.println(createTableSql.toString());
        // Execute the SQL
        jdbcTemplate.execute(createTableSql.toString());

        System.out.println("Table created: " + tableName);
        schemaRegistryService.addTable(tableName,columns);
        createNotificationFunction(tableName, columns);
        createNotificationTrigger(tableName);
        schemaRegistryService.addschemalink(sheetid,tableName);
    }



    public List<Map<String, Object>> getAllData(String tableName) {
        // Fetch schema details from the metadata service if necessary
        schemaRegistryService.validateTableExists(tableName);
        return dynamicTableRepository.findAll(tableName);
    }

    public void insertrow(String tableName, String pkey,String pkeyval) {
        // Validate that the table and column names match the metadata
//        schemaRegistryService.validateColumns(tableName, data);
        dynamicTableRepository.insertrow(tableName,  pkey, pkeyval);
    }

    public void updateData(String tableName, String key,String val, String pkey, String pkval) {
        // Validate schema before update
//        schemaRegistryService.validateColumns(tableName, data);
        dynamicTableRepository.updateData(tableName, key,val,pkey,pkval);
    }

    public void deleteData(String tableName, String pkey,String pkeyval) {
//        String condition = "where %s = %s".formatted(pkey,pkeyval);
        double number = Double.parseDouble(pkeyval);
        pkeyval = Integer.toString((int)number);
        System.out.println("gonna delete");
        dynamicTableRepository.deleteData(tableName, pkey,pkeyval);
    }


    public void createNotificationFunction(String tableName, Map<String, String> columns) {
        String functionName = "notify_" + tableName + "_changes";

        List<String> primaryKeys = getPrimaryKeyColumns(tableName);

        // Reverse the order of the column names (if needed)
//        List<String> reversedColumns = new ArrayList<>(columns.keySet());
        // Collections.reverse(reversedColumns); // Uncomment if you want to reverse the order

        StringBuilder functionSql = new StringBuilder()
                .append("CREATE OR REPLACE FUNCTION ").append(functionName)
                .append("() RETURNS trigger AS $$\n")
                .append("DECLARE\n")
                .append("    operation TEXT;\n")
                .append("    row_data JSONB;\n")
                .append("    pk_data JSONB;\n")
                .append("BEGIN\n")
                .append("    IF TG_OP = 'INSERT' THEN\n")
                .append("        operation := 'INSERT';\n")
                .append("        row_data := jsonb_build_object(\n")
                .append(columns.keySet().stream()
                        .map(col -> String.format("            '%s', NEW.%s", col, col))
                        .collect(Collectors.joining(",\n")))
                .append("\n        );\n")
                .append("        pk_data := jsonb_build_object(\n")
                .append(primaryKeys.stream()
                        .map(pk -> String.format("            '%s', NEW.%s", pk, pk))
                        .collect(Collectors.joining(",\n")))
                .append("\n        );\n")
                .append("    ELSIF TG_OP = 'UPDATE' THEN\n")
                .append("        operation := 'UPDATE';\n")
                .append("        row_data := jsonb_build_object(\n")
                .append(columns.keySet().stream()
                        .map(col -> String.format("            '%s', NEW.%s", col, col))
                        .collect(Collectors.joining(",\n")))
                .append("\n        );\n")
                .append("        pk_data := jsonb_build_object(\n")
                .append(primaryKeys.stream()
                        .map(pk -> String.format("            '%s', NEW.%s", pk, pk))
                        .collect(Collectors.joining(",\n")))
                .append("\n        );\n")
                .append("    ELSIF TG_OP = 'DELETE' THEN\n")
                .append("        operation := 'DELETE';\n")
                .append("        row_data := jsonb_build_object(\n")
                .append(columns.keySet().stream()
                        .map(col -> String.format("            '%s', OLD.%s", col, col))
                        .collect(Collectors.joining(",\n")))
                .append("\n        );\n")
                .append("        pk_data := jsonb_build_object(\n")
                .append(primaryKeys.stream()
                        .map(pk -> String.format("            '%s', OLD.%s", pk, pk))
                        .collect(Collectors.joining(",\n")))
                .append("\n        );\n")
                .append("    END IF;\n")
                .append("    PERFORM pg_notify('table_changes', \n")
                .append("        format('Table: %s - Operation: %s - Data: %s - PrimaryKey: %s', \n")
                .append("            '").append(tableName).append("',\n")
                .append("            operation,\n")
                .append("            row_data::jsonb,\n")  // Ensure it's passed as valid JSONB
                .append("            pk_data::jsonb\n")    // Ensure primary key is passed as JSONB
                .append("        )\n")
                .append("    );\n")
                .append("    RETURN NEW;\n")
                .append("END;\n")
                .append("$$ LANGUAGE plpgsql;\n");


        // Log the SQL statement for debugging purposes
        System.out.println(functionSql.toString());

        // Execute the SQL to create the function
        jdbcTemplate.execute(functionSql.toString());
    }


    private List<String> getPrimaryKeyColumns(String tableName) {
        String query = "SELECT column_name " +
                "FROM schema_registry " +
                "WHERE table_name = ? " +
                "AND column_type LIKE '%PRIMARY KEY%'";

        return jdbcTemplate.query(query, new Object[]{tableName},
                (rs, rowNum) -> rs.getString("column_name"));
    }





    private void createNotificationTrigger(String tableName) {
        String functionName = "notify_" + tableName + "_changes";
        String triggerName = tableName + "_changes_trigger";
        StringBuilder triggerSql = new StringBuilder()
                .append("CREATE TRIGGER ").append(triggerName)
                .append("\nAFTER INSERT OR UPDATE OR DELETE ON ").append(tableName)
                .append("\nFOR EACH ROW\n")
                .append("EXECUTE FUNCTION ").append(functionName).append("();\n");

        System.out.println(triggerSql.toString());

        // Execute the SQL to create the trigger
        jdbcTemplate.execute(triggerSql.toString());
    }

    public void scanpayload(NotificationPayload payload){
        List<String> details = schemaRegistryService.getTablefomschemalink(payload.getSheetid());
        String pkey = details.get(1);
        String table = details.get(0);
        System.out.println("pkey size"+ payload.getId().length());
        if(payload.getColumnName().equals(pkey)){
            if(payload.getId()==null || payload.getId().isEmpty()){
                System.out.println("gonna delete");
                deleteData(table,pkey,payload.getOldValue());
            }
            else {
                insertrow(table,pkey, payload.getNewValue());
            }
        }
        else{
            String column_name = payload.getColumnName();
            String val = payload.getNewValue();
            String pkeyval = payload.getId();
            System.out.println("column name"+column_name);
            System.out.println("val"+val);
            System.out.println("pkey"+pkey);
            System.out.println("pkeyval "+pkeyval);
            updateData(table, column_name, val,pkey,pkeyval);
        }
    }


}
