package com.erplancers.erp.accounting.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.accounting.domain.FiscalPeriod;
import com.erplancers.erp.accounting.domain.FiscalYear;
import com.erplancers.erp.accounting.domain.JournalEntry;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface JournalEntryRepository extends ErpRepository<JournalEntry> {
    List<JournalEntry> findByCompany(Contact company);

    List<JournalEntry> findByFiscalPeriod(FiscalPeriod fiscalPeriod);

    List<JournalEntry> findByFiscalYear(FiscalYear fiscalYear);

    List<JournalEntry> findByEntryDateBetween(LocalDate entryDateAfter, LocalDate entryDateBefore);
}
