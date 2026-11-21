package com.group4.warehouse.order;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;

/**
 * Customer orders.
 *
 *   GET    /api/orders?q=CO01               list / search (ID or customer name)   200
 *   GET    /api/orders/awaiting-issue       orders with no goods issue yet         200 { "items": [...] }
 *   GET    /api/orders/{id}                 one order                              200 | 404
 *   POST   /api/orders                      create                                 201 | 400 | 409
 *   PATCH  /api/orders/{id}                 change time / status / customer        200 | 400 | 404 | 409
 *   DELETE /api/orders/{id}                 delete                                 204 | 404 | 409 (has a goods issue)
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repository;
    private final OrderService service;

    public OrderController(OrderRepository repository, OrderService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    public List<CustomerOrder> list(@RequestParam(required = false) String q) {
        return service.list(q);
    }

    @GetMapping("/awaiting-issue")
    public Map<String, List<WaitingOrder>> awaitingIssue() {
        return Map.of("items", repository.findAwaitingIssue());
    }

    @GetMapping("/{id}")
    public CustomerOrder get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<CustomerOrder> create(@Valid @RequestBody CreateOrderRequest request) {
        CustomerOrder created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/orders/{id}")
                .buildAndExpand(created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public CustomerOrder update(@PathVariable String id, @Valid @RequestBody UpdateOrderRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
