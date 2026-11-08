package com.erplancers.erp.sales.order.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.sales.pricing.domain.Coupon;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@EntityListeners(OrderLifecycleListener.class)
@NoArgsConstructor @Getter @Setter
@Table(name = "erporder", uniqueConstraints = @UniqueConstraint(name = "uk_erporder_idempotency", columnNames = "idempotency_key"))
public class Order extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY) private Invoice invoice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) private Contact shippingContact;
    private String comment;
    private Long totalAmount;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Currency currency;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "order")
    private Set<OrderLine> lines = new LinkedHashSet<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY) private Set<Coupon> coupons;
    @Enumerated(EnumType.STRING) private OrderStatus orderStatus;
    @Column(name = "idempotency_key", length = 180) private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", length = 64) private String idempotencyPayloadHash;
    @Transient private boolean fulfillmentValidated;
}
