package com.example.demo.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "clock_in_timestamp")
    private LocalDateTime clockInTimestamp;

    @Column(name = "clock_out_timestamp")
    private LocalDateTime clockOutTimestamp;

    @Column(name = "total_hours")
    private Double totalHours;

    @Column(name = "overtime_hours")
    private Double overtimeHours;

    @Column(name = "is_flagged", nullable = false, columnDefinition = "boolean default false")
    private Boolean isFlagged = false;
}