package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(AccountDeterminationRuleLifecycleListener.class)
@Table(name="account_determination_rule",
    uniqueConstraints=@UniqueConstraint(name="uk_account_rule_company_event", columnNames={"company_id","event_type","priority"}))
@Getter @Setter @NoArgsConstructor
public class AccountDeterminationRule extends EntityBase {
    @Column(name="event_type", nullable=false, length=40) private String eventType;
    @ManyToOne(fetch=FetchType.LAZY) private Contact company;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Account debitAccount;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Account creditAccount;
    @ManyToOne(fetch=FetchType.LAZY) private Account taxAccount;
    @Column(nullable=false) private Integer priority = 100;
    @Column(nullable=false) private boolean active = true;
}
