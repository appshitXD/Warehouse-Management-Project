package com.group4.warehouse.count;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.StockLine;
import com.group4.warehouse.common.Text;

/** Checks stock count input before the transaction starts; 404 when the count does not exist. */
@Service
public class StockCountService {

    private final StockCountRepository repository;

    public StockCountService(StockCountRepository repository) {
        this.repository = repository;
    }

    public List<StockCount> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public StockCount get(String id) {
        return repository.findById(Text.trim(id)).orElseThrow(() -> notFound(id));
    }

    public List<StockCount.CountLine> lines(String id) {
        return get(id).lines();
    }

    public StockCount create(CreateCountRequest request) {
        List<StockLine> lines = StockLine.parse(request.lines(), false, true);
        LocalDateTime date = StockLine.documentTime(request.date());
        String id = repository.create(Text.trimToNull(request.id()), date, Text.trimToNull(request.status()), lines);
        return get(id);
    }

    public StockCount update(String id, UpdateCountRequest request) {
        if (request.date() == null && request.status() == null && request.lines() == null) {
            throw ApiException.badRequest("Nothing to update: send date, status and/or lines.");
        }
        LocalDateTime date = Text.trimToNull(request.date()) == null ? null : StockLine.documentTime(request.date());
        List<StockLine> lines = request.lines() == null ? null : StockLine.parse(request.lines(), false, true);
        if (!repository.update(Text.trim(id), date, Text.trimToNull(request.status()), lines)) {
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
        return ApiException.notFound("Stock count " + id + " was not found.");
    }
}
