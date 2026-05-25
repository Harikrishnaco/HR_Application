package com.example.demo.attendance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Service
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final OvertimeEntryRepository overtimeEntryRepository; // Added

    // Update constructor to inject the new repository
    public AttendanceService(AttendanceLogRepository attendanceLogRepository,
                             WorkerRepository workerRepository,
                             SiteRepository siteRepository,
                             OvertimeEntryRepository overtimeEntryRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.workerRepository = workerRepository;
        this.siteRepository = siteRepository;
        this.overtimeEntryRepository = overtimeEntryRepository;
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
        // Business Rule: Standard shift is 8 hours. Anything above is automated overtime hours.
        if (totalHoursCalculated > 8.0) {
            double otHours = totalHoursCalculated - 8.0;
            log.setOvertimeHours(otHours);

            // --- AUTOMATED OVERTIME ENTRY GENERATION ---
            // 1. Calculate worker's base hourly rate (Daily Rate / 8 hours)
            BigDecimal dailyRate = log.getWorker().getDailyWageRate();
            BigDecimal hourlyBaseRate = dailyRate.divide(BigDecimal.valueOf(8.0), 2, java.math.RoundingMode.HALF_UP);

            // 2. Apply Overtime Premium Multiplier (e.g., 1.5x hourly rate for overtime)
            BigDecimal otRateApplied = hourlyBaseRate.multiply(BigDecimal.valueOf(1.5));

            // 3. Calculate total earnings for this overtime period (Hours * OT Rate)
            BigDecimal otAmount = otRateApplied.multiply(BigDecimal.valueOf(otHours));

            // 4. Construct and Save the Overtime record
            OvertimeEntry otEntry = new OvertimeEntry(
                    log.getWorker(),
                    log,
                    log.getClockInTimestamp().toLocalDate(),
                    otHours,
                    otRateApplied,
                    otAmount,
                    SettlementStatus.PENDING
            );

            overtimeEntryRepository.save(otEntry);
        } else {
            log.setOvertimeHours(0.0);
        }
        return attendanceLogRepository.save(log);
    }
    /**
     * Calculates total pending payout (Regular Base Pay + Pending Overtime Pay) for a worker.
     */
    public java.util.Map<String, Object> calculatePendingPayroll(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalStateException("Worker does not exist"));

        // 1. Calculate Base Regular Wages from completed shifts (Up to 8 hours per shift)
        List<AttendanceLog> logs = attendanceLogRepository.findByWorkerId(workerId);
        double totalRegularHours = logs.stream()
                .filter(log -> log.getClockOutTimestamp() != null)
                .mapToDouble(log -> Math.min(log.getTotalHours(), 8.0))
                .sum();

        BigDecimal hourlyBaseRate = worker.getDailyWageRate().divide(BigDecimal.valueOf(8.0), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalBaseWages = hourlyBaseRate.multiply(BigDecimal.valueOf(totalRegularHours));

        // 2. Calculate Unpaid Overtime Wages
        List<OvertimeEntry> pendingOt = overtimeEntryRepository.findByWorkerIdAndSettlementStatus(workerId, SettlementStatus.PENDING);
        BigDecimal totalOvertimeWages = pendingOt.stream()
                .map(OvertimeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Compile Summary Response Map
        java.util.Map<String, Object> payrollReport = new java.util.HashMap<>();
        payrollReport.put("workerName", worker.getName());
        payrollReport.put("regularHoursWorked", totalRegularHours);
        payrollReport.put("baseWagesEarned", totalBaseWages);
        payrollReport.put("pendingOvertimeWages", totalOvertimeWages);
        payrollReport.put("grossPayoutDue", totalBaseWages.add(totalOvertimeWages));

        return payrollReport;
    }
    /**
     * Updates all PENDING overtime entries for a specific worker to SETTLED.
     */
    @Transactional
    public void settleWorkerOvertime(Long workerId) {
        List<OvertimeEntry> pendingEntries = overtimeEntryRepository.findByWorkerIdAndSettlementStatus(workerId, SettlementStatus.PENDING);

        if (pendingEntries.isEmpty()) {
            throw new IllegalStateException("No pending overtime entries found for this worker.");
        }

        for (OvertimeEntry entry : pendingEntries) {
            entry.setSettlementStatus(SettlementStatus.SETTLED);
        }

        overtimeEntryRepository.saveAll(pendingEntries);
    }
    /**
     * Fetches all attendance logs flagged for irregular activity (e.g., > 24 hours).
     */
    public List<AttendanceLog> getFlaggedShifts() {
        return attendanceLogRepository.findByIsFlagged(true);
    }
}