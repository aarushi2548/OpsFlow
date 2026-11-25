package opsflow.controller;

import opsflow.dto.CreateCustomerOrderRequest;
import opsflow.dto.CustomerOrderResponse;
import opsflow.service.CustomerOrderService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-orders")
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    public CustomerOrderController(CustomerOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrderResponse create(
        @Valid @RequestBody CreateCustomerOrderRequest request
    ) {
        return orderService.create(request);
    }

    @GetMapping
    public List<CustomerOrderResponse> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/{id}")
    public CustomerOrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PostMapping("/{id}/confirm")
    public CustomerOrderResponse confirm(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public CustomerOrderResponse cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}