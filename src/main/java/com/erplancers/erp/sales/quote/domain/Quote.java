package com.erplancers.erp.sales.quote.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.sales.domain.Sale;
import com.erplancers.erp.sales.order.domain.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@EntityListeners(QuoteLifecycleListener.class)
@NoArgsConstructor
@Getter
@Setter
@Table
public class Quote extends EntityBase {
    private String name;

    @Column(length = 24, nullable = false)
    private String status = "DRAFT";

    private LocalDate validUntil;

    @Column(length = 180, unique = true)
    private String conversionIdempotencyKey;

    @Column(length = 64)
    private String conversionPayloadHash;

    @ManyToOne(fetch = FetchType.LAZY)
    private Order convertedOrder;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<QuoteLine> lines = new LinkedHashSet<>();

    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    private Contact contactBilling;

    @ManyToOne(fetch = FetchType.LAZY)
    private Contact contactShipping;

    private Long numberQuote;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Opportunity> opportunities;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Sale> sales;

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency currency;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Invoice> invoices;

    @Transient
    private boolean conversionValidated;

    @Transient
    private boolean transitionValidated;
}
