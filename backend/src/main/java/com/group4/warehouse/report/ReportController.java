package com.group4.warehouse.report;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/product-locations")
    public Map<String, Object> productLocations(@RequestParam(required = false) String productId) {
        return service.productLocations(productId);
    }

    @GetMapping("/low-stock")
    public Map<String, Object> lowStock() {
        return service.lowStock();
    }

    @GetMapping("/overstock")
    public Map<String, Object> overstock() {
        return service.overstock();
    }

    @GetMapping("/slow-moving")
    public Map<String, Object> slowMoving(@RequestParam(required = false) String asOf) {
        return service.slowMoving(asOf);
    }

    @GetMapping("/top-customers")
    public Map<String, Object> topCustomers() {
        return service.topCustomers();
    }

    @GetMapping("/movements")
    public Map<String, Object> movements(@RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false) String productId,
                                         @RequestParam(required = false) String asOf,
                                         @RequestParam(required = false) String timezone) {
        return service.movements(from, to, productId, asOf, timezone);
    }

    @GetMapping("/empty-bins")
    public Map<String, Object> emptyBins(@RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false) String asOf,
                                         @RequestParam(required = false) String timezone) {
        return service.emptyBins(from, to, asOf, timezone);
    }

    @GetMapping("/latest-count-differences")
    public Map<String, Object> latestCountDifferences(@RequestParam(required = false) String asOf) {
        return service.latestCountDifferences(asOf);
    }
}
