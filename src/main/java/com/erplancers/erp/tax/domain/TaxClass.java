package com.erplancers.erp.tax.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class TaxClass extends EntityBase {
    
    String title;
    @OneToMany
    Set<TaxRate> taxRates;
}
