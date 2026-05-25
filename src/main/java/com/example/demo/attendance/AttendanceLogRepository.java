package com.example.demo.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {
    List<AttendanceLog> findByWorkerId(Long workerId);

    // Add this to find suspicious shifts
    List<AttendanceLog> findByIsFlagged(Boolean isFlagged);
}