package com.erplancers.erp.crm.contact.domain;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Location extends EntityBase {
    @ManyToOne
    Contact owner;
}
