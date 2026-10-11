package com.prem.oms.dto;

import com.prem.oms.entity.OrderStatus;
import com.prem.oms.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ----- auth -----
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, String name, String email, String role) {}

    // ----- catalog -----
    public record CategoryRequest(@NotBlank @Size(max = 100) String name) {}

    public record CategoryResponse(Long id, String name) {}

    public record ProductRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank @Size(max = 50) String sku,
            @Size(max = 500) String imageUrl,
            Long categoryId,
            @Min(0) Integer initialStock) {}

    public record ProductResponse(
            Long id, String name, String description, BigDecimal price, String sku,
            String imageUrl, boolean active, Long categoryId, String categoryName, int stock) {}

    // ----- inventory -----
    public record StockUpdateRequest(@Min(0) int quantity) {}

    public record InventoryResponse(Long productId, String productName, String sku, int quantity) {}

    // ----- orders -----
    public record OrderItemRequest(@NotNull Long productId, @Min(1) int quantity) {}

    public record OrderRequest(
            @NotEmpty @Valid List<OrderItemRequest> items,
            @NotBlank @Size(max = 500) String shippingAddress,
            @NotNull PaymentMethod paymentMethod) {}

    public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice) {}

    public record OrderResponse(
            Long id, String customerName, String customerEmail, OrderStatus status,
            BigDecimal totalAmount, PaymentMethod paymentMethod, String shippingAddress,
            LocalDateTime createdAt, List<OrderItemResponse> items) {}

    public record StatusUpdateRequest(@NotNull OrderStatus status) {}
}
