package com.group4.warehouse.aisle;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;
import com.group4.warehouse.zone.Zone;
import com.group4.warehouse.zone.ZoneService;

@Service
public class AisleService {
    private final AisleRepository repository;
    private final ZoneService zoneService;

    public AisleService(AisleRepository repository, ZoneService zoneService) {
        this.repository = repository;
        this.zoneService = zoneService;
    }

    public List<Aisle> listAll(String warehouseId, String zoneId, String q) {
        return repository.findAll(Text.trimToNull(warehouseId), Text.trimToNull(zoneId), Text.trimToNull(q));
    }

    public List<Aisle> list(String warehouseId, String zoneId, String q) {
        zoneService.get(warehouseId, zoneId);
        return repository.findAll(warehouseId, zoneId, Text.trimToNull(q));
    }

    public Aisle get(String warehouseId, String zoneId, String aisleId) {
        return repository.findById(warehouseId, zoneId, aisleId)
                .orElseThrow(() -> notFound(warehouseId, zoneId, aisleId));
    }

    public Aisle create(String warehouseId, String zoneId, CreateAisleRequest request) {
        Zone zone = zoneService.get(warehouseId, zoneId);
        Aisle aisle = new Aisle(
                zone.warehouseId(),
                zone.id(),
                Text.trim(request.id()),
                Text.trim(request.name()));
        repository.insert(aisle);
        return aisle;
    }

    public Aisle update(String warehouseId, String zoneId, String aisleId, UpdateAisleRequest request) {
        if (request.name() == null) {
            throw ApiException.badRequest("Nothing to update: send name.");
        }
        if (!repository.update(warehouseId, zoneId, aisleId, Text.trim(request.name()))) {
            throw notFound(warehouseId, zoneId, aisleId);
        }
        return get(warehouseId, zoneId, aisleId);
    }

    public void delete(String warehouseId, String zoneId, String aisleId) {
        if (!repository.delete(warehouseId, zoneId, aisleId)) {
            throw notFound(warehouseId, zoneId, aisleId);
        }
    }

    private static ApiException notFound(String warehouseId, String zoneId, String aisleId) {
        return ApiException.notFound("Aisle " + aisleId + " was not found in zone " + zoneId
                + " of warehouse " + warehouseId + ".");
    }
}
