package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.analytics.CreateScheduleRequest;
import com.ecommerse.backend.dto.analytics.GenerateReportRequest;
import com.ecommerse.backend.dto.analytics.ReportResponseDTO;
import com.ecommerse.backend.dto.analytics.UpdateScheduleRequest;
import com.ecommerse.backend.entities.analytics.ReportSchedule;
import com.ecommerse.backend.services.analytics.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for report generation and schedule management.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('OWNER')")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ReportResponseDTO> generateReport(@RequestBody GenerateReportRequest request) {
        ReportResponseDTO report = reportService.generateReport(request);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String reportType,
            @RequestParam String format,
            @RequestParam String dateRange) {
        byte[] fileContent = reportService.exportReport(reportType, format, dateRange);

        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = format.equalsIgnoreCase("CSV")
                ? MediaType.parseMediaType("text/csv")
                : MediaType.APPLICATION_OCTET_STREAM;
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("report-" + reportType + "." + format.toLowerCase())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }

    @PostMapping("/schedules")
    public ResponseEntity<ReportSchedule> createSchedule(@RequestBody CreateScheduleRequest request) {
        ReportSchedule schedule = reportService.createSchedule(request);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<ReportSchedule>> getSchedules() {
        List<ReportSchedule> schedules = reportService.getAllSchedules();
        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<ReportSchedule> updateSchedule(
            @PathVariable Long id,
            @RequestBody UpdateScheduleRequest request) {
        ReportSchedule schedule = reportService.updateSchedule(id, request);
        return ResponseEntity.ok(schedule);
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        reportService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
