package com.example.demo.attendance.controller;

import com.example.demo.attendance.AttendanceService;
import com.example.demo.attendance.AttendanceLog;
import com.example.demo.attendance.AttendanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/attendance/clock-in")
    public ResponseEntity<AttendanceLog> workerClockIn(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.clockIn(request.getWorkerId(), request.getSiteId()));
    }

    @PostMapping("/attendance/clock-out")
    public ResponseEntity<AttendanceLog> workerClockOut(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.clockOut(request.getWorkerId()));
    }

    @GetMapping("/attendance/active")
    public ResponseEntity<List<Object>> getActiveWorkers() {
        return ResponseEntity.ok(attendanceService.getAllActiveWorkers());
    }

    @GetMapping("/attendance/log")
    public ResponseEntity<Page<AttendanceLog>> getWorkerHistoryLogs(
            @RequestParam Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getWorkerHistory(workerId, from, to, page, size));
    }

    @PostMapping("/overtime/settle/{workerId}")
    public ResponseEntity<Map<String, Object>> settleMonthlyOvertimeRoute(
            @PathVariable Long workerId,
            @RequestParam String month) {
        BigDecimal totalPayout = attendanceService.settleMonthlyOvertime(workerId, month);
        return ResponseEntity.ok(Map.of(
                "status", "SETTLED",
                "workerId", workerId,
                "monthProcessed", month,
                "totalPayoutSettledAmount", totalPayout
        ));
    }
}