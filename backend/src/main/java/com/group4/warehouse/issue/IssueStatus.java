package com.group4.warehouse.issue;

/**
 * The statuses allowed by CHECK CHK_GI_STATUS, in the only order an issue may go through:
 * Pending -> Shipped -> Completed (Pending -> Completed is allowed too).
 * Pending = nothing has left the warehouse yet; Shipped/Completed = stock has been taken out.
 */
public final class IssueStatus {

    public static final String PENDING = "Pending";
    /** "" or missing = keep the current status (PATCH) or use the DEFAULT 'Pending' (POST). */
    public static final String PATTERN = "|\\s*(Pending|Shipped|Completed)\\s*";
    public static final String MESSAGE = "Status must be Pending, Shipped or Completed.";

    private IssueStatus() {
    }

    /** Pending 0, Shipped 1, Completed 2. */
    public static int rank(String status) {
        return switch (status) {
            case "Shipped" -> 1;
            case "Completed" -> 2;
            default -> 0;
        };
    }

    /** true once the goods have left: the lines are already taken out of stock. */
    public static boolean shipped(String status) {
        return rank(status) > 0;
    }
}
