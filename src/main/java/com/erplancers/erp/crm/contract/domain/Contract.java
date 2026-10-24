package com.erplancers.erp.crm.contract.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.activity.domain.Call;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.finance.domain.Currency;
import jakarta.persistence.*;



import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Contract extends EntityBase {
    String contractName;
    String referenceValue;
    Long totalContractAmount;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Currency currency;
    LocalDateTime customerSignedTime;
    LocalDateTime companySignedTime;
    LocalDateTime dueDate;
    LocalDateTime expiryDate;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Call> calls;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact customer;
    @ManyToOne
    Contact company;
    

}
