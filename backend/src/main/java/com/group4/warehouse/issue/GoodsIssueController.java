package com.group4.warehouse.issue;

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
 * Goods issues as a master-detail view, with create / edit / delete.
 *
 *   GET    /api/issues?q=CO01        list of issues + customer name           200
 *   GET    /api/issues/{id}          one issue                                200 | 404
 *   GET    /api/issues/{id}/lines    its lines + product name                 200 | 404
 *   POST   /api/issues               create (Shipped/Completed takes stock)   201 | 400 | 409
 *   PATCH  /api/issues/{id}          edit while Pending, or move the status   200 | 400 | 404 | 409
 *   DELETE /api/issues/{id}          delete, only while Pending               204 | 404 | 409
 */
@RestController
@RequestMapping("/api/issues")
public class GoodsIssueController {

    private final GoodsIssueService service;

    public GoodsIssueController(GoodsIssueService service) {
        this.service = service;
    }

    @GetMapping
    public List<GoodsIssue> list(@RequestParam(required = false) String q) {
        return service.list(q);
    }

    @GetMapping("/{id}")
    public GoodsIssue get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping("/{id}/lines")
    public List<GoodsIssueLine> lines(@PathVariable String id) {
        return service.lines(id);
    }

    @PostMapping
    public ResponseEntity<GoodsIssue> create(@Valid @RequestBody CreateIssueRequest request) {
        GoodsIssue created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/issues/{id}")
                .buildAndExpand(created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public GoodsIssue update(@PathVariable String id, @Valid @RequestBody UpdateIssueRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
