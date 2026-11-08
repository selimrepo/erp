package com.erplancers.erp.finance.domain;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@EntityListeners(BankStatementLineLifecycleListener.class)
@Table(name="bank_statement_line",
    uniqueConstraints={@UniqueConstraint(name="uk_bank_statement_external_line", columnNames={"statement_id","external_line_id"}),
        @UniqueConstraint(name="uk_bank_statement_matched_payment", columnNames="matched_payment_id")})
@Getter @Setter @NoArgsConstructor
public class BankStatementLine extends EntityBase {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private BankStatement statement;
    @Column(name="external_line_id", nullable=false, length=180) private String externalLineId;
    @Column(nullable=false) private LocalDate transactionDate;
    @Column(nullable=false) private Long amount;
    @Column(length=180) private String reference;
    @Column(length=500) private String description;
    @Column(nullable=false, length=24) private String reconciliationStatus = "UNMATCHED";
    @ManyToOne(fetch=FetchType.LAZY) private PaymentTransaction matchedPayment;
    private LocalDateTime reconciledAt;
    @Transient private boolean reconciliationValidated;
    @Transient private Long persistedMatchedPaymentId;
}
