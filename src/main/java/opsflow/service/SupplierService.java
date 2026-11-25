
package opsflow.service;

import opsflow.dto.CreateSupplierRequest;
import opsflow.entity.Supplier;
import opsflow.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Supplier create(CreateSupplierRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Supplier email already exists"
            );
        }

        Supplier supplier = new Supplier();
        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setActive(true);

        return repository.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<Supplier> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Supplier getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Supplier not found"
            ));
    }

    @Transactional
    public Supplier deactivate(Long id) {
        Supplier supplier = getById(id);
        supplier.setActive(false);
        return repository.save(supplier);
    }
}
