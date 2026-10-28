package com.group4.warehouse.product;

import java.math.BigDecimal;

/**
 * One row of the product table, as the API returns it.
 * JSON: {"id":"P01","name":"Laptop","unit":"pcs","storage":"Ambient","reorderLevel":50.000}
 *
 * reorderLevel is BigDecimal, not double: the column is DECIMAL(12,3), and double cannot
 * store values like 0.1 exactly. BigDecimal keeps the exact number the database holds.
 */
public record Product(String id, String name, String unit, String storage, BigDecimal reorderLevel) {
}
