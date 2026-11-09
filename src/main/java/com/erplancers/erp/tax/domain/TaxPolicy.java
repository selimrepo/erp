package com.erplancers.erp.tax.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Tenant-configurable tax rule. Null scope fields are deliberate wildcards.
 * Rates are percentages (for example 20.000000 means 20%).
 */
@Entity
@Table(name = "tax_policy", indexes = {
        @Index(name = "ix_tax_policy_lookup", columnList = "active,valid_from,valid_until,priority")
})
@EntityListeners(TaxPolicyLifecycleListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TaxPolicy extends EntityBase {
    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Contact company;

    @Column(length = 32)
    private String jurisdictionCode;

    @Column(length = 40)
    private String transactionType;

    @Column(length = 60)
    private String taxCategory;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private boolean taxInclusive;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private TaxTreatment treatment = TaxTreatment.STANDARD;

    @Column(length=80)
    private String exemptionCode;

    @Column(length=120)
    private String requiredEvidenceType;

    @OneToMany(mappedBy="taxPolicy", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("sequence ASC")
    private List<TaxPolicyComponent> components = new ArrayList<>();
}
