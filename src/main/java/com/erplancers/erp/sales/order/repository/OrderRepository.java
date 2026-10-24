package com.erplancers.erp.sales.order.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.common.repository.ErpRepository;
import com.erplancers.erp.sales.order.domain.Order;
import com.erplancers.erp.sales.pricing.domain.Coupon;
import org.aspectj.weaver.ast.Or;

import java.util.List;
import java.util.Set;

public interface OrderRepository extends ErpRepository<Order> {
    List<Order> findByCurrency(Currency currency);

    List<Order> findByInvoice(Invoice invoice);

    List<Order> findByCustomer(Customer customer);

    List<Or> findByCouponsContaining(Set<Coupon> coupons);

    List<Order> findAllByShippingContact(Contact shippingContact);

    List<Order> findAllByTotalAmountBetween(Long totalAmountAfter, Long totalAmountBefore);
}
