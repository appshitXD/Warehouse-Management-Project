package com.group4.warehouse.receipt;

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
 * Goods receipts (master GOODS_RECEIPT, detail GOODS_RECEIPT_LINE). Saving changes stock.
 *
 *   GET    /api/receipts?q=GR01       list / search (ID or supplier name)   200
 *   GET    /api/receipts/{id}         one receipt with its lines            200 | 404
 *   GET    /api/receipts/{id}/lines   its lines                             200 | 404
 *   POST   /api/receipts              create, adds stock to the bins        201 | 400 | 409
 *   PATCH  /api/receipts/{id}         change date / supplier / lines        200 | 400 | 404 | 409
 *   DELETE /api/receipts/{id}         delete, takes the stock back out      204 | 404 | 409
 */
@RestController
@RequestMapping("/api/receipts")
public class GoodsReceiptController {

    private final GoodsReceiptService service;

    public GoodsReceiptController(GoodsReceiptService service) {
        this.service = service;
    }

    @GetMapping
    public List<GoodsReceipt> list(@RequestParam(required = false) String q) {
        return service.list(q);
    }

    @GetMapping("/{id}")
    public GoodsReceipt get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping("/{id}/lines")
    public List<GoodsReceipt.ReceiptLine> lines(@PathVariable String id) {
        return service.lines(id);
    }

    @PostMapping
    public ResponseEntity<GoodsReceipt> create(@Valid @RequestBody CreateReceiptRequest request) {
        GoodsReceipt created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/receipts/{id}")
                .buildAndExpand(created.id()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public GoodsReceipt update(@PathVariable String id, @Valid @RequestBody UpdateReceiptRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
