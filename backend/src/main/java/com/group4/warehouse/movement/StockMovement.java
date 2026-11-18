package com.group4.warehouse.movement;

/**
 * One row of STOCK_MOVEMENT, in the shape contracts.js expects (movementDto):
 * bins as "W02|Z02|A02|B007", date as ISO 8601 with time zone.
 */
public record StockMovement(
        String id,
        String date,
        String productId,
        String fromBinId,
        String toBinId,
        String staffId,
        int quantity) {
}
