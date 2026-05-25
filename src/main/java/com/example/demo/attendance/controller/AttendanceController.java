package com.example.demo.attendance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    @GetMapping("/status")
    public String getSystemStatus() {
        return "Core Attendance System Modules are active and synced with Supabase!";
    }
}