package com.group4.warehouse.issue;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Goods issues as a master-detail view.
 *
 *   GET /api/issues?q=CO01        list of issues + customer name   200
 *   GET /api/issues/{id}          one issue                        200 | 404
 *   GET /api/issues/{id}/lines    its lines + product name         200 | 404
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
}
