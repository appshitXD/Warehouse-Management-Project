package com.group4.warehouse.issue;

/**
 * One row of GOODS_ISSUE (the "master"), joined with its order's customer.
 * date is ISO 8601 with time zone, e.g. "2026-10-12T00:00:00+07:00".
 */
public record GoodsIssue(
        String id,
        String date,
        String status,
        String stateOfGood,
        String orderId,
        String customerId,
        String customerName) {
}
