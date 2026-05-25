package com.example.demo.attendance.controller;

import com.example.demo.attendance.AttendanceService;
import com.example.demo.attendance.AttendanceLog;
import com.example.demo.attendance.AttendanceRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/attendance") // Updated path to match the assignment specification exactly
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // 1. Refactored Clock-In using JSON RequestBody
    @PostMapping("/clock-in")
    public AttendanceLog workerClockIn(@RequestBody AttendanceRequest request) {
        return attendanceService.clockIn(request.getWorkerId(), request.getSiteId());
    }

    // 2. Refactored Clock-Out using JSON RequestBody (Automated shift matching)
    @PostMapping("/clock-out")
    public AttendanceLog workerClockOut(@RequestBody AttendanceRequest request) {
        return attendanceService.clockOut(request.getWorkerId());
    }

    // 3. Payroll Calculation Endpoint
    @GetMapping("/payroll/{workerId}")
    public Map<String, Object> getWorkerPayroll(@PathVariable Long workerId) {
        return attendanceService.calculatePendingPayroll(workerId);
    }

    // 4. Overtime Settlement Endpoint
    @PutMapping("/settle/{workerId}")
    public String settlePayroll(@PathVariable Long workerId) {
        attendanceService.settleWorkerOvertime(workerId);
        return "All pending overtime entries for worker ID " + workerId + " have been successfully settled.";
    }

    // 5. Flagged Anomalies Security Report
    @GetMapping("/reports/flagged")
    public List<AttendanceLog> getFlaggedReports() {
        return attendanceService.getFlaggedShifts();
    }
}