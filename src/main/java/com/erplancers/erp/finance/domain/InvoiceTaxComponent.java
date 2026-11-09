package com.erplancers.erp.finance.domain;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.tax.domain.TaxComponentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name="invoice_tax_component")
@Getter @Setter @NoArgsConstructor
public class InvoiceTaxComponent extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Invoice invoice;
    @Column(nullable=false,length=120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private TaxComponentType componentType;
    @Column(nullable=false,precision=12,scale=6) private BigDecimal rate=BigDecimal.ZERO;
    @Column(nullable=false) private Long amount=0L;
    @ManyToOne(fetch=FetchType.LAZY) private Account outputTaxAccount;
    @ManyToOne(fetch=FetchType.LAZY) private Account inputTaxAccount;
}
