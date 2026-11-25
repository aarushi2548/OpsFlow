package opsflow.service;

import opsflow.dto.*;
import opsflow.entity.*;
import opsflow.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;

    public CustomerOrderService(
            CustomerOrderRepository orderRepository,
            CustomerRepository customerRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            StockMovementRepository movementRepository) {

        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
    }

    // CREATE A PENDING ORDER
    @Transactional
    public CustomerOrderResponse create(CreateCustomerOrderRequest request) {

        Customer customer = customerRepository.findById(request.customerId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer not found"
            ));

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Customer is inactive"
            );
        }

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Warehouse not found"
            ));

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setWarehouse(warehouse);

        Set<Long> productIds = new HashSet<>();

        for (CustomerOrderItemRequest itemRequest : request.items()) {

            if (!productIds.add(itemRequest.productId())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A product cannot appear twice in the same order"
                );
            }

            Product product = productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found: " + itemRequest.productId()
                ));

            if (product.getPrice() == null ||
                    product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid product price"
                );
            }

            CustomerOrderItem item = new CustomerOrderItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getPrice());

            order.addItem(item);
        }

        return toResponse(orderRepository.save(order));
    }

    // GET ALL ORDERS
    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getAll() {
        return orderRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // GET ORDER BY ID
    @Transactional(readOnly = true)
    public CustomerOrderResponse getById(Long id) {
        return toResponse(findOrder(id));
    }

    // CONFIRM ORDER AND DEDUCT STOCK
    @Transactional
    public CustomerOrderResponse confirm(Long id) {

        CustomerOrder order = orderRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer order not found"
            ));

        if (order.getStatus() != CustomerOrderStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only pending orders can be confirmed"
            );
        }

        // Lock inventory rows in a consistent product order.
        List<CustomerOrderItem> sortedItems = order.getItems()
            .stream()
            .sorted(Comparator.comparing(
                item -> item.getProduct().getId()
            ))
            .toList();

        // Check and deduct stock.
        for (CustomerOrderItem item : sortedItems) {

            Inventory existing = inventoryRepository
                .findByWarehouseIdAndProductId(
                    order.getWarehouse().getId(),
                    item.getProduct().getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No inventory record found for product: "
                        + item.getProduct().getName()
                ));

            Inventory inventory = inventoryRepository
                .findByIdForUpdate(existing.getId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Inventory record not found"
                ));

            if (inventory.getQuantityOnHand() < item.getQuantity()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock for " + item.getProduct().getName()
                    + ". Available: " + inventory.getQuantityOnHand()
                    + ", Required: " + item.getQuantity()
                );
            }

            inventory.setQuantityOnHand(
                inventory.getQuantityOnHand() - item.getQuantity()
            );

            inventoryRepository.save(inventory);

            StockMovement movement = new StockMovement();
            movement.setInventory(inventory);
            movement.setMovementType(MovementType.STOCK_OUT);
            movement.setQuantityChange(-item.getQuantity());
            movement.setReference("CO-" + order.getId());
            movement.setNotes(
                "Stock deducted for customer order " + order.getId()
            );

            movementRepository.save(movement);
        }

        order.setStatus(CustomerOrderStatus.CONFIRMED);

        return toResponse(orderRepository.save(order));
    }

    // CANCEL ORDER AND RESTORE STOCK IF IT WAS CONFIRMED
    @Transactional
    public CustomerOrderResponse cancel(Long id) {

        CustomerOrder order = orderRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer order not found"
            ));

        if (order.getStatus() == CustomerOrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Order is already cancelled"
            );
        }

        if (order.getStatus() == CustomerOrderStatus.CONFIRMED) {

            List<CustomerOrderItem> sortedItems = order.getItems()
                .stream()
                .sorted(Comparator.comparing(
                    item -> item.getProduct().getId()
                ))
                .toList();

            for (CustomerOrderItem item : sortedItems) {

                Inventory existing = inventoryRepository
                    .findByWarehouseIdAndProductId(
                        order.getWarehouse().getId(),
                        item.getProduct().getId()
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory record missing during cancellation"
                    ));

                Inventory inventory = inventoryRepository
                    .findByIdForUpdate(existing.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventory record not found"
                    ));

                inventory.setQuantityOnHand(
                    inventory.getQuantityOnHand() + item.getQuantity()
                );

                inventoryRepository.save(inventory);

                StockMovement movement = new StockMovement();
                movement.setInventory(inventory);
                movement.setMovementType(MovementType.STOCK_IN);
                movement.setQuantityChange(item.getQuantity());
                movement.setReference("CO-CANCEL-" + order.getId());
                movement.setNotes(
                    "Stock restored after cancellation of order "
                        + order.getId()
                );

                movementRepository.save(movement);
            }
        }

        order.setStatus(CustomerOrderStatus.CANCELLED);

        return toResponse(orderRepository.save(order));
    }

    private CustomerOrder findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer order not found"
            ));
    }

    private CustomerOrderResponse toResponse(CustomerOrder order) {

        List<CustomerOrderItemResponse> itemResponses =
            order.getItems().stream().map(item -> {

                BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

                return new CustomerOrderItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getSku(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    lineTotal
                );
            }).toList();

        BigDecimal total = itemResponses.stream()
            .map(CustomerOrderItemResponse::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CustomerOrderResponse(
            order.getId(),
            order.getCustomer().getId(),
            order.getCustomer().getName(),
            order.getWarehouse().getId(),
            order.getWarehouse().getName(),
            order.getStatus(),
            itemResponses,
            total,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}