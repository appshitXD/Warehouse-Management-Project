package com.group4.warehouse.aisle;

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
public class AisleController {
    private static final String BASE = "/warehouses/{warehouseId}/zones/{zoneId}/aisles";

    private final AisleService service;

    public AisleController(AisleService service) {
        this.service = service;
    }

    @GetMapping("/aisles")
    public List<Aisle> listAll(@RequestParam(required = false) String warehouseId,
                               @RequestParam(required = false) String zoneId,
                               @RequestParam(required = false) String q) {
        return service.listAll(warehouseId, zoneId, q);
    }

    @GetMapping(BASE)
    public List<Aisle> list(@PathVariable String warehouseId, @PathVariable String zoneId,
                            @RequestParam(required = false) String q) {
        return service.list(warehouseId, zoneId, q);
    }

    @GetMapping(BASE + "/{aisleId}")
    public Aisle get(@PathVariable String warehouseId, @PathVariable String zoneId,
                     @PathVariable String aisleId) {
        return service.get(warehouseId, zoneId, aisleId);
    }

    @PostMapping(BASE)
    public ResponseEntity<Aisle> create(@PathVariable String warehouseId, @PathVariable String zoneId,
                                        @Valid @RequestBody CreateAisleRequest request) {
        Aisle created = service.create(warehouseId, zoneId, request);
        URI location = UriComponentsBuilder.fromPath("/api" + BASE + "/{aisleId}")
                .buildAndExpand(created.warehouseId(), created.zoneId(), created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping(BASE + "/{aisleId}")
    public Aisle update(@PathVariable String warehouseId, @PathVariable String zoneId,
                        @PathVariable String aisleId, @Valid @RequestBody UpdateAisleRequest request) {
        return service.update(warehouseId, zoneId, aisleId, request);
    }

    @DeleteMapping(BASE + "/{aisleId}")
    public ResponseEntity<Void> delete(@PathVariable String warehouseId, @PathVariable String zoneId,
                                       @PathVariable String aisleId) {
        service.delete(warehouseId, zoneId, aisleId);
        return ResponseEntity.noContent().build();
    }
}
