package com.erplancers.erp.inventory.warehouse.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.inventory.product.domain.Manufacturer;
import com.erplancers.erp.procurement.domain.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
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

public class Store extends EntityBase {
    @ManyToOne
    Contact contact;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Manufacturer> manufacturers;
    @OneToMany
    Set<Supplier> suppliers;
}
