package com.group4.warehouse.order;

/**
 * One row of CUSTOMER_ORDER with the customer's name.
 * time is ISO 8601 with time zone, e.g. "2026-10-17T12:15:00+07:00".
 */
public record CustomerOrder(String id, String time, String status, String customerId, String customerName) {

    /** The statuses allowed by CHECK CHK_ORDER_STATUS. "" or missing = keep / DEFAULT 'Pending'. */
    public static final String STATUS_PATTERN = "|\\s*(Pending|Shipped|Delivered|Cancelled)\\s*";
    public static final String STATUS_MESSAGE = "Status must be Pending, Shipped, Delivered or Cancelled.";
}
