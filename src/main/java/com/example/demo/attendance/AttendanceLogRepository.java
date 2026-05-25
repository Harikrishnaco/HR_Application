package com.example.demo.attendance;

import com.example.demo.attendance.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {
    // Used to look up a worker's shifts
    List<AttendanceLog> findByWorkerId(Long workerId);
}