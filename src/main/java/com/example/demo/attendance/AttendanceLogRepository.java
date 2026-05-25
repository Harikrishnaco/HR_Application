package com.example.demo.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    /**
     * Finds all attendance logs where the worker has not clocked out yet.
     */
    @Query("SELECT a FROM AttendanceLog a WHERE a.clockOutTimestamp IS NULL")
    List<AttendanceLog> findActiveSessions();

    /**
     * Checks if a specific worker is currently clocked in without a clock-out record.
     */
    @Query("SELECT a FROM AttendanceLog a WHERE a.worker.id = :workerId AND a.clockOutTimestamp IS NULL")
    Optional<AttendanceLog> findByWorkerIdAndClockOutTimestampIsNull(@Param("workerId") Long workerId);

    /**
     * Calculates the total sum of overtime hours a worker has accumulated since a given start date.
     */
    @Query("SELECT COALESCE(SUM(a.overtimeHours), 0.0) FROM AttendanceLog a " +
            "WHERE a.worker.id = :workerId AND a.clockInTimestamp >= :startDate")
    Double getMonthlyTotalOvertimeHours(
            @Param("workerId") Long workerId,
            @Param("startDate") LocalDateTime startDate
    );

    /**
     * Fetches a paginated list of attendance logs for a specific worker within a date range.
     */
    @Query("SELECT a FROM AttendanceLog a WHERE a.worker.id = :workerId " +
            "AND a.clockInTimestamp >= :start AND a.clockInTimestamp <= :end")
    Page<AttendanceLog> findWorkerLogsInPeriod(
            @Param("workerId") Long workerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    /**
     * Fetches the complete historical attendance log records for a single worker.
     */
    @Query("SELECT a FROM AttendanceLog a WHERE a.worker.id = :workerId")
    List<AttendanceLog> findByWorkerId(@Param("workerId") Long workerId);

    /**
     * 🚀 FIXED LINE 254:39
     * Fetches all attendance logs based on their flagged status.
     */
    @Query("SELECT a FROM AttendanceLog a WHERE a.isFlagged = :isFlagged")
    List<AttendanceLog> findByIsFlagged(@Param("isFlagged") boolean isFlagged);

    /**
     * 🛡️ BONUS PRE-EMPTIVE FIX
     * Fetches all attendance logs for a specific job site.
     */
    @Query("SELECT a FROM AttendanceLog a WHERE a.site.id = :siteId")
    List<AttendanceLog> findBySiteId(@Param("siteId") Long siteId);
}