package com.group4.warehouse.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.BinId;

/** Checks done before the transaction starts: a bad request must never reach the database. */
class StockMovementServiceTest {

    private final StockMovementRepository repository = mock(StockMovementRepository.class);
    private final StockMovementService service = new StockMovementService(repository);

    @Test
    void sameSourceAndDestinationIsRejected() {
        CreateMovementRequest request = new CreateMovementRequest(
                "P01", "W02|Z02|A02|B007", "W02|Z02|A02|B007", 10, "ST01");

        ApiException e = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(HttpStatus.BAD_REQUEST, e.status());
        verify(repository, never()).move(anyString(), any(), any(), anyInt(), anyString());
    }

    @Test
    void badBinFormatIsRejected() {
        CreateMovementRequest request = new CreateMovementRequest(
                "P01", "W02-B007", "W03|Z01|A02|B007", 10, "ST01");

        assertThrows(ApiException.class, () -> service.create(request));
        verify(repository, never()).move(anyString(), any(), any(), anyInt(), anyString());
    }

    @Test
    void validRequestIsPassedOnTrimmed() {
        service.create(new CreateMovementRequest(
                " P01 ", "W02|Z02|A02|B007", "W03|Z01|A02|B007", 10, " ST01 "));

        verify(repository).move("P01", new BinId("W02", "Z02", "A02", "B007"),
                new BinId("W03", "Z01", "A02", "B007"), 10, "ST01");
    }
}
