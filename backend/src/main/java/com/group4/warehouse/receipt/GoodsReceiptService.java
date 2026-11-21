package com.group4.warehouse.receipt;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.StockLine;
import com.group4.warehouse.common.Text;

/** Checks receipt input before the transaction starts; 404 when the receipt does not exist. */
@Service
public class GoodsReceiptService {

    private final GoodsReceiptRepository repository;

    public GoodsReceiptService(GoodsReceiptRepository repository) {
        this.repository = repository;
    }

    public List<GoodsReceipt> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public GoodsReceipt get(String id) {
        return repository.findById(Text.trim(id)).orElseThrow(() -> notFound(id));
    }

    public List<GoodsReceipt.ReceiptLine> lines(String id) {
        return get(id).lines();
    }

    public GoodsReceipt create(CreateReceiptRequest request) {
        List<StockLine> lines = StockLine.parse(request.lines(), true, false);
        LocalDate date = StockLine.documentTime(request.date()).toLocalDate();
        String id = repository.create(Text.trimToNull(request.id()), date, Text.trim(request.supplierId()), lines);
        return get(id);
    }

    public GoodsReceipt update(String id, UpdateReceiptRequest request) {
        if (request.date() == null && request.supplierId() == null && request.lines() == null) {
            throw ApiException.badRequest("Nothing to update: send date, supplierId and/or lines.");
        }
        LocalDate date = Text.trimToNull(request.date()) == null
                ? null : StockLine.documentTime(request.date()).toLocalDate();
        List<StockLine> lines = request.lines() == null ? null : StockLine.parse(request.lines(), true, false);
        if (!repository.update(Text.trim(id), date, Text.trimToNull(request.supplierId()), lines)) {
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
        return ApiException.notFound("Goods receipt " + id + " was not found.");
    }
}
