package com.group4.warehouse.count;

/**
 * The statuses allowed by CHECK CHK_SC_STATUS, in the only order a count may go through:
 * Pending -> In Progress -> Completed (steps may be skipped, never reversed).
 * Only Completed changes stock: the counted quantities replace what PRODUCT_BIN held.
 */
public final class CountStatus {

    public static final String PENDING = "Pending";
    public static final String COMPLETED = "Completed";
    /** "" or missing = keep the current status (PATCH) or use the DEFAULT 'Pending' (POST). */
    public static final String PATTERN = "|\\s*(Pending|In Progress|Completed)\\s*";
    public static final String MESSAGE = "Status must be Pending, In Progress or Completed.";

    private CountStatus() {
    }

    /** Pending 0, In Progress 1, Completed 2. */
    public static int rank(String status) {
        return switch (status) {
            case "In Progress" -> 1;
            case COMPLETED -> 2;
            default -> 0;
        };
    }
}
