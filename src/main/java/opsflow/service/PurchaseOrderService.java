
package opsflow.service;

import opsflow.dto.CreatePurchaseOrderRequest;
import opsflow.dto.PurchaseOrderItemRequest;
import opsflow.dto.PurchaseOrderItemResponse;
import opsflow.dto.PurchaseOrderResponse;
import opsflow.dto.ReceivePurchaseOrderRequest;
import opsflow.entity.Inventory;
import opsflow.entity.MovementType;
import opsflow.entity.Product;
import opsflow.entity.PurchaseOrder;
import opsflow.entity.PurchaseOrderItem;
import opsflow.entity.PurchaseOrderStatus;
import opsflow.entity.StockMovement;
import opsflow.entity.Supplier;
import opsflow.entity.Warehouse;
import opsflow.repository.InventoryRepository;
import opsflow.repository.ProductRepository;
import opsflow.repository.PurchaseOrderRepository;
import opsflow.repository.StockMovementRepository;
import opsflow.repository.SupplierRepository;
import opsflow.repository.WarehouseRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierRepository supplierRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    // CREATE PURCHASE ORDER
    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Supplier not found"));

        if (!supplier.getActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Supplier is inactive");
        }

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Warehouse not found"));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Purchase order must contain items");
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplier(supplier);
        order.setWarehouse(warehouse);
        order.setStatus(PurchaseOrderStatus.DRAFT);

        for (PurchaseOrderItemRequest itemRequest : request.items()) {

            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Product not found"));

            if (itemRequest.orderedQuantity() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Ordered quantity must be positive");
            }

            if (itemRequest.unitPrice() == null ||
                    itemRequest.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unit price cannot be negative");
            }

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setOrderedQuantity(itemRequest.orderedQuantity());
            item.setReceivedQuantity(0);
            item.setUnitPrice(itemRequest.unitPrice());

            order.addItem(item);
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        return toResponse(savedOrder);
    }

    // GET ALL PURCHASE ORDERS
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAll() {

        return purchaseOrderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET PURCHASE ORDER BY ID
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {

        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Purchase order not found"));

        return toResponse(order);
    }

    // PLACE PURCHASE ORDER
    @Transactional
    public PurchaseOrderResponse place(Long id) {

        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Purchase order not found"));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only draft purchase orders can be placed");
        }

        order.setStatus(PurchaseOrderStatus.PLACED);

        return toResponse(purchaseOrderRepository.save(order));
    }

    // RECEIVE GOODS
    @Transactional
    public PurchaseOrderResponse receive(
            Long orderId,
            ReceivePurchaseOrderRequest request) {

        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Purchase order not found"));

        // FIX: Allow both PLACED and PARTIALLY_RECEIVED orders.
        if (order.getStatus() != PurchaseOrderStatus.PLACED &&
                order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Purchase order must be placed before receiving goods");
        }

        PurchaseOrderItem selectedItem = order.getItems()
                .stream()
                .filter(item -> item.getId().equals(request.itemId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase order item not found"));

        if (request.quantityReceived() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Received quantity must be positive");
        }

        int pendingQuantity = selectedItem.getOrderedQuantity()
                - selectedItem.getReceivedQuantity();

        if (request.quantityReceived() > pendingQuantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Received quantity exceeds pending quantity");
        }

        Long warehouseId = order.getWarehouse().getId();
        Long productId = selectedItem.getProduct().getId();

        Inventory inventory = inventoryRepository
                .findWithDetailsByWarehouseIdAndProductId(
                        warehouseId, productId)
                .orElseGet(() -> {

                    Inventory newInventory = new Inventory();
                    newInventory.setWarehouse(order.getWarehouse());
                    newInventory.setProduct(selectedItem.getProduct());
                    newInventory.setQuantityOnHand(0);
                    newInventory.setReorderLevel(10);

                    return inventoryRepository.save(newInventory);
                });

        // Update inventory
        inventory.setQuantityOnHand(
                inventory.getQuantityOnHand() + request.quantityReceived());

        inventoryRepository.save(inventory);

        // Record stock movement
        StockMovement movement = new StockMovement();
        movement.setInventory(inventory);
        movement.setMovementType(MovementType.STOCK_IN);
        movement.setQuantityChange(request.quantityReceived());
        movement.setReference(request.reference());
        movement.setNotes(request.notes());

        stockMovementRepository.save(movement);

        // Update purchase order item
        selectedItem.setReceivedQuantity(
                selectedItem.getReceivedQuantity()
                        + request.quantityReceived());

        // Calculate total received quantity
        boolean allReceived = order.getItems()
                .stream()
                .allMatch(item ->
                        item.getReceivedQuantity()
                                .equals(item.getOrderedQuantity()));

        boolean anyReceived = order.getItems()
                .stream()
                .anyMatch(item -> item.getReceivedQuantity() > 0);

        if (allReceived) {
            order.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (anyReceived) {
            order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        return toResponse(savedOrder);
    }

    // CANCEL PURCHASE ORDER
    @Transactional
    public PurchaseOrderResponse cancel(Long id) {

        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Purchase order not found"));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT &&
                order.getStatus() != PurchaseOrderStatus.PLACED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only draft or placed orders can be cancelled");
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);

        return toResponse(purchaseOrderRepository.save(order));
    }

    // CONVERT ENTITY TO RESPONSE DTO
    private PurchaseOrderResponse toResponse(PurchaseOrder order) {

        List<PurchaseOrderItemResponse> itemResponses =
                order.getItems().stream()
                        .map(item -> {

                            BigDecimal lineTotal = item.getUnitPrice()
                                    .multiply(BigDecimal.valueOf(
                                            item.getOrderedQuantity()));

                            return new PurchaseOrderItemResponse(
                                    item.getId(),
                                    item.getProduct().getId(),
                                    item.getProduct().getName(),
                                    item.getProduct().getSku(),
                                    item.getOrderedQuantity(),
                                    item.getReceivedQuantity(),
                                    item.getOrderedQuantity()
                                            - item.getReceivedQuantity(),
                                    item.getUnitPrice(),
                                    lineTotal
                            );
                        })
                        .toList();

        BigDecimal totalAmount = itemResponses.stream()
                .map(PurchaseOrderItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchaseOrderResponse(
                order.getId(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getWarehouse().getId(),
                order.getWarehouse().getName(),
                order.getStatus(),
                itemResponses,
                totalAmount,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
