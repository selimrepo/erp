package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table()
@Getter
@Setter

public class FiscalPeriod extends EntityBase {



    @ManyToOne(fetch= FetchType.LAZY)
    private FiscalYear fiscalYear;

    private Integer periodNumber;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean closed;

    private boolean adjustmentPeriod;
}