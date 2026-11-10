package com.group4.warehouse.zone;

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

@RestController
@RequestMapping("/api")
public class ZoneController {
    private final ZoneService service;

    public ZoneController(ZoneService service) {
        this.service = service;
    }

    @GetMapping("/zones")
    public List<Zone> listAll(@RequestParam(required = false) String warehouseId,
                              @RequestParam(required = false) String q) {
        return service.listAll(warehouseId, q);
    }

    @GetMapping("/warehouses/{warehouseId}/zones")
    public List<Zone> list(@PathVariable String warehouseId, @RequestParam(required = false) String q) {
        return service.list(warehouseId, q);
    }

    @GetMapping("/warehouses/{warehouseId}/zones/{zoneId}")
    public Zone get(@PathVariable String warehouseId, @PathVariable String zoneId) {
        return service.get(warehouseId, zoneId);
    }

    @PostMapping("/warehouses/{warehouseId}/zones")
    public ResponseEntity<Zone> create(@PathVariable String warehouseId,
                                       @Valid @RequestBody CreateZoneRequest request) {
        Zone created = service.create(warehouseId, request);
        URI location = UriComponentsBuilder.fromPath("/api/warehouses/{warehouseId}/zones/{zoneId}")
                .buildAndExpand(created.warehouseId(), created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/warehouses/{warehouseId}/zones/{zoneId}")
    public Zone update(@PathVariable String warehouseId, @PathVariable String zoneId,
                       @Valid @RequestBody UpdateZoneRequest request) {
        return service.update(warehouseId, zoneId, request);
    }

    @DeleteMapping("/warehouses/{warehouseId}/zones/{zoneId}")
    public ResponseEntity<Void> delete(@PathVariable String warehouseId, @PathVariable String zoneId) {
        service.delete(warehouseId, zoneId);
        return ResponseEntity.noContent().build();
    }
}
