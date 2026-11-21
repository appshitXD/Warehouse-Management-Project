package com.group4.warehouse.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A document line after its input has been checked: the bin is parsed from
 * "W02|Z02|A02|B007" into its four key columns.
 */
public record StockLine(String productId, BinId bin, int quantity) {

    /**
     * Checks every line before any SQL runs, so the user gets one clear message.
     *
     * @param positive       true for receipts and issues (quantity must be > 0)
     * @param uniqueLocation true for stock counts (UNIQUE (Count_ID, Product_ID, bin) in COUNT_LINE)
     */
    public static List<StockLine> parse(List<DocumentLineRequest> lines, boolean positive,
                                        boolean uniqueLocation) {
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("Add at least one line.");
        }
        List<StockLine> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            DocumentLineRequest line = lines.get(i);
            if (line == null) {
                throw ApiException.badRequest("Line " + (i + 1) + " is empty.");
            }
            String productId = Text.trim(line.productId());
            BinId bin = BinId.parse(line.binId(), "Line " + (i + 1) + " bin");
            if (positive && line.quantity() <= 0) {
                throw ApiException.badRequest("Line " + (i + 1) + ": quantity must be greater than 0.");
            }
            if (uniqueLocation && !seen.add(productId + "@" + bin)) {
                throw ApiException.badRequest("Line " + (i + 1) + ": product " + productId
                        + " in bin " + bin + " is already counted on another line.");
            }
            result.add(new StockLine(productId, bin, line.quantity()));
        }
        return result;
    }

    /**
     * The stock change needed to go from the old lines to the new lines, one entry per
     * product and bin, quantity = new total - old total (negative = take stock out).
     * Example: old 20 of P01 in bin X, new 25 -> +5. Working with the difference means
     * editing a receipt only fails if the bin really cannot give back the stock.
     */
    public static List<StockLine> difference(List<StockLine> before, List<StockLine> after) {
        Map<String, StockLine> changes = new LinkedHashMap<>();
        for (StockLine line : before) {
            merge(changes, line, -line.quantity());
        }
        for (StockLine line : after) {
            merge(changes, line, line.quantity());
        }
        List<StockLine> result = new ArrayList<>();
        for (StockLine change : changes.values()) {
            if (change.quantity() != 0) {
                result.add(change);
            }
        }
        return result;
    }

    /** All the lines of one document as stock changes (same product and bin added together). */
    public static List<StockLine> totals(List<StockLine> lines) {
        return difference(List.of(), lines);
    }

    private static void merge(Map<String, StockLine> changes, StockLine line, int quantity) {
        changes.merge(line.productId() + "@" + line.bin(), new StockLine(line.productId(), line.bin(), quantity),
                (a, b) -> new StockLine(a.productId(), a.bin(), a.quantity() + b.quantity()));
    }

    /** Date typed in the form, or now (warehouse time) when the form sends none. */
    public static LocalDateTime documentTime(String value) {
        String text = Text.trimToNull(value);
        return text == null ? IsoTime.now() : Dates.parseDateTime(text, IsoTime.ZONE, "date").withNano(0);
    }
}
