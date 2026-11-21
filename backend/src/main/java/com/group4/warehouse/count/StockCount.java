package com.group4.warehouse.count;

import java.util.List;

/**
 * One stock count (STOCK_COUNT) with its lines (COUNT_LINE).
 * date is ISO 8601 with time zone, e.g. "2026-10-16T06:45:00+07:00".
 */
public record StockCount(String id, String date, String status, List<CountLine> lines) {

    /**
     * recordedQuantity = what PRODUCT_BIN said, countedQuantity = what staff found,
     * difference = counted - recorded (negative = stock is missing).
     */
    public record CountLine(
            int lineNo,
            String productId,
            String productName,
            String unit,
            String binId,
            int recordedQuantity,
            int countedQuantity,
            int difference) {
    }
}
