package com.prem.oms.service;

import com.prem.oms.dto.Dtos.OrderItemRequest;
import com.prem.oms.dto.Dtos.OrderRequest;
import com.prem.oms.dto.Dtos.OrderResponse;
import com.prem.oms.entity.*;
import com.prem.oms.exception.ApiException;
import com.prem.oms.repository.InventoryRepository;
import com.prem.oms.repository.OrderRepository;
import com.prem.oms.repository.ProductRepository;
import com.prem.oms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryRepository inventoryRepository;
    @InjectMocks OrderService orderService;

    private User user;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@oms.com");
        user.setRole(Role.CUSTOMER);

        product = new Product();
        product.setId(10L);
        product.setName("Widget");
        product.setPrice(new BigDecimal("100.00"));
        product.setSku("W-1");

        inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(10);
    }

    private OrderRequest request(int qty) {
        return new OrderRequest(List.of(new OrderItemRequest(10L, qty)), "221B Baker Street", PaymentMethod.COD);
    }

    @Test
    void placeOrder_decrementsStockAndComputesTotal() {
        when(userRepository.findByEmail("test@oms.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = orderService.place("test@oms.com", request(3));

        assertEquals(7, inventory.getQuantity());
        assertEquals(0, new BigDecimal("300.00").compareTo(resp.totalAmount()));
        assertEquals(OrderStatus.PLACED, resp.status());
        assertEquals(1, resp.items().size());
    }

    @Test
    void placeOrder_insufficientStock_throwsConflictAndSavesNothing() {
        when(userRepository.findByEmail("test@oms.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventory));

        ApiException ex = assertThrows(ApiException.class, () -> orderService.place("test@oms.com", request(11)));

        assertEquals(409, ex.getStatus().value());
        assertEquals(10, inventory.getQuantity());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_restoresStock() {
        Order order = new Order();
        order.setId(5L);
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(new BigDecimal("200.00"));
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        order.getItems().add(item);

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse resp = orderService.cancel("test@oms.com", false, 5L);

        assertEquals(12, inventory.getQuantity());
        assertEquals(OrderStatus.CANCELLED, resp.status());
    }

    @Test
    void cancelOrder_byAnotherCustomer_isForbidden() {
        Order order = new Order();
        order.setId(5L);
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class, () -> orderService.cancel("other@oms.com", false, 5L));
        assertEquals(403, ex.getStatus().value());
    }
}
