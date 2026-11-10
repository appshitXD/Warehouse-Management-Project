package com.group4.warehouse.bin;

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
public class BinController {
    private static final String BASE = "/warehouses/{warehouseId}/zones/{zoneId}/aisles/{aisleId}/bins";

    private final BinService service;

    public BinController(BinService service) {
        this.service = service;
    }

    @GetMapping("/bins")
    public List<Bin> listAll(@RequestParam(required = false) String warehouseId,
                             @RequestParam(required = false) String zoneId,
                             @RequestParam(required = false) String aisleId,
                             @RequestParam(required = false) String q) {
        return service.listAll(warehouseId, zoneId, aisleId, q);
    }

    @GetMapping(BASE)
    public List<Bin> list(@PathVariable String warehouseId, @PathVariable String zoneId,
                          @PathVariable String aisleId, @RequestParam(required = false) String q) {
        return service.list(warehouseId, zoneId, aisleId, q);
    }

    @GetMapping(BASE + "/{position}")
    public Bin get(@PathVariable String warehouseId, @PathVariable String zoneId,
                   @PathVariable String aisleId, @PathVariable String position) {
        return service.get(warehouseId, zoneId, aisleId, position);
    }

    @PostMapping(BASE)
    public ResponseEntity<Bin> create(@PathVariable String warehouseId, @PathVariable String zoneId,
                                      @PathVariable String aisleId, @Valid @RequestBody CreateBinRequest request) {
        Bin created = service.create(warehouseId, zoneId, aisleId, request);
        URI location = UriComponentsBuilder.fromPath("/api" + BASE + "/{position}")
                .buildAndExpand(created.warehouseId(), created.zoneId(), created.aisleId(), created.position())
                .encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping(BASE + "/{position}")
    public Bin update(@PathVariable String warehouseId, @PathVariable String zoneId,
                      @PathVariable String aisleId, @PathVariable String position,
                      @Valid @RequestBody UpdateBinRequest request) {
        return service.update(warehouseId, zoneId, aisleId, position, request);
    }

    @DeleteMapping(BASE + "/{position}")
    public ResponseEntity<Void> delete(@PathVariable String warehouseId, @PathVariable String zoneId,
                                       @PathVariable String aisleId, @PathVariable String position) {
        service.delete(warehouseId, zoneId, aisleId, position);
        return ResponseEntity.noContent().build();
    }
}
