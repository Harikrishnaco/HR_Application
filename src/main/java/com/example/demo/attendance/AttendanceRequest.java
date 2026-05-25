package com.example.demo.attendance;

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
}