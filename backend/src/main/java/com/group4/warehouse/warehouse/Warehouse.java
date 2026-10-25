package com.group4.warehouse.warehouse;

/**
 * One row of the warehouse table, as the API returns it.
 * JSON: {"id":"WH01","name":"Thu Duc Central","address":"12 Vo Van Ngan, ..."}
 */
public record Warehouse(String id, String name, String address) {
}
