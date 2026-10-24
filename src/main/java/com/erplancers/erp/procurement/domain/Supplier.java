package com.erplancers.erp.procurement.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.inventory.stock.domain.StockItem;
import com.erplancers.erp.inventory.warehouse.domain.Store;
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

public class Supplier extends EntityBase {
    @ManyToOne
    Contact contact;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<StockItem> stockItems;
    String name;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Store> stores;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<StockItem> stockItemsNotInStock;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<StockItem> stockItemsComing;

}
