package com.group4.warehouse.issue;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

/** Rules for reading goods issues: trimming input, 404 when the issue does not exist. */
@Service
public class GoodsIssueService {

    private final GoodsIssueRepository repository;

    public GoodsIssueService(GoodsIssueRepository repository) {
        this.repository = repository;
    }

    public List<GoodsIssue> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public GoodsIssue get(String id) {
        return repository.findById(Text.trim(id))
                .orElseThrow(() -> notFound(id));
    }

    /**
     * Lines of one issue. The issue is looked up first so that an unknown ID gives 404,
     * while a known issue without lines gives an empty list [].
     */
    public List<GoodsIssueLine> lines(String id) {
        GoodsIssue issue = get(id);
        return repository.findLines(issue.id());
    }

    private static ApiException notFound(String id) {
        return ApiException.notFound("Goods issue " + id + " was not found.");
    }
}
