package com.erplancers.erp.inventory.product.repository;

import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.product.domain.ProductQuote;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface ProductQuoteRepository extends ErpRepository<ProductQuote> {
    List<ProductQuote> findByNameContainingIgnoreCase(String name);

    List<ProductQuote> findByDescriptionContainsIgnoreCase(String description);

    List<ProductQuote> findByProductsContaining(Set<Product> products);
}
