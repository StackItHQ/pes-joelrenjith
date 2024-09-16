package com.example.yuga.start.controller;

import com.example.yuga.start.service.DynamicTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
@CrossOrigin(origins = "*",allowedHeaders = "*")
public class NotificationController {

    @Autowired
    DynamicTableService dynamicTableService;
//    @GetMapping("/notification")
//    public void handleNotification() {
//        // Process the payload
//        System.out.println("Received notification: " );
//
//        // Example: Log the notification data
//        // Log or handle the notification data as needed
//    }
    @PostMapping("/notifications")
    public void handleNotifications(@RequestBody NotificationPayload payload) {
        // Process the payload
        System.out.println("Received notifications: " + payload);
        dynamicTableService.scanpayload(payload);

        // Example: Log the notification data
        // Log or handle the notification data as needed
    }
}
