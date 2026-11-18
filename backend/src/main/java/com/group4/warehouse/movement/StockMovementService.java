package com.group4.warehouse.movement;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.BinId;
import com.group4.warehouse.common.Text;

/** Checks a movement request before the transaction starts. */
@Service
public class StockMovementService {

    private final StockMovementRepository repository;

    public StockMovementService(StockMovementRepository repository) {
        this.repository = repository;
    }

    public StockMovement create(CreateMovementRequest request) {
        BinId from = BinId.parse(request.fromBinId(), "fromBinId");
        BinId to = BinId.parse(request.toBinId(), "toBinId");
        // Same rule as CHECK CHK_SM_BIN_DIFF, but the user gets a clear message before any SQL runs.
        if (from.equals(to)) {
            throw ApiException.badRequest("Source and destination bins must be different.");
        }
        return repository.move(
                Text.trim(request.productId()), from, to, request.quantity(), Text.trim(request.staffId()));
    }
}
