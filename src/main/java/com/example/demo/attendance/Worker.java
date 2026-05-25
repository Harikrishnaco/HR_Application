package com.example.demo.attendance;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Designation designation;

    @Column(name = "daily_wage_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyWageRate;

    @Column(name = "active_status", nullable = false, columnDefinition = "boolean default true")
    private Boolean activeStatus = true;

    // --- CONSTRUCTORS ---
    public Worker() {}

    public Worker(Long id, String name, String phone, Designation designation, BigDecimal dailyWageRate, Boolean activeStatus) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.designation = designation;
        this.dailyWageRate = dailyWageRate;
        this.activeStatus = activeStatus;
    }

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }

    public BigDecimal getDailyWageRate() { return dailyWageRate; }
    public void setDailyWageRate(BigDecimal dailyWageRate) { this.dailyWageRate = dailyWageRate; }

    public Boolean getActiveStatus() { return activeStatus; }
    public void setActiveStatus(Boolean activeStatus) { this.activeStatus = activeStatus; }


    // 1. Add the field with a default of true
    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    // 2. Add the getter method (this is what AttendanceService is looking for)
    public boolean isActive() {
        return active;
    }

    // 3. Add the setter method
    public void setActive(boolean active) {
        this.active = active;
    }
}