package com.erplancers.erp.sales.pricing.repository;

import com.erplancers.erp.sales.pricing.domain.Coupon;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface CouponRepository extends ErpRepository<Coupon> {
    List<Coupon> findByCouponCodeContainingIgnoreCase(String couponCode);

    List<Coupon> findByDiscountBetween(Float discountAfter, Float discountBefore);
}
