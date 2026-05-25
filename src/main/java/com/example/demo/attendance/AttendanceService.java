package com.example.demo.attendance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;

    // Constructor Injection matching Amigoscode style
    public AttendanceService(AttendanceLogRepository attendanceLogRepository,
                             WorkerRepository workerRepository,
                             SiteRepository siteRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.workerRepository = workerRepository;
        this.siteRepository = siteRepository;
    }

    /**
     * Clocks a worker into a specific construction site.
     */
    @Transactional
    public AttendanceLog clockIn(Long workerId, Long siteNameId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalStateException("Worker with ID " + workerId + " does not exist"));

        Site site = siteRepository.findById(siteNameId)
                .orElseThrow(() -> new IllegalStateException("Site with ID " + siteNameId + " does not exist"));

        // Business Rule: Check if worker is already clocked in somewhere without a clock-out timestamp
        List<AttendanceLog> activeLogs = attendanceLogRepository.findByWorkerId(workerId);
        boolean alreadyClockedIn = activeLogs.stream()
                .anyMatch(log -> log.getClockOutTimestamp() == null);

        if (alreadyClockedIn) {
            throw new IllegalStateException("Worker is already actively clocked into a site!");
        }

        AttendanceLog log = new AttendanceLog();
        log.setWorker(worker);
        log.setSite(site);
        log.setClockInTimestamp(LocalDateTime.now());
        log.setFlagged(false);

        return attendanceLogRepository.save(log);
    }

    /**
     * Clocks a worker out and calculates regular vs overtime hours.
     */
    @Transactional
    public AttendanceLog clockOut(Long logId) {
        AttendanceLog log = attendanceLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalStateException("Attendance log record not found"));

        if (log.getClockOutTimestamp() != null) {
            throw new IllegalStateException("Worker has already clocked out of this shift");
        }

        LocalDateTime clockOutTime = LocalDateTime.now();
        log.setClockOutTimestamp(clockOutTime);

        // Calculate time delta
        Duration duration = Duration.between(log.getClockInTimestamp(), clockOutTime);
        double totalHoursCalculated = duration.toMinutes() / 60.0;
        log.setTotalHours(totalHoursCalculated);

        // Business Rule: Shifts over 24 hours are highly irregular for construction safety logs -> Flag them
        if (totalHoursCalculated > 24.0) {
            log.setFlagged(true);
        }

        // Business Rule: Standard shift is 8 hours. Anything above is automated overtime hours.
        if (totalHoursCalculated > 8.0) {
            double otHours = totalHoursCalculated - 8.0;
            log.setOvertimeHours(otHours);
        } else {
            log.setOvertimeHours(0.0);
        }

        return attendanceLogRepository.save(log);
    }
}