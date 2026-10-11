package com.prem.oms.controller;

import com.prem.oms.dto.Dtos.*;
import com.prem.oms.service.InventoryService;
import com.prem.oms.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ProductService productService;
    private final InventoryService inventoryService;

    public CatalogController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    // ----- public -----
    @GetMapping("/products")
    public Page<ProductResponse> products(@RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String q,
                                          @PageableDefault(size = 12, sort = "id") Pageable pageable) {
        return productService.search(categoryId, q, pageable);
    }

    @GetMapping("/products/{id}")
    public ProductResponse product(@PathVariable Long id) {
        return productService.get(id);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return productService.categories();
    }

    // ----- admin -----
    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return productService.createCategory(req);
    }

    @PostMapping("/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest req) {
        return productService.create(req);
    }

    @PutMapping("/admin/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/admin/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deactivate(id);
    }

    @GetMapping("/admin/inventory")
    public List<InventoryResponse> inventory() {
        return inventoryService.list();
    }

    @PutMapping("/admin/inventory/{productId}")
    public InventoryResponse updateStock(@PathVariable Long productId, @Valid @RequestBody StockUpdateRequest req) {
        return inventoryService.setQuantity(productId, req.quantity());
    }
}
