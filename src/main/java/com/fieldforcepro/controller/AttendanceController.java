package com.fieldforcepro.controller;

import com.fieldforcepro.model.AttendanceRecord;
import com.fieldforcepro.repository.AttendanceRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@Tag(name = "Attendance")
public class AttendanceController {

    private final AttendanceRecordRepository attendanceRecordRepository;

    public AttendanceController(AttendanceRecordRepository attendanceRecordRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    public record AttendanceEntry(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            String status
    ) { }

    public record MarkAttendanceRequest(
            String employeeId,
            String employeeName,
            List<AttendanceEntry> entries
    ) { }

    public record AttendanceDto(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            String status
    ) { }

    @PostMapping("/mark")
    @Operation(summary = "Mark attendance for an employee for one or more dates")
    @Transactional
    public ResponseEntity<Void> markAttendance(@RequestBody MarkAttendanceRequest request) {
        if (request.employeeId() == null || request.employeeId().isBlank()
                || request.employeeName() == null || request.employeeName().isBlank()
                || request.entries() == null || request.entries().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ZoneId zoneId = ZoneId.systemDefault();
        List<AttendanceRecord> toSave = new ArrayList<>();

        for (AttendanceEntry entry : request.entries()) {
            if (entry.date() == null || entry.status() == null || entry.status().isBlank()) {
                continue;
            }

            LocalDate d = entry.date();
            Instant fromInstant = d.atStartOfDay(zoneId).toInstant();
            Instant toInstant = d.plusDays(1).atStartOfDay(zoneId).toInstant();

            // Upsert behavior for this exact date: update existing record if present, otherwise create new
            List<AttendanceRecord> existingForDay = attendanceRecordRepository
                    .findByAgentIdAndCheckInTimeBetween(request.employeeId(), fromInstant, toInstant);

            AttendanceRecord record;
            if (!existingForDay.isEmpty()) {
                record = existingForDay.get(0);
                record.setStatus(entry.status());
                record.setAgentName(request.employeeName());
            } else {
                LocalDateTime ldt = d.atStartOfDay();
                Instant checkIn = ldt.atZone(zoneId).toInstant();

                record = AttendanceRecord.builder()
                        .agentId(request.employeeId())
                        .agentName(request.employeeName())
                        .checkInTime(checkIn)
                        .status(entry.status())
                        .build();
            }

            toSave.add(record);
        }

        if (!toSave.isEmpty()) {
            attendanceRecordRepository.saveAll(toSave);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get attendance for an employee between two dates")
    public ResponseEntity<List<AttendanceDto>> getAttendance(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("employeeName") String employeeName,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        if (employeeId == null || employeeId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ZoneId zoneId = ZoneId.systemDefault();
        Instant fromInstant = fromDate.atStartOfDay(zoneId).toInstant();
        Instant toInstant = toDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        List<AttendanceRecord> records = attendanceRecordRepository
                .findByAgentIdAndCheckInTimeBetween(employeeId, fromInstant, toInstant);

        List<AttendanceDto> result = new ArrayList<>();
        for (AttendanceRecord r : records) {
            LocalDate date = LocalDateTime.ofInstant(r.getCheckInTime(), zoneId).toLocalDate();
            result.add(new AttendanceDto(date, r.getStatus()));
        }

        return ResponseEntity.ok(result);
    }
}
