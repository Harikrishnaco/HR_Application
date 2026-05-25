package com.example.demo.attendance;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "overtime_entries")
public class OvertimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attendance_log_id", nullable = false)
    private AttendanceLog attendanceLog;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "overtime_hours", nullable = false)
    private Double overtimeHours;

    @Column(name = "overtime_rate_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal overtimeRateApplied;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false)
    private SettlementStatus settlementStatus;

    // --- CONSTRUCTORS ---
    public OvertimeEntry() {}

    public OvertimeEntry(Worker worker, AttendanceLog attendanceLog, LocalDate date,
                         Double overtimeHours, BigDecimal overtimeRateApplied, BigDecimal amount,
                         SettlementStatus settlementStatus) {
        this.worker = worker;
        this.attendanceLog = attendanceLog;
        this.date = date;
        this.overtimeHours = overtimeHours;
        this.overtimeRateApplied = overtimeRateApplied;
        this.amount = amount;
        this.settlementStatus = settlementStatus;
    }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }

    public AttendanceLog getAttendanceLog() { return attendanceLog; }
    public void setAttendanceLog(AttendanceLog attendanceLog) { this.attendanceLog = attendanceLog; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getOvertimeRateApplied() { return overtimeRateApplied; }
    public void setOvertimeRateApplied(BigDecimal overtimeRateApplied) { this.overtimeRateApplied = overtimeRateApplied; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public SettlementStatus getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(SettlementStatus settlementStatus) { this.settlementStatus = settlementStatus; }
}