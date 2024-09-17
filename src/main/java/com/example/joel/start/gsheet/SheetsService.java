package com.example.joel.start.gsheet;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
//import java.security.Permission;

@Service
public class SheetsService {

    private final Sheets sheetsService;
    private final Drive driveService;

    public SheetsService(Sheets sheetsService, Drive driveService) {
        this.sheetsService = sheetsService;
        this.driveService = driveService;
    }

    private static final String SPREADSHEETS_URL = "https://sheets.googleapis.com/v4/spreadsheets";

    public Spreadsheet createSpreadsheet(String title) throws IOException {
        System.out.println("Creating spreadsheet: " + title);

        // Create a new spreadsheet
        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties().setTitle(title));

        // Request to create the spreadsheet
        spreadsheet = sheetsService.spreadsheets().create(spreadsheet).execute();

        System.out.println("Spreadsheet created with ID: " + spreadsheet.getSpreadsheetId());

        // Make the spreadsheet public
        try {
            makePublic(spreadsheet.getSpreadsheetId());
        } catch (Exception e) {
            System.err.println("Error making spreadsheet public: " + e.getMessage());
            e.printStackTrace();
        }

        return spreadsheet;
    }

    public void makePublic(String fileId) throws IOException {
        System.out.println("Attempting to make spreadsheet public. File ID: " + fileId);

        Permission newPermission = new Permission()
                .setType("anyone")
                .setRole("writer");

        try {
            Permission result = driveService.permissions().create(fileId, newPermission)
                    .setFields("id")
                    .setTransferOwnership(true)
                    .execute();
            System.out.println("Spreadsheet made public. Permission ID: " + result.getId());
        } catch (Exception e) {
            System.err.println("Error setting permission: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addTableToSheet(String spreadsheetId, String sheetName, List<String> columnNames) throws IOException {
        // Define the range where the column headers will be placed
        String range = "!A1:" + getColumnLetter(columnNames.size()) + "1";

        // Prepare the values to be written
        List<List<Object>> values = new ArrayList<>();
        List<Object> row = new ArrayList<>(); // Create a row list

        // Add each column name as a separate cell in the row
        for (String columnName : columnNames) {
            row.add(columnName);
        }

        values.add(row);
        System.out.println("values size"+values.size());// Add column names to the first row
        System.out.println("values"+ values);

        ValueRange body = new ValueRange().setValues(values);

        UpdateValuesResponse result = sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED") // Use "RAW" to input data as-is or "USER_ENTERED" for user-friendly formatting
                .execute();

        System.out.println("Updated cells: " + result.getUpdatedCells());
    }

    public void insertData(String spreadsheetId, Map<String, String> data) throws IOException {
        // Fetch the existing data to search for the row where column A matches the value in the map
        String range =  "!A:A"; // We're only interested in column A for finding the correct row
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> existingData = response.getValues();

        int targetRow = -1; // Initialize target row to indicate not found
        try{
        String columnAValue = data.get("A").toString(); // Get the value for column A from the map

        // Search for the row where column A matches the value provided
        if (existingData != null) {
            for (int i = 0; i < existingData.size(); i++) {
                if (existingData.get(i).size() > 0 && columnAValue.equals(existingData.get(i).get(0).toString())) {
                    targetRow = i + 1; // Google Sheets rows are 1-based, so we add 1
                    break;
                }
            }
        }
        }
        catch (Exception e){
//            System.out.println("");
        }

        // If the row is not found, append data after the last filled row
        if (targetRow == -1) {
            targetRow = existingData.size() + 1; // Set target row to the next empty row
        }

        // Prepare the data to be inserted
        List<List<Object>> rowData = new ArrayList<>();
        List<Object> row = new ArrayList<>();

        // Determine the range dynamically based on the map keys (columns)
        List<String> columns = new ArrayList<>(data.keySet());
        columns.sort(String::compareTo); // Sort keys to maintain column order (e.g., A, B, C)

        for (String column : columns) {
            row.add(data.get(column)); // Add the corresponding value to the row
        }

        rowData.add(row);

        // Define the range where the data will be placed based on the first and last columns in the map
        String startColumn = columns.get(0); // First column in the map
        String endColumn = columns.get(columns.size() - 1); // Last column in the map
        String updateRange = "!" + startColumn + targetRow + ":" + endColumn + targetRow;

        // Prepare the values to be written
        ValueRange body = new ValueRange().setValues(rowData);
        UpdateValuesResponse result = sheetsService.spreadsheets().values()
                .update(spreadsheetId, updateRange, body)
                .setValueInputOption("USER_ENTERED") // Use "RAW" or "USER_ENTERED" based on your needs
                .execute();

        System.out.println("Updated cells: " + result.getUpdatedCells());
    }

    public void deleteRowIfExists(String spreadsheetId, Map<String, String> data) throws IOException {
        // Fetch the existing data to search for the row where column A matches the value in the map
        String range = "!A:A"; // We're only interested in column A for finding the correct row
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> existingData = response.getValues();

        int targetRow = -1; // Initialize target row to indicate not found
        String columnAValue = data.get("A").toString(); // Get the value for column A (the unique ID)

        // Search for the row where column A matches the value provided (unique ID)
        if (existingData != null) {
            for (int i = 0; i < existingData.size(); i++) {
                if (existingData.get(i).size() > 0 && columnAValue.equals(existingData.get(i).get(0).toString())) {
                    targetRow = i + 1; // Google Sheets rows are 1-based, so we add 1
                    break;
                }
            }
        }

        // If the row is found, delete the row
        if (targetRow != -1) {
            deleteRow(spreadsheetId, targetRow);
        } else {
            System.out.println("Row with ID " + columnAValue + " not found. No row deleted.");
        }
    }

    private void deleteRow(String spreadsheetId, int rowIndex) throws IOException {
        // Create a request to delete the row at the specified index
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(Collections.singletonList(
                        new Request().setDeleteDimension(new DeleteDimensionRequest()
                                .setRange(new DimensionRange()
                                        .setSheetId(getSheetId(spreadsheetId))
                                        .setDimension("ROWS")
                                        .setStartIndex(rowIndex - 1) // 0-based index
                                        .setEndIndex(rowIndex) // Exclusive end index
                                )
                        )
                ));

        // Execute the batch update request
        sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
        System.out.println("Deleted row: " + rowIndex);
    }

    // Helper method to get sheet ID based on the spreadsheet ID
    private int getSheetId(String spreadsheetId) throws IOException {
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
        return spreadsheet.getSheets().get(0).getProperties().getSheetId(); // Assumes the first sheet; adapt as needed
    }


    private String getColumnLetter(int columnIndex) {
        StringBuilder column = new StringBuilder();
        while (columnIndex > 0) {
            int modulo = (columnIndex - 1) % 26;
            column.insert(0, (char) (65 + modulo));
            columnIndex = (columnIndex - modulo) / 26;
        }
        return column.toString();
    }

    public void readAllContentFromSheet(String spreadsheetId, String sheetName) throws IOException {
        // Define the range for the entire sheet
        String range = "A3:C3"; // This reads all data in the specified sheet

        // Call the Sheets API to get the values
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        // Get the values from the response
        List<List<Object>> values = response.getValues();

        // Check if there are any values
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            // Print the values
            for (List<Object> row : values) {
                System.out.println(row);
            }
        }


    }
}
