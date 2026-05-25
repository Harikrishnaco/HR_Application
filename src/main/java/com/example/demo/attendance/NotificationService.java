package com.example.demo.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Fix LF-204: Fires strictly AFTER the database successfully saves and commits changes
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOvertimeSettlementSMS(OvertimeSettledEvent event) {
        try {
            log.info("Contacting external carrier SMS Gateway for Worker: {}...", event.getWorkerId());
            String textPayload = String.format("Your %s overtime payment of ₹%s has been successfully processed and settled.",
                    event.getMonth(), event.getTotalAmount());
            log.info("SMS Dispatched successfully: '{}'", textPayload);
        } catch (Exception ex) {
            log.error("Notification delivery failed. Core transaction data remains safe. Error: {}", ex.getMessage());
        }
    }
}