package com.example.demo.attendance;

import java.math.BigDecimal;

public class OvertimeSettledEvent {
    private final Long workerId;
    private final String month;
    private final BigDecimal totalAmount;

    public OvertimeSettledEvent(Long workerId, String month, BigDecimal totalAmount) {
        this.workerId = workerId;
        this.month = month;
        this.totalAmount = totalAmount;
    }

    public Long getWorkerId() { return workerId; }
    public String getMonth() { return month; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}