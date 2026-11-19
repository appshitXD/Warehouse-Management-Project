package com.group4.warehouse.movement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * POST /api/movements   move stock between two bins (one transaction)
 *   201  the saved movement
 *   400  invalid field, wrong bin format, same source and destination
 *   409  product not in the source bin, not enough stock, unknown bin or staff
 * The list of movements comes with GET /api/bootstrap.
 */
@RestController
@RequestMapping("/api/movements")
public class StockMovementController {

    private final StockMovementService service;

    public StockMovementController(StockMovementService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StockMovement> create(@Valid @RequestBody CreateMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}
