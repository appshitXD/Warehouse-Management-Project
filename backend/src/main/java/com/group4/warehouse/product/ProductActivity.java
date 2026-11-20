package com.group4.warehouse.product;

/**
 * One event in a product's history: a goods receipt (+), a goods issue (-) or a stock movement.
 * location is the warehouse, or "W01 -> W02" for a movement.
 */
public record ProductActivity(String activityDate, String type, String documentId, int quantity,
                              String location) {
}
