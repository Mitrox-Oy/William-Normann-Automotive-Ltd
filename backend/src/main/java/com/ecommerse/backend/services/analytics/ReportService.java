package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.dto.analytics.CreateScheduleRequest;
import com.ecommerse.backend.dto.analytics.GenerateReportRequest;
import com.ecommerse.backend.dto.analytics.ReportResponseDTO;
import com.ecommerse.backend.dto.analytics.UpdateScheduleRequest;
import com.ecommerse.backend.entities.analytics.ReportSchedule;
import com.ecommerse.backend.repositories.analytics.ReportScheduleRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles ad-hoc and scheduled reporting functionality for analytics.
 */
@Service
public class ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportService.class);

    private final ReportScheduleRepository reportScheduleRepository;

    public ReportService(ReportScheduleRepository reportScheduleRepository) {
        this.reportScheduleRepository = reportScheduleRepository;
    }

    public ReportResponseDTO generateReport(GenerateReportRequest request) {
        // Placeholder implementation until full reporting engine is implemented
        String reportId = UUID.randomUUID().toString();
        String message = String.format("Report %s generation started for range %s", request.getType(), request.getDateRange());
        LOGGER.info(message);
        return new ReportResponseDTO(reportId, "QUEUED", null, message);
    }

    public byte[] exportReport(String reportType, String format, String dateRange) {
        // Provide simple CSV placeholder content for now
        String csv = "metric,value\n" +
                "report_type," + reportType + "\n" +
                "date_range," + dateRange + "\n" +
                "generated_at," + LocalDateTime.now() + "\n";
        LOGGER.info("Exporting {} report in {} format for range {}", reportType, format, dateRange);
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ReportSchedule createSchedule(CreateScheduleRequest request) {
        ReportSchedule schedule = new ReportSchedule();
        schedule.setReportName(request.getReportName());
        schedule.setReportType(request.getReportType());
        schedule.setScheduleExpression(request.getScheduleExpression());
        schedule.setRecipients(request.getRecipients());
        schedule.setFormat(request.getFormat());
        schedule.setFilterJson(request.getFilterJson());
        schedule.setNextRunAt(LocalDateTime.now());
        LOGGER.info("Creating report schedule '{}'", request.getReportName());
        return reportScheduleRepository.save(schedule);
    }

    public List<ReportSchedule> getAllSchedules() {
        return reportScheduleRepository.findAll();
    }

    @Transactional
    public ReportSchedule updateSchedule(Long id, UpdateScheduleRequest request) {
        ReportSchedule schedule = reportScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report schedule not found: " + id));

        Optional.ofNullable(request.getReportName()).ifPresent(schedule::setReportName);
        Optional.ofNullable(request.getReportType()).ifPresent(schedule::setReportType);
        Optional.ofNullable(request.getScheduleExpression()).ifPresent(schedule::setScheduleExpression);
        Optional.ofNullable(request.getRecipients()).ifPresent(schedule::setRecipients);
        Optional.ofNullable(request.getFormat()).ifPresent(schedule::setFormat);
        Optional.ofNullable(request.getFilterJson()).ifPresent(schedule::setFilterJson);
        if (request.getIsActive() != null) {
            schedule.setIsActive(request.getIsActive());
        }
        return reportScheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        reportScheduleRepository.deleteById(id);
    }
}
