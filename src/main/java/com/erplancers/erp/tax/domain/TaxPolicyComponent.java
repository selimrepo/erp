package com.erplancers.erp.tax.domain;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name="tax_policy_component", indexes=@Index(name="ix_tax_policy_component_policy",columnList="tax_policy_id,sequence_no"))
@Getter @Setter @NoArgsConstructor
public class TaxPolicyComponent extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private TaxPolicy taxPolicy;
    @Column(nullable=false,length=120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private TaxComponentType componentType;
    @Column(nullable=false,precision=12,scale=6) private BigDecimal rate=BigDecimal.ZERO;
    @Column(name="sequence_no",nullable=false) private Integer sequence=0;
    @ManyToOne(fetch=FetchType.LAZY) private Account outputTaxAccount;
    @ManyToOne(fetch=FetchType.LAZY) private Account inputTaxAccount;
}
