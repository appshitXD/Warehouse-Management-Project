package com.group4.warehouse.supplier;

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
 * HTTP endpoints for suppliers.
 *
 *   GET    /api/suppliers?q=food   list / search      200
 *   GET    /api/suppliers/{id}     one supplier       200 | 404
 *   POST   /api/suppliers          create             201 | 400 | 409
 *   PATCH  /api/suppliers/{id}     partial update     200 | 400 | 404
 *   DELETE /api/suppliers/{id}     delete             204 | 404 | 409 (still has goods receipts)
 */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public List<Supplier> list(@RequestParam(required = false) String q) {
        return service.list(q);
    }

    @GetMapping("/{id}")
    public Supplier get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Supplier> create(@Valid @RequestBody CreateSupplierRequest request) {
        Supplier created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/suppliers/{id}")
                .buildAndExpand(created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public Supplier update(@PathVariable String id, @Valid @RequestBody UpdateSupplierRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
