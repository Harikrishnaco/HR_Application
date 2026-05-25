package com.example.demo.attendance;

import org.springframework.web.bind.annotation.*;

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
}