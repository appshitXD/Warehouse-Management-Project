package com.group4.warehouse.supplier;

/**
 * One row of the supplier table, as the API returns it.
 * JSON: {"id":"S01","name":"Tech Supplies Inc","phone":"555-0101","address":"111 Tech Blvd"}
 */
public record Supplier(String id, String name, String phone, String address) {
}
