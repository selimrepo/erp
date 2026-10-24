package com.erplancers.erp.crm.contact.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.CascadeType;
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

public class City extends EntityBase {
    String cityName;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Address> addresses;
    
}
