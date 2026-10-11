package com.prem.oms.repository;

import com.prem.oms.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);

    @Query("""
           select p from Product p
           where p.active = true
             and (:categoryId is null or p.category.id = :categoryId)
             and (:q is null or lower(p.name) like lower(concat('%', :q, '%')))
           """)
    Page<Product> search(@Param("categoryId") Long categoryId, @Param("q") String q, Pageable pageable);
}
