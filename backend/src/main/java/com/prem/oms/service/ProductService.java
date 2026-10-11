package com.prem.oms.service;

import com.prem.oms.dto.Dtos.*;
import com.prem.oms.entity.Category;
import com.prem.oms.entity.Inventory;
import com.prem.oms.entity.Product;
import com.prem.oms.exception.ApiException;
import com.prem.oms.repository.CategoryRepository;
import com.prem.oms.repository.InventoryRepository;
import com.prem.oms.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(Long categoryId, String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        Page<Product> page = productRepository.search(categoryId, query, pageable);
        List<Long> ids = page.getContent().stream().map(Product::getId).toList();
        Map<Long, Integer> stock = new HashMap<>();
        if (!ids.isEmpty()) {
            inventoryRepository.findByProductIdIn(ids)
                    .forEach(i -> stock.put(i.getProduct().getId(), i.getQuantity()));
        }
        return page.map(p -> toResponse(p, stock.getOrDefault(p.getId(), 0)));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product p = find(id);
        int stock = inventoryRepository.findByProductId(id).map(Inventory::getQuantity).orElse(0);
        return toResponse(p, stock);
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsBySku(req.sku().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU already exists");
        }
        Product p = new Product();
        apply(p, req);
        p = productRepository.save(p);
        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setQuantity(req.initialStock() == null ? 0 : req.initialStock());
        inventoryRepository.save(inv);
        return toResponse(p, inv.getQuantity());
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = find(id);
        if (!p.getSku().equals(req.sku().trim()) && productRepository.existsBySku(req.sku().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU already exists");
        }
        apply(p, req);
        p.setActive(true);
        int stock = inventoryRepository.findByProductId(id).map(Inventory::getQuantity).orElse(0);
        return toResponse(productRepository.save(p), stock);
    }

    /** Soft delete so historical orders keep their product reference. */
    @Transactional
    public void deactivate(Long id) {
        Product p = find(id);
        p.setActive(false);
        productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        return categoryRepository.findAll().stream().map(c -> new CategoryResponse(c.getId(), c.getName())).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Category already exists");
        }
        Category c = new Category();
        c.setName(req.name().trim());
        c = categoryRepository.save(c);
        return new CategoryResponse(c.getId(), c.getName());
    }

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setSku(req.sku().trim());
        p.setImageUrl(req.imageUrl());
        if (req.categoryId() != null) {
            p.setCategory(categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found: " + req.categoryId())));
        } else {
            p.setCategory(null);
        }
    }

    private ProductResponse toResponse(Product p, int stock) {
        Category c = p.getCategory();
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getSku(),
                p.getImageUrl(), p.isActive(), c == null ? null : c.getId(), c == null ? null : c.getName(), stock);
    }
}
