package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name="purchase_receipt", uniqueConstraints=@UniqueConstraint(name="uk_purchase_receipt_idempotency", columnNames="idempotency_key"))
@Getter @Setter @NoArgsConstructor
public class PurchaseReceipt extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Purchase purchase;
    @Column(name="idempotency_key", nullable=false, length=180) private String idempotencyKey;
    @Column(name="payload_hash", nullable=false, length=64) private String payloadHash;
    @Column(nullable=false) private LocalDateTime receivedAt;
    @OneToMany(mappedBy="receipt", cascade=CascadeType.ALL, orphanRemoval=true)
    private Set<PurchaseReceiptLine> lines = new LinkedHashSet<>();
}
