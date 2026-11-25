
package opsflow.controller;

import opsflow.dto.CreateSupplierRequest;
import opsflow.entity.Supplier;
import opsflow.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Supplier create(@Valid @RequestBody CreateSupplierRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<Supplier> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Supplier getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PatchMapping("/{id}/deactivate")
    public Supplier deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }
}
