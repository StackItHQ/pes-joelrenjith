package com.example.joel.start.service;

import com.example.joel.start.gsheet.SheetsService;
import com.example.joel.start.repos.SchemaRegistryRepository;
import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Statement;
import java.util.Map;

@Service
public class PostgresNotificationService {

    private final JdbcTemplate jdbcTemplate;
    private final SchemaRegistryRepository schemaRegistryRepository;
    private final SheetsService sheetsService;
    private PgConnection pgConn;

    public PostgresNotificationService(JdbcTemplate jdbcTemplate, SchemaRegistryRepository schemaRegistryRepository, SheetsService sheetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaRegistryRepository = schemaRegistryRepository;
        this.sheetsService = sheetsService;
    }

    @PostConstruct
    public void startListener() throws Exception {
        // Unwrap the existing connection to get PGConnection
        pgConn = jdbcTemplate.getDataSource().getConnection().unwrap(PgConnection.class);

        // Create a statement to listen for notifications
        Statement stmt = pgConn.createStatement();
        stmt.execute("LISTEN table_changes");

        // Run the listener in a separate thread to avoid blocking the main thread
        new Thread(this::listenForChanges).start();
    }

    private void listenForChanges() {
        try {
            System.out.println("Listening for notifications...");

            while (true) {
                // Check for notifications
                PGNotification[] notifications = pgConn.getNotifications();

                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        // Handle the notification
//                        System.out.println("Received notification: " + notification.getParameter().formatted());
                        String ans = notification.getParameter();
                        System.out.println("ans = "+ans);
                        SqlNotificationPayload sqlNotificationPayload = SqlNotificationPayload.fromNotification(notification.getParameter());
                        // Further processing logic (e.g., trigger service or handle data)
                        System.out.println("sql="+ sqlNotificationPayload);

                        Map<String, String> mapping =schemaRegistryRepository.getGSheetColumnMapping(sqlNotificationPayload);
                        String sheetId = schemaRegistryRepository.getLink(sqlNotificationPayload.getTableName());
                        if(sqlNotificationPayload.getOperation().equals("DELETE")){
                                sheetsService.deleteRowIfExists(sheetId,mapping);
                        }
                        else {
                            sheetsService.insertData(sheetId, mapping);
                        }

                    }
                }

                // Sleep to reduce CPU usage
                Thread.sleep(500);
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stopListener() throws Exception {
        // Close the connection when shutting down the service
        if (pgConn != null) {
            pgConn.close();
        }
    }
}
