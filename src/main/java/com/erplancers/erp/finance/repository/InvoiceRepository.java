package com.erplancers.erp.finance.repository;

import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface InvoiceRepository extends ErpRepository<Invoice> {
    List<Invoice> findByInvoiceNameContainingIgnoreCase(String invoiceName);

    List<Invoice> findByCurrency_TitleContainsIgnoreCase(String currencyCode);
}
