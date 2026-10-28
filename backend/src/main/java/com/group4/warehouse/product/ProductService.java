package com.group4.warehouse.product;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

/** Business rules for products: trimming input, default reorder level, 404 when the ID does not exist. */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public Product get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product " + id + " was not found."));
    }

    public Product create(CreateProductRequest request) {
        // Same as the column's DEFAULT 0, written here so the response shows the real value.
        BigDecimal reorderLevel = request.reorderLevel() == null ? BigDecimal.ZERO : request.reorderLevel();
        Product product = new Product(
                Text.trim(request.id()),
                Text.trim(request.name()),
                Text.trim(request.unit()),
                Text.trim(request.storage()),
                reorderLevel);
        repository.insert(product);
        return get(product.id());
    }

    public Product update(String id, UpdateProductRequest request) {
        if (request.name() == null && request.unit() == null
                && request.storage() == null && request.reorderLevel() == null) {
            throw ApiException.badRequest("Nothing to update: send name, unit, storage and/or reorderLevel.");
        }
        boolean found = repository.update(id,
                Text.trim(request.name()),
                Text.trim(request.unit()),
                Text.trim(request.storage()),
                request.reorderLevel());
        if (!found) {
            throw ApiException.notFound("Product " + id + " was not found.");
        }
        return get(id);
    }

    public void delete(String id) {
        if (!repository.delete(id)) {
            throw ApiException.notFound("Product " + id + " was not found.");
        }
    }
}
