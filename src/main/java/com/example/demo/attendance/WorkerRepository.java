package com.example.demo.attendance;

import com.example.demo.attendance.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    // We will add custom queries here later to find active workers
}