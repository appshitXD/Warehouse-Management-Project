package com.group4.warehouse.issue;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.StockLine;
import com.group4.warehouse.common.Text;

/** Checks goods issue input before the transaction starts; 404 when the issue does not exist. */
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

    public GoodsIssue create(CreateIssueRequest request) {
        List<StockLine> lines = StockLine.parse(request.lines(), true, false);
        LocalDate date = StockLine.documentTime(request.date()).toLocalDate();
        String id = repository.create(Text.trimToNull(request.id()), date, Text.trim(request.orderId()),
                Text.trim(request.stateOfGood()), Text.trimToNull(request.status()), lines);
        return get(id);
    }

    public GoodsIssue update(String id, UpdateIssueRequest request) {
        if (request.date() == null && request.orderId() == null && request.stateOfGood() == null
                && request.status() == null && request.lines() == null) {
            throw ApiException.badRequest("Nothing to update: send date, orderId, stateOfGood, status and/or lines.");
        }
        LocalDate date = Text.trimToNull(request.date()) == null
                ? null : StockLine.documentTime(request.date()).toLocalDate();
        List<StockLine> lines = request.lines() == null ? null : StockLine.parse(request.lines(), true, false);
        boolean found = repository.update(Text.trim(id), date, Text.trimToNull(request.orderId()),
                Text.trimToNull(request.stateOfGood()), Text.trimToNull(request.status()), lines);
        if (!found) {
            throw notFound(id);
        }
        return get(id);
    }

    public void delete(String id) {
        if (!repository.delete(Text.trim(id))) {
            throw notFound(id);
        }
    }

    private static ApiException notFound(String id) {
        return ApiException.notFound("Goods issue " + id + " was not found.");
    }
}
