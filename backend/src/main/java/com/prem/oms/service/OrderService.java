package com.prem.oms.service;

import com.prem.oms.dto.Dtos.*;
import com.prem.oms.entity.*;
import com.prem.oms.exception.ApiException;
import com.prem.oms.repository.InventoryRepository;
import com.prem.oms.repository.OrderRepository;
import com.prem.oms.repository.ProductRepository;
import com.prem.oms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        ProductRepository productRepository, InventoryRepository inventoryRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /** Places an order: validates stock under a row lock, decrements inventory and totals the price. */
    @Transactional
    public OrderResponse place(String email, OrderRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentMethod(req.paymentMethod());
        order.setShippingAddress(req.shippingAddress().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest ir : req.items()) {
            Product p = productRepository.findById(ir.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found: " + ir.productId()));
            Inventory inv = inventoryRepository.findByProductIdForUpdate(p.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No stock record for " + p.getName()));
            if (inv.getQuantity() < ir.quantity()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Insufficient stock for " + p.getName() + " (available: " + inv.getQuantity() + ")");
            }
            inv.setQuantity(inv.getQuantity() - ir.quantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(p);
            item.setQuantity(ir.quantity());
            item.setUnitPrice(p.getPrice());
            order.getItems().add(item);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(ir.quantity())));
        }
        order.setTotalAmount(total);
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(String email) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(email).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(String email, boolean admin, Long id) {
        Order o = find(id);
        checkAccess(o, email, admin);
        return toResponse(o);
    }

    @Transactional
    public OrderResponse cancel(String email, boolean admin, Long id) {
        Order o = find(id);
        checkAccess(o, email, admin);
        if (o.getStatus() != OrderStatus.PLACED && o.getStatus() != OrderStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "Order can no longer be cancelled (status: " + o.getStatus() + ")");
        }
        restoreStock(o);
        o.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(o));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> adminList(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus target) {
        Order o = find(id);
        if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.DELIVERED) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is already " + o.getStatus() + " and cannot change");
        }
        if (target == OrderStatus.CANCELLED) {
            restoreStock(o);
        }
        o.setStatus(target);
        return toResponse(orderRepository.save(o));
    }

    private void restoreStock(Order o) {
        for (OrderItem item : o.getItems()) {
            inventoryRepository.findByProductIdForUpdate(item.getProduct().getId())
                    .ifPresent(inv -> inv.setQuantity(inv.getQuantity() + item.getQuantity()));
        }
    }

    private Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private void checkAccess(Order o, String email, boolean admin) {
        if (!admin && !o.getUser().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this order");
        }
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(o.getId(), o.getUser().getName(), o.getUser().getEmail(), o.getStatus(),
                o.getTotalAmount(), o.getPaymentMethod(), o.getShippingAddress(), o.getCreatedAt(), items);
    }
}
