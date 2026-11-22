package com.group4.warehouse.count;

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
 * Stock counts (master STOCK_COUNT, detail COUNT_LINE).
 *
 *   GET    /api/counts?q=SC19        list / search (ID or status)              200
 *   GET    /api/counts/{id}          one count with its lines                  200 | 404
 *   GET    /api/counts/{id}/lines    its lines with recorded/counted/difference 200 | 404
 *   POST   /api/counts               create (Completed = apply to stock)       201 | 400 | 409
 *   PATCH  /api/counts/{id}          edit until Completed                      200 | 400 | 404 | 409
 *   DELETE /api/counts/{id}          delete, only while Pending                204 | 404 | 409
 */
@RestController
@RequestMapping("/api/counts")
public class StockCountController {

    private final StockCountService service;

    public StockCountController(StockCountService service) {
        this.service = service;
    }

    @GetMapping
    public List<StockCount> list(@RequestParam(required = false) String q) {
        return service.list(q);
    }

    @GetMapping("/{id}")
    public StockCount get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping("/{id}/lines")
    public List<StockCount.CountLine> lines(@PathVariable String id) {
        return service.lines(id);
    }

    @PostMapping
    public ResponseEntity<StockCount> create(@Valid @RequestBody CreateCountRequest request) {
        StockCount created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/counts/{id}")
                .buildAndExpand(created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public StockCount update(@PathVariable String id, @Valid @RequestBody UpdateCountRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
