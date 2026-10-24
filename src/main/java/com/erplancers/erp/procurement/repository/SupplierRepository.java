package com.erplancers.erp.procurement.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.inventory.stock.domain.StockItem;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.procurement.domain.Supplier;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface SupplierRepository extends ErpRepository<Supplier> {
    List<Supplier> findByContact(Contact contact);

    List<Supplier> findByStockItemsContaining(Set<StockItem> stockItems);

    List<Supplier> findByStoresContaining(Set<Store> stores);

    List<Supplier> findByStockItemsNotInStockContaining(Set<StockItem> stockItemsNotInStock);

    List<Supplier> findByStockItemsComingContaining(Set<StockItem> stockItemsComing);
}
