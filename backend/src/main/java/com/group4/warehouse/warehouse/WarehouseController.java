package com.group4.warehouse.warehouse;

import java.net.URI;
import java.util.List;

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
 * HTTP endpoints for warehouses.
 *
 *   GET    /api/warehouses?q=thu   list / search      200
 *   GET    /api/warehouses/{id}    one warehouse      200 | 404
 *   POST   /api/warehouses         create             201 | 400 | 409
 *   PATCH  /api/warehouses/{id}    partial update     200 | 400 | 404 | 409
 *   DELETE /api/warehouses/{id}    delete             204 | 404 | 409 (still has zones)
 */
@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping
    public List<Warehouse> list(@RequestParam(required = false) String q) {
        return service.list(q);
    }

    @GetMapping("/{id}")
    public Warehouse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Warehouse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        Warehouse created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/warehouses/{id}")
                .buildAndExpand(created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public Warehouse update(@PathVariable String id, @Valid @RequestBody UpdateWarehouseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
