package com.group4.warehouse.report;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Dates;
import com.group4.warehouse.common.Text;

@Service
public class ReportService {
    private final ReportRepository repository;

    public ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> productLocations(String productId) {
        String id = Text.trimToNull(productId);
        if (id == null) {
            throw ApiException.badRequest("Choose a product: productId is required.");
        }
        if (!repository.productExists(id)) {
            throw ApiException.notFound("Product " + id + " was not found.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", id);
        result.put("items", repository.productLocations(id));
        return result;
    }

    public Map<String, Object> lowStock() {
        return Map.of("items", repository.lowStock());
    }

    public Map<String, Object> overstock() {
        return Map.of("items", repository.overstock());
    }

    /** asOf is echoed back as YYYY-MM-DD: contracts.js checks it equals the date it asked for. */
    public Map<String, Object> slowMoving(String asOf) {
        LocalDate day = Dates.asOf(asOf);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("asOf", day.toString());
        result.put("items", repository.slowMoving(day));
        return result;
    }

    public Map<String, Object> topCustomers() {
        return Map.of("items", repository.topCustomers());
    }

    public Map<String, Object> movements(String from, String to, String productId, String asOf, String timezone) {
        ZoneId zone = Dates.zone(timezone);
        LocalDate day = Dates.asOf(asOf);
        LocalDateTime end = Dates.to(to, zone, day.plusDays(1).atStartOfDay());
        LocalDateTime start = Dates.from(from, zone, day.minusMonths(1).atStartOfDay());
        checkRange(start, end);
        String id = Text.trimToNull(productId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", Dates.iso(start, zone));
        result.put("to", Dates.iso(end, zone));
        result.put("productId", id);
        result.put("items", repository.movements(id, start, end));
        return result;
    }

    public Map<String, Object> emptyBins(String from, String to, String asOf, String timezone) {
        ZoneId zone = Dates.zone(timezone);
        LocalDate weekStart = Dates.asOf(asOf).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime end = Dates.to(to, zone, weekStart.atStartOfDay());
        LocalDateTime start = Dates.from(from, zone, weekStart.minusWeeks(1).atStartOfDay());
        checkRange(start, end);
        List<Map<String, Object>> items = repository.emptyBins(start, end);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", Dates.iso(start, zone));
        result.put("to", Dates.iso(end, zone));
        result.put("timezone", zone.getId());
        result.put("items", items);
        return result;
    }

    public Map<String, Object> latestCountDifferences(String asOf) {
        LocalDate day = Dates.asOf(asOf);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("asOf", day.toString());
        result.put("count", repository.latestCountDifferences(day.plusDays(1).atStartOfDay()).orElse(null));
        return result;
    }

    private static void checkRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw ApiException.badRequest("The start of the period must be before its end.");
        }
    }
}
