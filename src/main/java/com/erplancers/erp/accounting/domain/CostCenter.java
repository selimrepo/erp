package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table
public class CostCenter extends EntityBase {
    @ManyToOne
    Contact company;
    String code;
    String name;
    String description;
    @ManyToOne
    CostCenter parent;
    Boolean active;
    LocalDate validFrom;
    LocalDate validTo;
}
