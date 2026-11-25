
package opsflow.controller;

import opsflow.dto.*;
import opsflow.service.PurchaseOrderService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(
        @Valid @RequestBody CreatePurchaseOrderRequest request
    ) {
        return service.create(request);
    }

    @GetMapping
    public List<PurchaseOrderResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("/{id}/place")
    public PurchaseOrderResponse place(@PathVariable Long id) {
        return service.place(id);
    }

    @PostMapping("/{id}/receive")
    public PurchaseOrderResponse receive(
        @PathVariable Long id,
        @Valid @RequestBody ReceivePurchaseOrderRequest request
    ) {
        return service.receive(id, request);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseOrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
