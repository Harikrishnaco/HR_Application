package com.example.demo.attendance;

import com.example.demo.attendance.OvertimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OvertimeEntryRepository extends JpaRepository<OvertimeEntry, Long> {
}