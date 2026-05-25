package com.example.demo.attendance.controller;

import com.example.demo.attendance.AttendanceService; // 2. IMPORT the service from the parent folder
import com.example.demo.attendance.AttendanceLog;     // 3. IMPORT the entity from the parent folder
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in")
    public AttendanceLog workerClockIn(@RequestParam Long workerId, @RequestParam Long siteId) {
        return attendanceService.clockIn(workerId, siteId);
    }

    @PostMapping("/clock-out/{logId}")
    public AttendanceLog workerClockOut(@PathVariable Long logId) {
        return attendanceService.clockOut(logId);
    }
    @GetMapping("/payroll/{workerId}")
    public java.util.Map<String, Object> getWorkerPayroll(@PathVariable Long workerId) {
        return attendanceService.calculatePendingPayroll(workerId);
    }
    @PutMapping("/settle/{workerId}")
    public String settlePayroll(@PathVariable Long workerId) {
        attendanceService.settleWorkerOvertime(workerId);
        return "All pending overtime entries for worker ID " + workerId + " have been successfully settled.";
    }
    @GetMapping("/reports/flagged")
    public List<AttendanceLog> getFlaggedReports() {
        return attendanceService.getFlaggedShifts();
    }
}