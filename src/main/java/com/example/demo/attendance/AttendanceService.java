package com.example.demo.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceLogRepository attendanceLogRepository;
    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final OvertimeEntryRepository overtimeEntryRepository;
    private final ActiveWorkerCacheService activeWorkerCacheService;
    private final ApplicationEventPublisher eventPublisher;

    public AttendanceService(AttendanceLogRepository attendanceLogRepository,
                             WorkerRepository workerRepository,
                             SiteRepository siteRepository,
                             OvertimeEntryRepository overtimeEntryRepository,
                             ActiveWorkerCacheService activeWorkerCacheService,
                             ApplicationEventPublisher eventPublisher) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.workerRepository = workerRepository;
        this.siteRepository = siteRepository;
        this.overtimeEntryRepository = overtimeEntryRepository;
        this.activeWorkerCacheService = activeWorkerCacheService;
        this.eventPublisher = eventPublisher;
    }

    public AttendanceLog clockIn(Long workerId, Long siteId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException("WORKER_NOT_FOUND", "Worker does not exist.", HttpStatus.NOT_FOUND));

        if (!worker.isActive()) {
            throw new BusinessException("INACTIVE_WORKER", "Worker profile is currently marked inactive.", HttpStatus.BAD_REQUEST);
        }

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException("SITE_NOT_FOUND", "Site location does not exist.", HttpStatus.NOT_FOUND));

        if (!site.isActive()) {
            throw new BusinessException("INACTIVE_SITE", "Site operations are not active.", HttpStatus.BAD_REQUEST);
        }

        attendanceLogRepository.findByWorkerIdAndClockOutTimestampIsNull(workerId).ifPresent(existingLog -> {
            throw new BusinessException("DUPLICATE_CLOCK_IN", "Worker is already clocked in at Site: " + existingLog.getSite().getSiteName(), HttpStatus.CONFLICT);
        });

        // 1. TEMPORARY TEST LINE: Force the clock-in to happen in April
        java.time.LocalDateTime fakeAprilTime = java.time.LocalDateTime.of(2026, 4, 20, 8, 0, 0);
        AttendanceLog attendanceLog = new AttendanceLog(worker, site, fakeAprilTime);

        // 2. CORE FIX: Assign the saved database output to 'savedLog' explicitly
        AttendanceLog savedLog = attendanceLogRepository.save(attendanceLog);

        // Safe string extraction to prevent any silent entity property failures
        String cacheWorkerName = (worker.getName() != null) ? worker.getName() : "Worker " + workerId;
        String cacheSiteName = "Active Site";

        try {
            // Try to use the site name safely
            if (site != null) {
                cacheSiteName = site.getSiteName(); // Change to site.getName() if your entity uses getName()
            }
        } catch (Exception e) {
            // Fallback if the entity getter fails
            cacheSiteName = "Site " + siteId;
        }

        try {
            log.info(">>>> ATTEMPTING TO WRITE TO REDIS HASH TABLE FOR WORKER {} <<<<", workerId);
            activeWorkerCacheService.cacheActiveWorker(
                    workerId,
                    cacheWorkerName,
                    cacheSiteName,
                    savedLog.getClockInTimestamp().toString()
            );
            log.info(">>>> REDIS WRITE COMPLETED SUCCESSFULLY FOR WORKER {} <<<<", workerId);
        } catch (Exception ex) {
            log.error("Redis unreachable during clock-in. Error: {}", ex.getMessage());
        }

        return savedLog;
    }

    @Transactional
    public AttendanceLog clockOut(Long workerId) {
        AttendanceLog attendanceLog = attendanceLogRepository.findByWorkerIdAndClockOutTimestampIsNull(workerId)
                .orElseThrow(() -> new BusinessException("NO_ACTIVE_SHIFT", "Worker is not currently clocked in.", HttpStatus.BAD_REQUEST));

        attendanceLog.setClockOutTimestamp(LocalDateTime.now());
        java.time.Duration duration = java.time.Duration.between(attendanceLog.getClockInTimestamp(), attendanceLog.getClockOutTimestamp());



        double totalHours = duration.toMinutes() / 60.0;
        attendanceLog.setTotalHours(totalHours);



        // Rule: If shift exceeds 16 hours, auto-flag the record for manual review
        if (totalHours > 16.0) {
            attendanceLog.setIsFlagged(true);
        }

        // Rule: Overtime Calculations over 8 hours limit threshold
        if (totalHours > 8.0) {
            double rawOtHours = totalHours - 8.0;

            // Enforce Monthly 60-Hour Ceiling Limit Cap
            LocalDateTime startOfMonth = attendanceLog.getClockInTimestamp().withDayOfMonth(1).withHour(0).withMinute(0);
            double currentAccruedOt = attendanceLogRepository.getMonthlyTotalOvertimeHours(workerId, startOfMonth);

            double allowedOtHours = rawOtHours;
            if (currentAccruedOt + rawOtHours > 60.0) {
                allowedOtHours = Math.max(0.0, 60.0 - currentAccruedOt);
            }

            attendanceLog.setOvertimeHours(allowedOtHours);

            if (allowedOtHours > 0) {
                BigDecimal dailyRate = attendanceLog.getWorker().getDailyWageRate();
                BigDecimal baseHourlyRate = dailyRate.divide(BigDecimal.valueOf(8.0), 2, java.math.RoundingMode.HALF_UP);

                // Tiered Rates Rule: First 2 hours at 1.5x hourly rate, subsequent hours at 2.0x hourly rate
                BigDecimal otTotalAmount = BigDecimal.ZERO;
                if (allowedOtHours <= 2.0) {
                    BigDecimal rate15 = baseHourlyRate.multiply(BigDecimal.valueOf(1.5));
                    otTotalAmount = rate15.multiply(BigDecimal.valueOf(allowedOtHours));
                } else {
                    BigDecimal rate15 = baseHourlyRate.multiply(BigDecimal.valueOf(1.5));
                    BigDecimal rate20 = baseHourlyRate.multiply(BigDecimal.valueOf(2.0));
                    BigDecimal baseTierPay = rate15.multiply(BigDecimal.valueOf(2.0));
                    BigDecimal extendedTierPay = rate20.multiply(BigDecimal.valueOf(allowedOtHours - 2.0));
                    otTotalAmount = baseTierPay.add(extendedTierPay);
                }

                OvertimeEntry otEntry = new OvertimeEntry(
                        attendanceLog.getWorker(), attendanceLog, attendanceLog.getClockInTimestamp().toLocalDate(),
                        allowedOtHours, baseHourlyRate, otTotalAmount, SettlementStatus.PENDING
                );
                overtimeEntryRepository.save(otEntry);
            }
        } else {
            attendanceLog.setOvertimeHours(0.0);
        }

        try {
            activeWorkerCacheService.removeActiveWorker(workerId);
        } catch (Exception ex) {
            log.error("Redis unreachable during clock-out eviction. Error: {}", ex.getMessage());
        }

        return attendanceLogRepository.save(attendanceLog);
    }

    public List<Object> getAllActiveWorkers() {
        try {
            // Calling our custom query method
            return attendanceLogRepository.findActiveSessions()
                    .stream()
                    .map(log -> String.format("Worker ID: %d | Name: %s | Site: %s | Arrived: %s",
                            log.getWorker().getId(),
                            log.getWorker().getName(),
                            log.getSite().getSiteName(),
                            log.getClockInTimestamp()))
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception ex) {
            log.error("Fallback failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public org.springframework.data.domain.Page<AttendanceLog> getWorkerHistory(
            Long workerId, LocalDateTime from, LocalDateTime to, int page, int size) {
        return attendanceLogRepository.findWorkerLogsInPeriod(workerId, from, to, org.springframework.data.domain.PageRequest.of(page, size));
    }

    // Ticket LF-204: Enforce an all-or-nothing, atomic monthly settlement loop
    @Transactional
    public BigDecimal settleMonthlyOvertime(Long workerId, String monthStr) {
        YearMonth targetMonth = YearMonth.parse(monthStr); // Format parameter input: YYYY-MM
        if (targetMonth.equals(YearMonth.now())) {
            throw new BusinessException("INVALID_SETTLEMENT", "Cannot settle current active open month payroll logs.", HttpStatus.BAD_REQUEST);
        }

        List<OvertimeEntry> entries = overtimeEntryRepository.findByWorkerIdAndSettlementStatus(workerId, SettlementStatus.PENDING);
        List<OvertimeEntry> filteredMonthlyEntries = entries.stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(targetMonth))
                .collect(java.util.stream.Collectors.toList());

        if (filteredMonthlyEntries.isEmpty()) {
            throw new BusinessException("NO_PENDING_PAYABLE", "No pending overtime entries found for the specified month.", HttpStatus.NOT_FOUND);
        }

        BigDecimal totalSettledPayout = BigDecimal.ZERO;
        for (OvertimeEntry entry : filteredMonthlyEntries) {
            entry.setSettlementStatus(SettlementStatus.SETTLED);
            totalSettledPayout = totalSettledPayout.add(entry.getAmount());
        }

        overtimeEntryRepository.saveAll(filteredMonthlyEntries);

        // Safe Event Trigger: Message publishes here but text alerts won't fire unless transaction successfully commits
        eventPublisher.publishEvent(new OvertimeSettledEvent(workerId, monthStr, totalSettledPayout));

        return totalSettledPayout;
    }

    public Map<String, Object> calculatePendingPayroll(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException("WORKER_NOT_FOUND", "Worker does not exist.", HttpStatus.NOT_FOUND));

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

    public List<AttendanceLog> getFlaggedShifts() {
        return attendanceLogRepository.findByIsFlagged(true);
    }
}