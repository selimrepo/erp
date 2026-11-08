package com.erplancers.erp.sales.quote.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.tax.domain.TaxRate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(QuoteLineLifecycleListener.class)
@Table(name = "erp_quote_line")
@Getter @Setter @NoArgsConstructor
public class QuoteLine extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Quote quote;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Product product;
    @ManyToOne(fetch = FetchType.LAZY) private TaxRate taxRate;
    @Column(nullable = false) private Long quantity;
    @Column(nullable = false) private Long unitPrice;
    @Column(nullable = false) private Long taxAmount = 0L;
    @Column(nullable = false) private Long lineTotal;
    
}
