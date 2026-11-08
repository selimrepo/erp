package com.erplancers.erp.finance.domain;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@EntityListeners(BankStatementLifecycleListener.class)
@Table(name="bank_statement", uniqueConstraints=@UniqueConstraint(name="uk_bank_statement_idempotency", columnNames="idempotency_key"))
@Getter @Setter @NoArgsConstructor
public class BankStatement extends EntityBase {
    @Column(nullable=false, length=180) private String idempotencyKey;
    @Column(nullable=false, length=64) private String payloadHash;
    @Column(nullable=false, length=120) private String statementReference;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Currency currency;
    @Column(nullable=false) private LocalDate periodStart;
    @Column(nullable=false) private LocalDate periodEnd;
    @Column(nullable=false, length=24) private String status = "IMPORTED";
    @OneToMany(mappedBy="statement", cascade=CascadeType.ALL, orphanRemoval=true)
    private Set<BankStatementLine> lines = new LinkedHashSet<>();
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean reconciliationValidated;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean importValidated;
}
