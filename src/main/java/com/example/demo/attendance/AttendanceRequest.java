package com.example.demo.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

public class AttendanceRequest {
    private Long workerId;
    private Long siteId; // Will be null during clock-out

    // Constructors
    public AttendanceRequest() {}

    public AttendanceRequest(Long workerId, Long siteId) {
        this.workerId = workerId;
        this.siteId = siteId;
    }

    // Getters and Setters
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }

    @Repository
    public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

        // Add this exact line so Spring knows how to auto-generate the query
        List<AttendanceLog> findByClockOutTimestampIsNull();
    }
}