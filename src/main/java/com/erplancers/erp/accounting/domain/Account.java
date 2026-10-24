package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.finance.domain.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Account extends EntityBase {

    @ManyToOne
    Account accountParent;
    @ManyToOne
    Contact contact;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Account> accountChildren;
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    private BalanceType balanceType;

    @ManyToOne(fetch = FetchType.LAZY)
    private Currency currency;
    private boolean postingAllowed;

    private boolean reconciliationAccount;

    private boolean blocked;

    private Integer level;
    String name;
    String code;

    private Integer sortOrder;


}
