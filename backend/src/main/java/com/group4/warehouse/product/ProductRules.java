package com.group4.warehouse.product;

/** Constants shared by the product request classes; they mirror the CHECK constraints in schema.sql. */
final class ProductRules {

    /** The four values allowed by ck_product_storage. Surrounding spaces are trimmed by ProductService. */
    static final String STORAGE_PATTERN = "\\s*(Ambient|Dry Ambient|Cold|Hazardous)\\s*";

    static final String STORAGE_MESSAGE =
            "Storage requirement must be one of: Ambient, Dry Ambient, Cold, Hazardous.";

    private ProductRules() {
    }
}
