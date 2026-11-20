package com.group4.warehouse.order;

/** A customer order that has no goods issue yet ("waiting to ship"). time is ISO 8601 with zone. */
public record WaitingOrder(String orderId, String time, String status, String customerName) {
}
