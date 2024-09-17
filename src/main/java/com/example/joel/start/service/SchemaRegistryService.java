package com.example.joel.start.service;

import com.example.joel.start.repos.SchemaRegistryRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class SchemaRegistryService {

    private final SchemaRegistryRepository schemaRegistryRepository;

    public SchemaRegistryService(SchemaRegistryRepository schemaRegistryRepository) {
        this.schemaRegistryRepository = schemaRegistryRepository;
    }

    public boolean validateTableExists(String tableName) {
        // Check if the table exists in the registry
        //            throw new IllegalArgumentException("Table " + tableName + " does not exist");
        System.out.println("table existence ="+schemaRegistryRepository.tableExists(tableName));
        return schemaRegistryRepository.tableExists(tableName);
    }

    public void validateColumns(String tableName, Map<String, Object> data) {
        // Fetch columns from the registry
        List<String> validColumns = schemaRegistryRepository.getColumnsForTable(tableName);
        System.out.println("the colums are:"+validColumns.toString());
        for (String column : data.keySet()) {
            if (!validColumns.contains(column)) {
                throw new IllegalArgumentException("Invalid column " + column + " for table " + tableName);
            }
        }
    }

    public void addTable(String tableName, Map<String, String> columns){
        schemaRegistryRepository.insertSchema(tableName,columns);
    }

    public void addschemalink(String sheetid,String tablename){
        schemaRegistryRepository.insertIntoSchemaLink(sheetid,tablename);
    }

    public List<String> getTablefomschemalink(String sheetid){
        return  schemaRegistryRepository.getTableandprimarykeyfomschemalink(sheetid);
    }
}
