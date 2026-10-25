package com.group4.warehouse.warehouse;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

/**
 * Business rules for warehouses. The controller only deals with HTTP,
 * the repository only deals with SQL; decisions like "404 or not" happen here.
 */
@Service
public class WarehouseService {

    private final WarehouseRepository repository;

    public WarehouseService(WarehouseRepository repository) {
        this.repository = repository;
    }

    public List<Warehouse> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public Warehouse get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Warehouse " + id + " was not found."));
    }

    public Warehouse create(CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse(
                Text.trim(request.id()),
                Text.trim(request.name()),
                Text.trimToNull(request.address()));
        repository.insert(warehouse);
        return warehouse;
    }

    public Warehouse update(String id, UpdateWarehouseRequest request) {
        if (request.name() == null && request.address() == null) {
            throw ApiException.badRequest("Nothing to update: send name and/or address.");
        }
        // address: null = keep, "" (after trimming) = clear. The repository's SQL handles both.
        boolean found = repository.update(id, Text.trim(request.name()), Text.trim(request.address()));
        if (!found) {
            throw ApiException.notFound("Warehouse " + id + " was not found.");
        }
        return get(id);
    }

    public void delete(String id) {
        if (!repository.delete(id)) {
            throw ApiException.notFound("Warehouse " + id + " was not found.");
        }
    }
}
