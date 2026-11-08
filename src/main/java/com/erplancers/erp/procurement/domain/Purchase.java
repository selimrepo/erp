package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.procurement.domain.Supplier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity @NoArgsConstructor @Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_purchase_idempotency", columnNames = "idempotency_key"))
public class Purchase extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Supplier supplier;
    private LocalDateTime purchaseDate;
    private LocalDateTime approvedDate;
    private Long totalAmount;
    @Column(length = 24) private String status;
    @Column(name = "idempotency_key", length = 180) private String idempotencyKey;
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PurchaseDetail> purchaseDetails = new LinkedHashSet<>();
}
