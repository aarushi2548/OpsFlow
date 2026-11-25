
package opsflow.service;

import opsflow.entity.Warehouse;
import opsflow.repository.WarehouseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public Warehouse createWarehouse(Warehouse warehouse) {

        if (warehouseRepository.existsByCode(warehouse.getCode())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Warehouse code already exists"
            );
        }

        warehouse.setId(null);
        return warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(Long id) {

        return warehouseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Warehouse not found"
            ));
    }

    public Warehouse updateWarehouse(Long id, Warehouse updatedWarehouse) {

        Warehouse existing = getWarehouseById(id);

        if (!existing.getCode().equals(updatedWarehouse.getCode())
                && warehouseRepository.existsByCode(updatedWarehouse.getCode())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Warehouse code already exists"
            );
        }

        existing.setName(updatedWarehouse.getName());
        existing.setCode(updatedWarehouse.getCode());
        existing.setLocation(updatedWarehouse.getLocation());
        existing.setCapacity(updatedWarehouse.getCapacity());
        existing.setActive(updatedWarehouse.getActive());

        return warehouseRepository.save(existing);
    }

    public void deleteWarehouse(Long id) {

        Warehouse warehouse = getWarehouseById(id);
        warehouseRepository.delete(warehouse);
    }
}
