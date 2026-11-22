package com.group4.warehouse.task;

/** One row of TASK with the assigned staff member's name. */
public record Task(String id, String name, String status, String staffId, String staffName) {

    /** The statuses allowed by CHECK CHK_TASK_STATUS. "" or missing = keep / DEFAULT 'Pending'. */
    public static final String STATUS_PATTERN = "|\\s*(Pending|In Progress|Completed)\\s*";
    public static final String STATUS_MESSAGE = "Status must be Pending, In Progress or Completed.";
}
