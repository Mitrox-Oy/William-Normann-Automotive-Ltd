package com.ecommerse.backend.dto.analytics;

/**
 * Response returned after an ad-hoc report generation request.
 */
public class ReportResponseDTO {

    private String reportId;
    private String status;
    private String downloadUrl;
    private String message;

    public ReportResponseDTO() {
    }

    public ReportResponseDTO(String reportId, String status, String downloadUrl, String message) {
        this.reportId = reportId;
        this.status = status;
        this.downloadUrl = downloadUrl;
        this.message = message;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
