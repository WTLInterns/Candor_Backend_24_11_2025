package com.fieldforcepro.repository;

import com.fieldforcepro.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, String> {

    List<AttendanceRecord> findByAgentIdAndCheckInTimeBetween(String agentId, Instant from, Instant to);

    void deleteByAgentIdAndCheckInTimeBetween(String agentId, Instant from, Instant to);
}
