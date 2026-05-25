package com.example.demo.attendance;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_logs", indexes = {
        @Index(name = "idx_worker_date", columnList = "worker_id, clock_in_timestamp")
})
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "clock_in_timestamp", nullable = false)
    private LocalDateTime clockInTimestamp;

    @Column(name = "clock_out_timestamp")
    private LocalDateTime clockOutTimestamp;

    @Column(name = "total_hours")
    private Double totalHours;

    @Column(name = "overtime_hours")
    private Double overtimeHours;

    @Column(name = "is_flagged", nullable = false, columnDefinition = "boolean default false")
    private Boolean isFlagged = false;

    // --- CONSTRUCTORS ---
    public AttendanceLog() {}

    public AttendanceLog(Worker worker, Site site, LocalDateTime clockInTimestamp) {
        this.worker = worker;
        this.site = site;
        this.clockInTimestamp = clockInTimestamp;
        this.isFlagged = false;
    }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public LocalDateTime getClockInTimestamp() { return clockInTimestamp; }
    public void setClockInTimestamp(LocalDateTime clockInTimestamp) { this.clockInTimestamp = clockInTimestamp; }

    public LocalDateTime getClockOutTimestamp() { return clockOutTimestamp; }
    public void setClockOutTimestamp(LocalDateTime clockOutTimestamp) { this.clockOutTimestamp = clockOutTimestamp; }

    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }

    public Double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }

    public Boolean getIsFlagged() { return isFlagged; }
    public void setIsFlagged(Boolean flagged) { isFlagged = flagged; }
}