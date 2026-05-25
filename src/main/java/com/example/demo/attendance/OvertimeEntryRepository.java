package com.example.demo.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OvertimeEntryRepository extends JpaRepository<OvertimeEntry, Long> {
    List<OvertimeEntry> findByWorkerIdAndSettlementStatus(Long workerId, SettlementStatus status);

    // Add this line to look up all pending entries for a worker to settle them bulk
    List<OvertimeEntry> findByWorkerId(Long workerId);
}