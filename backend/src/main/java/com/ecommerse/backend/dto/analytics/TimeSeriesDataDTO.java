package com.ecommerse.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Simple time series point for chart visualisations.
 */
public class TimeSeriesDataDTO {

    private LocalDate date;
    private BigDecimal value;

    public TimeSeriesDataDTO() {
    }

    public TimeSeriesDataDTO(LocalDate date, BigDecimal value) {
        this.date = date;
        this.value = value;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
