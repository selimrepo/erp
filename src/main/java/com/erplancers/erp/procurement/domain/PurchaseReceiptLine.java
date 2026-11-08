package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(PurchaseReceiptLineLifecycleListener.class)
@Table(name="purchase_receipt_line", uniqueConstraints=@UniqueConstraint(name="uk_purchase_receipt_detail", columnNames={"receipt_id","purchase_detail_id"}))
@Getter @Setter @NoArgsConstructor
public class PurchaseReceiptLine extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private PurchaseReceipt receipt;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private PurchaseDetail purchaseDetail;
    @Column(nullable=false) private Long quantity;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean receiptLineValidated;
}
