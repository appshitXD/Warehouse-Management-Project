package com.group4.warehouse.issue;

/**
 * One row of GOODS_ISSUE_LINE (the "detail"), joined with PRODUCT for the name and unit.
 * binId is the bin's composite key as one string: "W01|Z01|A02|B010".
 */
public record GoodsIssueLine(
        int id,
        String issueId,
        String productId,
        String productName,
        String unit,
        String binId,
        int quantity) {
}
