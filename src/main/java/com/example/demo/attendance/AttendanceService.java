package com.example.demo.attendance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final OvertimeEntryRepository overtimeEntryRepository;

    public AttendanceService(AttendanceLogRepository attendanceLogRepository,
                             WorkerRepository workerRepository,
                             SiteRepository siteRepository,
                             OvertimeEntryRepository overtimeEntryRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.workerRepository = workerRepository;
        this.siteRepository = siteRepository;
        this.overtimeEntryRepository = overtimeEntryRepository;
    }

    public AttendanceLog clockIn(Long workerId, Long siteId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalStateException("Worker not found"));
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalStateException("Site not found"));

        attendanceLogRepository.findByWorkerIdAndClockOutTimestampIsNull(workerId).ifPresent(log -> {
            throw new IllegalStateException("Worker is already clocked into an active shift.");
        });

        AttendanceLog log = new AttendanceLog(worker, site, java.time.LocalDateTime.now());
        return attendanceLogRepository.save(log);
    }

    @Transactional
    public AttendanceLog clockOut(Long workerId) {
        AttendanceLog log = attendanceLogRepository.findByWorkerIdAndClockOutTimestampIsNull(workerId)
                .orElseThrow(() -> new IllegalStateException("No active shift found for this worker."));

        log.setClockOutTimestamp(java.time.LocalDateTime.now());

        java.time.Duration duration = java.time.Duration.between(log.getClockInTimestamp(), log.getClockOutTimestamp());
        double totalHoursCalculated = duration.toMinutes() / 60.0;
        log.setTotalHours(totalHoursCalculated);

        if (totalHoursCalculated > 24.0) {
            log.setIsFlagged(true);
        }

        if (totalHoursCalculated > 8.0) {
            double otHours = totalHoursCalculated - 8.0;
            log.setOvertimeHours(otHours);

            BigDecimal dailyRate = log.getWorker().getDailyWageRate();
            BigDecimal hourlyBaseRate = dailyRate.divide(BigDecimal.valueOf(8.0), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal otRateApplied = hourlyBaseRate.multiply(BigDecimal.valueOf(1.5));
            BigDecimal otAmount = otRateApplied.multiply(BigDecimal.valueOf(otHours));

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

    public Map<String, Object> calculatePendingPayroll(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalStateException("Worker does not exist"));

        List<AttendanceLog> logs = attendanceLogRepository.findByWorkerId(workerId);
        double totalRegularHours = logs.stream()
                .filter(log -> log.getClockOutTimestamp() != null)
                .mapToDouble(log -> Math.min(log.getTotalHours(), 8.0))
                .sum();

        BigDecimal hourlyBaseRate = worker.getDailyWageRate().divide(BigDecimal.valueOf(8.0), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalBaseWages = hourlyBaseRate.multiply(BigDecimal.valueOf(totalRegularHours));

        List<OvertimeEntry> pendingOt = overtimeEntryRepository.findByWorkerIdAndSettlementStatus(workerId, SettlementStatus.PENDING);
        BigDecimal totalOvertimeWages = pendingOt.stream()
                .map(OvertimeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> payrollReport = new HashMap<>();
        payrollReport.put("workerName", worker.getName());
        payrollReport.put("regularHoursWorked", totalRegularHours);
        payrollReport.put("baseWagesEarned", totalBaseWages);
        payrollReport.put("pendingOvertimeWages", totalOvertimeWages);
        payrollReport.put("grossPayoutDue", totalBaseWages.add(totalOvertimeWages));

        return payrollReport;
    }

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

    public List<AttendanceLog> getFlaggedShifts() {
        return attendanceLogRepository.findByIsFlagged(true);
    }
}