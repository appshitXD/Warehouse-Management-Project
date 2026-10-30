package com.group4.warehouse.product;

/**
 * One row of the product table, as the API returns it.
 * JSON: {"id":"P01","name":"Laptop","unit":"pcs","storage":"Ambient","reorderLevel":50}
 *
 * reorderLevel is a whole number of units (column INT), so it is Integer, not a decimal type.
 */
public record Product(String id, String name, String unit, String storage, Integer reorderLevel) {
}
