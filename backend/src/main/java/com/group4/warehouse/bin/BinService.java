package com.group4.warehouse.bin;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.aisle.Aisle;
import com.group4.warehouse.aisle.AisleService;
import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

@Service
public class BinService {
    private final BinRepository repository;
    private final AisleService aisleService;

    public BinService(BinRepository repository, AisleService aisleService) {
        this.repository = repository;
        this.aisleService = aisleService;
    }

    public List<Bin> listAll(String warehouseId, String zoneId, String aisleId, String q) {
        return repository.findAll(Text.trimToNull(warehouseId), Text.trimToNull(zoneId),
                Text.trimToNull(aisleId), Text.trimToNull(q));
    }

    public List<Bin> list(String warehouseId, String zoneId, String aisleId, String q) {
        aisleService.get(warehouseId, zoneId, aisleId);
        return repository.findAll(warehouseId, zoneId, aisleId, Text.trimToNull(q));
    }

    public Bin get(String warehouseId, String zoneId, String aisleId, String position) {
        return repository.findById(warehouseId, zoneId, aisleId, position)
                .orElseThrow(() -> notFound(warehouseId, zoneId, aisleId, position));
    }

    public Bin create(String warehouseId, String zoneId, String aisleId, CreateBinRequest request) {
        Aisle aisle = aisleService.get(warehouseId, zoneId, aisleId);
        Bin bin = new Bin(
                aisle.warehouseId(),
                aisle.zoneId(),
                aisle.id(),
                Text.trim(request.position()),
                Text.trimToNull(request.name()));
        repository.insert(bin);
        return bin;
    }

    public Bin update(String warehouseId, String zoneId, String aisleId, String position, UpdateBinRequest request) {
        if (request.name() == null) {
            throw ApiException.badRequest("Nothing to update: send name (\"\" clears it).");
        }

        if (!repository.update(warehouseId, zoneId, aisleId, position, Text.trim(request.name()))) {
            throw notFound(warehouseId, zoneId, aisleId, position);
        }
        return get(warehouseId, zoneId, aisleId, position);
    }

    public void delete(String warehouseId, String zoneId, String aisleId, String position) {
        if (!repository.delete(warehouseId, zoneId, aisleId, position)) {
            throw notFound(warehouseId, zoneId, aisleId, position);
        }
    }

    private static ApiException notFound(String warehouseId, String zoneId, String aisleId, String position) {
        return ApiException.notFound("Bin " + position + " was not found in aisle " + aisleId
                + ", zone " + zoneId + " of warehouse " + warehouseId + ".");
    }
}
