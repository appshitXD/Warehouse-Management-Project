package com.group4.warehouse.receipt;

import java.util.List;

/**
 * One goods receipt (GOODS_RECEIPT) with its supplier name and its lines (GOODS_RECEIPT_LINE).
 * date is ISO 8601 with time zone, e.g. "2026-10-15T00:00:00+07:00".
 */
public record GoodsReceipt(
        String id,
        String date,
        String supplierId,
        String supplierName,
        List<ReceiptLine> lines) {

    /** One line: binId is "W02|Z02|A02|B007". */
    public record ReceiptLine(
            int lineNo,
            String productId,
            String productName,
            String unit,
            String binId,
            int quantity) {
    }
}
