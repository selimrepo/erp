package com.erplancers.erp.inventory.warehouse.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.inventory.product.domain.Manufacturer;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.procurement.domain.Supplier;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface StoreRepository extends ErpRepository<Store> {
    List<Store> findByContact(Contact contact);

    List<Store> findByManufacturers(Set<Manufacturer> manufacturers);

    List<Store> findBySuppliersContaining(Set<Supplier> suppliers);
}
