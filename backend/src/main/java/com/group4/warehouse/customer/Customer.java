package com.group4.warehouse.customer;

/**
 * One row of the customer table, as the API returns it.
 * JSON: {"id":"C01","name":"Alice Smith","phone":"555-0201","address":"333 Apple St"}
 */
public record Customer(String id, String name, String phone, String address) {
}
