package com.group4.warehouse.customer;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.Text;

/** Business rules for customers: trimming input, 404 when the ID does not exist. */
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public List<Customer> list(String q) {
        return repository.findAll(Text.trimToNull(q));
    }

    public Customer get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Customer " + id + " was not found."));
    }

    public Customer create(CreateCustomerRequest request) {
        Customer customer = new Customer(
                Text.trim(request.id()),
                Text.trim(request.name()),
                Text.trimToNull(request.phone()),
                Text.trimToNull(request.address()));
        repository.insert(customer);
        return customer;
    }

    public Customer update(String id, UpdateCustomerRequest request) {
        if (request.name() == null && request.phone() == null && request.address() == null) {
            throw ApiException.badRequest("Nothing to update: send name, phone and/or address.");
        }
        boolean found = repository.update(id,
                Text.trim(request.name()),
                Text.trim(request.phone()),
                Text.trim(request.address()));
        if (!found) {
            throw ApiException.notFound("Customer " + id + " was not found.");
        }
        return get(id);
    }

    public void delete(String id) {
        if (!repository.delete(id)) {
            throw ApiException.notFound("Customer " + id + " was not found.");
        }
    }
}
