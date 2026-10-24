package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

public class FiscalYear extends EntityBase {


    @ManyToOne(fetch= FetchType.LAZY)
    private Contact company;

    private Integer fiscalYear;

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean closed;


}
