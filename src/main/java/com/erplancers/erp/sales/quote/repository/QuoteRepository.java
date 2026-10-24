package com.erplancers.erp.sales.quote.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.opportunity.domain.Opportunity;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.common.repository.ErpRepository;
import com.erplancers.erp.sales.domain.Sale;
import com.erplancers.erp.sales.quote.domain.Quote;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface QuoteRepository extends ErpRepository<Quote> {
    List<Quote> findByNameContainingIgnoreCase(String name);

    List<Quote> findByDateBetween(LocalDate dateAfter, LocalDate dateBefore);

    List<Quote> findByContactBilling(Contact contactBilling);

    List<Quote> findByContactShipping(Contact contactShipping);

    List<Quote> findByNumberQuoteBetween(Long numberQuoteAfter, Long numberQuoteBefore);

    List<Quote> findByOpportunitiesContaining(Set<Opportunity> opportunities);

    List<Quote> findByCurrency(Currency currency);

    List<Quote> findBySalesContaining(Set<Sale> sales);

    List<Quote> findByInvoicesContaining(Set<Invoice> invoices);

}
