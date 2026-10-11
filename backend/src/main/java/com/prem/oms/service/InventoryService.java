package com.prem.oms.service;

import com.prem.oms.dto.Dtos.InventoryResponse;
import com.prem.oms.entity.Inventory;
import com.prem.oms.exception.ApiException;
import com.prem.oms.repository.InventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> list() {
        return inventoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public InventoryResponse setQuantity(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Inventory not found for product " + productId));
        inv.setQuantity(quantity);
        return toResponse(inventoryRepository.save(inv));
    }

    private InventoryResponse toResponse(Inventory i) {
        return new InventoryResponse(i.getProduct().getId(), i.getProduct().getName(),
                i.getProduct().getSku(), i.getQuantity());
    }
}
