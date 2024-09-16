package com.example.yuga.start;


import com.example.yuga.start.gsheet.SheetsService;
import com.example.yuga.start.service.DynamicTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class StartApplication implements CommandLineRunner {

	@Autowired
	DynamicTableService dynamicTableService;
	@Autowired
	private SheetsService gglsheetservice;
//    @Autowired
//    private GoogleSheetsConfig googleSheetsConfig;

	public static void main(String[] args) {
		SpringApplication.run(StartApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Example table name and columns
		Scanner in  = new Scanner(System.in);
//		String tableName = "students";
//		Map<String, String> columns = Map.of(
//				"srn", "TEXT PRIMARY KEY",
//				"name", "VARCHAR(255)",
//				"mail", "VARCHAR(255)"
//		);
		System.out.println("want to create new table?");
		if(in.next().equals("yes")) {
			System.out.println("enter sheet id");
			String sheetid = in.next();
			System.out.println("enter table name");
			String tableName = in.next();
			System.out.println("enter columns");
			HashMap<String, String> columns = new LinkedHashMap<>();
			System.out.println("enter primary key");
			String pkey = in.next();
			columns.put(pkey, "TEXT PRIMARY KEY");
			System.out.println("enter other columns");
			String column = in.next();
			while (!column.equals("exit")) {
				columns.put(column, "VARCHAR(255)");
				column = in.next();
			}
			// Create table dynamically
			dynamicTableService.createTable(sheetid,tableName, columns);
			System.out.println("Created in sql table: " + tableName);

			gglsheetservice.addTableToSheet(sheetid, tableName, new ArrayList<>(columns.keySet()));
			System.out.println("Created in gsheets table: " + tableName);
		}
//		System.out.println("enter sheet id");
//		String id = in.next();
		// Insert data dynamically
//		Map<String, Object> rowData = Map.of(
//				"srn", "PES1UG21CS247",
//				"name", "John Doe",
//				"mail", "john.doe@example.com"
//		);
//		dynamicTableService.insertData(tableName, rowData);
//		System.out.println("Inserted data into table: " + tableName);
////
//		// Query and print data dynamically
//		var result = dynamicTableService.getAllData(tableName);
//		System.out.println("Data from table: " + result);

    }
}
