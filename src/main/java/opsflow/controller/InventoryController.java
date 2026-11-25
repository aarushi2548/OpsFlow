
package opsflow.controller;

import opsflow.dto.*;
import opsflow.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse create(
        @Valid @RequestBody CreateInventoryRequest request
    ) {
        return inventoryService.create(request);
    }

    @GetMapping
    public List<InventoryResponse> getAll() {
        return inventoryService.getAll();
    }

    @GetMapping("/low-stock")
    public List<InventoryResponse> getLowStock() {
        return inventoryService.getLowStock();
    }

    @GetMapping("/warehouse/{warehouseId}")
    public List<InventoryResponse> getByWarehouse(
        @PathVariable Long warehouseId
    ) {
        return inventoryService.getByWarehouse(warehouseId);
    }

    @GetMapping("/product/{productId}")
    public List<InventoryResponse> getByProduct(
        @PathVariable Long productId
    ) {
        return inventoryService.getByProduct(productId);
    }

    @GetMapping("/{id}/movements")
    public List<StockMovementResponse> getMovements(
        @PathVariable Long id
    ) {
        return inventoryService.getMovements(id);
    }

    @GetMapping("/{id}")
    public InventoryResponse getById(@PathVariable Long id) {
        return inventoryService.getById(id);
    }

    @PostMapping("/{id}/stock-in")
    public InventoryResponse stockIn(
        @PathVariable Long id,
        @Valid @RequestBody StockOperationRequest request
    ) {
        return inventoryService.stockIn(id, request);
    }

    @PostMapping("/{id}/stock-out")
    public InventoryResponse stockOut(
        @PathVariable Long id,
        @Valid @RequestBody StockOperationRequest request
    ) {
        return inventoryService.stockOut(id, request);
    }

    @PutMapping("/{id}/adjust")
    public InventoryResponse adjust(
        @PathVariable Long id,
        @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return inventoryService.adjust(id, request);
    }
}
