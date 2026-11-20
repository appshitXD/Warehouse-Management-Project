package com.group4.warehouse.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** The bin ID string shared with the frontend: "warehouse|zone|aisle|position". */
class BinIdTest {

    @Test
    void parsesFourPartsAndTrimsThem() {
        BinId bin = BinId.parse(" W02 | Z02 | A02 | B007 ", "fromBinId");
        assertEquals(new BinId("W02", "Z02", "A02", "B007"), bin);
    }

    @Test
    void printsBackTheSameFormat() {
        assertEquals("W02|Z02|A02|B007", new BinId("W02", "Z02", "A02", "B007").toString());
    }

    @Test
    void rejectsWrongNumberOfParts() {
        ApiException e = assertThrows(ApiException.class, () -> BinId.parse("W02-B007", "fromBinId"));
        assertEquals(HttpStatus.BAD_REQUEST, e.status());
    }

    @Test
    void rejectsEmptyPartAndNull() {
        assertThrows(ApiException.class, () -> BinId.parse("W02||A02|B007", "toBinId"));
        assertThrows(ApiException.class, () -> BinId.parse(null, "toBinId"));
    }
}
