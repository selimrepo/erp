package com.erplancers.erp.sales.order.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.sales.pricing.domain.Coupon;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "erporder")

public class Order extends EntityBase {
    
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Invoice invoice;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Customer customer;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact shippingContact;
    String comment;
    Long totalAmount;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Currency currency;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Coupon> coupons;
    @Enumerated(EnumType.STRING)
    OrderStatus orderStatus;


}
