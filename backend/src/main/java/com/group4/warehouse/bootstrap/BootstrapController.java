package com.group4.warehouse.bootstrap;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BootstrapController {
    private final BootstrapRepository repository;

    public BootstrapController(BootstrapRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/bootstrap")
    public Map<String, Object> bootstrap() {
        return repository.load();
    }
}
