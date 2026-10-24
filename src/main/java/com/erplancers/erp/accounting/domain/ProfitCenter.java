package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table
public class ProfitCenter  extends EntityBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    private Contact company;


    private String code;


    private String name;


    private String description;

    @ManyToOne(fetch = FetchType.LAZY)

    private ProfitCenter parent;
    @OneToMany
    Set<ProfitCenter> children;

    private boolean active = true;

    private LocalDate validFrom;

    private LocalDate validTo;
}
