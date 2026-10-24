package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class JournalEntry extends EntityBase {
    @ManyToOne
    Contact company;
    @ManyToOne
    FiscalYear fiscalYear;
    @ManyToOne
    FiscalPeriod fiscalPeriod;
    String entryId;
    LocalDate entryDate;
    String description;
}
