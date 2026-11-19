package com.group4.warehouse.staff;

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
 * HTTP endpoints for staff (STAFF + STAFF_PHONE).
 *
 *   GET    /api/staff?q=john&warehouseId=W02   list / search   200
 *   GET    /api/staff/{id}                     one person      200 | 404
 *   POST   /api/staff                          create          201 | 400 | 409
 *   PATCH  /api/staff/{id}                     partial update  200 | 400 | 404 | 409
 *   DELETE /api/staff/{id}                     delete          204 | 404 | 409 (has tasks, movements
 *                                                                              or supervises others)
 */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public List<Staff> list(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String warehouseId) {
        return service.list(q, warehouseId);
    }

    @GetMapping("/{id}")
    public Staff get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Staff> create(@Valid @RequestBody CreateStaffRequest request) {
        Staff created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/staff/{id}")
                .buildAndExpand(created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public Staff update(@PathVariable String id, @Valid @RequestBody UpdateStaffRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
