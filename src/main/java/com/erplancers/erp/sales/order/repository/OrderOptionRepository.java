package com.erplancers.erp.sales.order.repository;

import com.erplancers.erp.sales.order.domain.OrderOption;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.product.domain.ProductOption;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface OrderOptionRepository extends ErpRepository<OrderOption> {
    List<OrderOption> findByProduct(Product product);

    List<OrderOption> findByOptionContainsIgnoreCase(String option);

    List<OrderOption> findByProductOptionsContaining(Set<ProductOption> productOptions);

    List<OrderOption> findByPriceBetween(Long priceAfter, Long priceBefore);

    List<OrderOption> findByQuantityBetween(Long quantityAfter, Long quantityBefore);
}
