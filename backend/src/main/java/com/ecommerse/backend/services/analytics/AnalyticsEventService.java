package com.ecommerse.backend.services.analytics;

import com.ecommerse.backend.entities.analytics.AnalyticsEvent;
import com.ecommerse.backend.repositories.analytics.AnalyticsEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsEventService {

    private final AnalyticsEventRepository repository;

    public AnalyticsEventService(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    public AnalyticsEvent ingest(AnalyticsEvent event) {
        if (event.getType() == null || event.getType().length() > 64) {
            throw new IllegalArgumentException("Invalid event type");
        }
        if (event.getSessionId() != null && event.getSessionId().length() > 128) {
            throw new IllegalArgumentException("Invalid sessionId");
        }
        if (event.getPath() != null && event.getPath().length() > 512) {
            throw new IllegalArgumentException("Invalid path");
        }
        return repository.save(event);
    }

    public Map<String, Object> getSummary(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        Map<String, Object> result = new HashMap<>();

        // Totals by type
        Map<String, Long> totals = new HashMap<>();
        List<Object[]> rows = repository.countByTypeSince(from);
        for (Object[] row : rows) {
            String type = (String) row[0];
            Long cnt = (Long) row[1];
            totals.put(type, cnt);
        }
        result.put("totals", totals);

        // Simple 24h series (hourly) for the last day
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        List<AnalyticsEvent> recent = repository.findByOccurredAtBetween(last24h, LocalDateTime.now());
        Map<String, long[]> series = new HashMap<>();
        for (AnalyticsEvent e : recent) {
            String type = e.getType();
            series.computeIfAbsent(type, k -> new long[24]);
            int hourIndex = Math.max(0, Math.min(23, e.getOccurredAt().getHour()));
            series.get(type)[hourIndex]++;
        }
        result.put("series24h", series);

        return result;
    }
}




