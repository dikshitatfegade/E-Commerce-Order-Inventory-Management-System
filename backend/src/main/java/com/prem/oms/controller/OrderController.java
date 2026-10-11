package com.prem.oms.controller;

import com.prem.oms.dto.Dtos.OrderRequest;
import com.prem.oms.dto.Dtos.OrderResponse;
import com.prem.oms.dto.Dtos.StatusUpdateRequest;
import com.prem.oms.entity.OrderStatus;
import com.prem.oms.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(Authentication auth, @Valid @RequestBody OrderRequest req) {
        return orderService.place(auth.getName(), req);
    }

    @GetMapping("/orders/my")
    public List<OrderResponse> myOrders(Authentication auth) {
        return orderService.myOrders(auth.getName());
    }

    @GetMapping("/orders/{id}")
    public OrderResponse get(Authentication auth, @PathVariable Long id) {
        return orderService.get(auth.getName(), isAdmin(auth), id);
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderResponse cancel(Authentication auth, @PathVariable Long id) {
        return orderService.cancel(auth.getName(), isAdmin(auth), id);
    }

    // ----- admin -----
    @GetMapping("/admin/orders")
    public List<OrderResponse> adminOrders(@RequestParam(required = false) OrderStatus status) {
        return orderService.adminList(status);
    }

    @PutMapping("/admin/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return orderService.updateStatus(id, req.status());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
