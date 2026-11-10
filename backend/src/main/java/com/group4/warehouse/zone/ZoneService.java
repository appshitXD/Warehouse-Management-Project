package com.group4.warehouse.zone;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;
import com.group4.warehouse.warehouse.Warehouse;
import com.group4.warehouse.warehouse.WarehouseService;

@Service
public class ZoneService {
    private final ZoneRepository repository;
    private final WarehouseService warehouseService;

    public ZoneService(ZoneRepository repository, WarehouseService warehouseService) {
        this.repository = repository;
        this.warehouseService = warehouseService;
    }

    public List<Zone> listAll(String warehouseId, String q) {
        return repository.findAll(Text.trimToNull(warehouseId), Text.trimToNull(q));
    }

    public List<Zone> list(String warehouseId, String q) {
        warehouseService.get(warehouseId);
        return repository.findAll(warehouseId, Text.trimToNull(q));
    }

    public Zone get(String warehouseId, String zoneId) {
        return repository.findById(warehouseId, zoneId)
                .orElseThrow(() -> notFound(warehouseId, zoneId));
    }

    public Zone create(String warehouseId, CreateZoneRequest request) {
        Warehouse warehouse = warehouseService.get(warehouseId);
        Zone zone = new Zone(
                warehouse.id(),
                Text.trim(request.id()),
                Text.trim(request.name()));
        repository.insert(zone);
        return zone;
    }

    public Zone update(String warehouseId, String zoneId, UpdateZoneRequest request) {
        if (request.name() == null) {
            throw ApiException.badRequest("Nothing to update: send name.");
        }
        if (!repository.update(warehouseId, zoneId, Text.trim(request.name()))) {
            throw notFound(warehouseId, zoneId);
        }
        return get(warehouseId, zoneId);
    }

    public void delete(String warehouseId, String zoneId) {
        if (!repository.delete(warehouseId, zoneId)) {
            throw notFound(warehouseId, zoneId);
        }
    }

    private static ApiException notFound(String warehouseId, String zoneId) {
        return ApiException.notFound("Zone " + zoneId + " was not found in warehouse " + warehouseId + ".");
    }
}
