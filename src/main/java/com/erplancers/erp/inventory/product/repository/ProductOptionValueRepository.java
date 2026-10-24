package com.erplancers.erp.inventory.product.repository;

import com.erplancers.erp.inventory.product.domain.ProductOptionValue;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface ProductOptionValueRepository extends ErpRepository<ProductOptionValue> {
    List<ProductOptionValue> findByAttributeNameContainingIgnoreCase(String attributeName);

    List<ProductOptionValue> findByPriceChangeBetween(Long priceChangeAfter, Long priceChangeBefore);

    List<ProductOptionValue> findByBuyPriceBetween(Long buyPriceAfter, Long buyPriceBefore);

    List<ProductOptionValue> findBySellPriceBetween(Long sellPriceAfter, Long sellPriceBefore);

    List<ProductOptionValue> findByWidthBetween(Long widthAfter, Long widthBefore);

    List<ProductOptionValue> findByHeightBetween(Long heightAfter, Long heightBefore);

    List<ProductOptionValue> findByLengthBetween(Long lengthAfter, Long lengthBefore);
}
