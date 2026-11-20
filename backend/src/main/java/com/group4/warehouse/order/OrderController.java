package com.group4.warehouse.order;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *   GET /api/orders/awaiting-issue   orders waiting to ship   200  { "items": [...] }
 * Saving orders is not part of the brief, so there is no POST/PATCH/DELETE here.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repository;

    public OrderController(OrderRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/awaiting-issue")
    public Map<String, List<WaitingOrder>> awaitingIssue() {
        return Map.of("items", repository.findAwaitingIssue());
    }
}
