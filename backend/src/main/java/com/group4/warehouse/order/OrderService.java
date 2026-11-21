package com.group4.warehouse.order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.StockLine;
import com.group4.warehouse.common.Text;

/** Rules for customer orders: trimming input, 404 when the order does not exist. */
@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public List<CustomerOrder> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public CustomerOrder get(String id) {
        return repository.findById(Text.trim(id)).orElseThrow(() -> notFound(id));
    }

    public CustomerOrder create(CreateOrderRequest request) {
        String id = repository.insert(Text.trimToNull(request.id()), StockLine.documentTime(request.time()),
                Text.trimToNull(request.status()), Text.trim(request.customerId()));
        return get(id);
    }

    public CustomerOrder update(String id, UpdateOrderRequest request) {
        if (request.time() == null && request.status() == null && request.customerId() == null) {
            throw ApiException.badRequest("Nothing to update: send time, status and/or customerId.");
        }
        LocalDateTime time = Text.trimToNull(request.time()) == null ? null : StockLine.documentTime(request.time());
        if (!repository.update(Text.trim(id), time, Text.trimToNull(request.status()),
                Text.trimToNull(request.customerId()))) {
            throw notFound(id);
        }
        return get(id);
    }

    public void delete(String id) {
        if (!repository.delete(Text.trim(id))) {
            throw notFound(id);
        }
    }

    private static ApiException notFound(String id) {
        return ApiException.notFound("Customer order " + id + " was not found.");
    }
}
