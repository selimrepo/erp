package com.erplancers.erp.finance.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Currency extends EntityBase {
    String title;
    String description;
    String currency;
    Float rate;
    String rateType;
    String symbolLeft;
    String symbolRight;
    
}
