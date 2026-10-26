package com.group4.warehouse.supplier;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

/** Business rules for suppliers: trimming input, 404 when the ID does not exist. */
@Service
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    public List<Supplier> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public Supplier get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier " + id + " was not found."));
    }

    public Supplier create(CreateSupplierRequest request) {
        Supplier supplier = new Supplier(
                Text.trim(request.id()),
                Text.trim(request.name()),
                Text.trimToNull(request.phone()),
                Text.trimToNull(request.address()));
        repository.insert(supplier);
        return supplier;
    }

    public Supplier update(String id, UpdateSupplierRequest request) {
        if (request.name() == null && request.phone() == null && request.address() == null) {
            throw ApiException.badRequest("Nothing to update: send name, phone and/or address.");
        }
        boolean found = repository.update(id,
                Text.trim(request.name()),
                Text.trim(request.phone()),
                Text.trim(request.address()));
        if (!found) {
            throw ApiException.notFound("Supplier " + id + " was not found.");
        }
        return get(id);
    }

    public void delete(String id) {
        if (!repository.delete(id)) {
            throw ApiException.notFound("Supplier " + id + " was not found.");
        }
    }
}
