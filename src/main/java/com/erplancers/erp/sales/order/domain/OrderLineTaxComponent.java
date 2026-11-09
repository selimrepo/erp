package com.erplancers.erp.sales.order.domain;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.tax.domain.TaxComponentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name="erp_order_line_tax_component")
@Getter @Setter @NoArgsConstructor
public class OrderLineTaxComponent extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private OrderLine orderLine;
    @Column(nullable=false,length=120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private TaxComponentType componentType;
    @Column(nullable=false,precision=12,scale=6) private BigDecimal rate=BigDecimal.ZERO;
    @Column(nullable=false) private Long amount=0L;
    @ManyToOne(fetch=FetchType.LAZY) private Account outputTaxAccount;
    @ManyToOne(fetch=FetchType.LAZY) private Account inputTaxAccount;
}
