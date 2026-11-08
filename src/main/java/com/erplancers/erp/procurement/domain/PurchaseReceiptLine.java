package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="purchase_receipt_line")
@Getter @Setter @NoArgsConstructor
public class PurchaseReceiptLine extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private PurchaseReceipt receipt;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private PurchaseDetail purchaseDetail;
    @Column(nullable=false) private Long quantity;
}
