package com.erplancers.erp.tax.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.customer.domain.CustomerGroup;
import com.erplancers.erp.crm.contact.domain.Location;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@EntityListeners(TaxRateLifecycleListener.class)
@NoArgsConstructor
@Getter
@Setter
@Table

public class TaxRate extends EntityBase {
    
    String name;
    float rate;
    @ManyToOne(fetch = FetchType.LAZY)
    Zone zone;
    @ManyToOne(fetch = FetchType.LAZY)
    Location location;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<CustomerGroup> customerGroups;

}
