
package opsflow.service;

import opsflow.dto.*;
import opsflow.entity.*;
import opsflow.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public InventoryService(
        InventoryRepository inventoryRepository,
        StockMovementRepository movementRepository,
        WarehouseRepository warehouseRepository,
        ProductRepository productRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public InventoryResponse create(CreateInventoryRequest request) {

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Warehouse not found"
            ));

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found"
            ));

        if (inventoryRepository.existsByWarehouseIdAndProductId(
            request.warehouseId(), request.productId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Inventory already exists for this product and warehouse"
            );
        }

        Inventory inventory = new Inventory();
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setQuantityOnHand(0);
        inventory.setReorderLevel(request.reorderLevel());

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll(Sort.by("id").descending())
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getById(Long id) {
        return toResponse(getInventory(id));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getByWarehouse(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(
            warehouseId, Sort.by("id").descending()
        ).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getByProduct(Long productId) {
        return inventoryRepository.findByProductId(
            productId, Sort.by("id").descending()
        ).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStock() {
        return inventoryRepository.findLowStockItems()
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public InventoryResponse stockIn(Long id, StockOperationRequest request) {
        Inventory inventory = getInventoryForUpdate(id);

        inventory.setQuantityOnHand(
            inventory.getQuantityOnHand() + request.quantity()
        );

        inventoryRepository.save(inventory);
        saveMovement(inventory, MovementType.STOCK_IN,
            request.quantity(), request.reference(), request.notes());

        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse stockOut(Long id, StockOperationRequest request) {
        Inventory inventory = getInventoryForUpdate(id);

        if (inventory.getQuantityOnHand() < request.quantity()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Insufficient stock. Available quantity: " + inventory.getQuantityOnHand()
            );
        }

        inventory.setQuantityOnHand(
            inventory.getQuantityOnHand() - request.quantity()
        );

        inventoryRepository.save(inventory);
        saveMovement(inventory, MovementType.STOCK_OUT,
            -request.quantity(), request.reference(), request.notes());

        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse adjust(Long id, StockAdjustmentRequest request) {
        Inventory inventory = getInventoryForUpdate(id);

        int oldQuantity = inventory.getQuantityOnHand();
        int newQuantity = request.targetQuantity();
        int difference = newQuantity - oldQuantity;

        inventory.setQuantityOnHand(newQuantity);
        inventoryRepository.save(inventory);

        if (difference != 0) {
            saveMovement(inventory, MovementType.ADJUSTMENT,
                difference, "MANUAL_ADJUSTMENT", request.reason());
        }

        return toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(Long inventoryId) {
        getInventory(inventoryId);

        return movementRepository
            .findByInventoryIdOrderByCreatedAtDesc(inventoryId)
            .stream()
            .map(this::toMovementResponse)
            .toList();
    }

    private Inventory getInventory(Long id) {
        return inventoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory record not found"
            ));
    }

    private Inventory getInventoryForUpdate(Long id) {
        return inventoryRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory record not found"
            ));
    }

    private void saveMovement(
        Inventory inventory,
        MovementType type,
        int quantityChange,
        String reference,
        String notes
    ) {
        StockMovement movement = new StockMovement();
        movement.setInventory(inventory);
        movement.setMovementType(type);
        movement.setQuantityChange(quantityChange);
        movement.setReference(reference);
        movement.setNotes(notes);
        movementRepository.save(movement);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
            inventory.getId(),
            inventory.getWarehouse().getId(),
            inventory.getWarehouse().getName(),
            inventory.getProduct().getId(),
            inventory.getProduct().getName(),
            inventory.getProduct().getSku(),
            inventory.getQuantityOnHand(),
            inventory.getReorderLevel(),
            inventory.getQuantityOnHand() <= inventory.getReorderLevel(),
            inventory.getCreatedAt(),
            inventory.getUpdatedAt()
        );
    }

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        return new StockMovementResponse(
            movement.getId(),
            movement.getInventory().getId(),
            movement.getMovementType(),
            movement.getQuantityChange(),
            movement.getReference(),
            movement.getNotes(),
            movement.getCreatedAt()
        );
    }
}
