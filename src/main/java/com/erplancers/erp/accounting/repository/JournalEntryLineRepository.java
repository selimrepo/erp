package com.erplancers.erp.accounting.repository;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.accounting.domain.CostCenter;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.accounting.domain.JournalEntryLine;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;


public interface JournalEntryLineRepository extends ErpRepository<JournalEntryLine> {
    List<JournalEntryLine> findByAccount(Account account);

    List<JournalEntryLine> findByCurrency(Currency currency);

    List<JournalEntryLine> findByAccountIdAndEntryDateBetween(Long accountId, LocalDate from, LocalDate to);

    List<JournalEntryLine> findByEntryDateBetween(LocalDate from, LocalDate to);

    List<JournalEntryLine> findByFiscalYearId(Long fiscalYearId);

    List<JournalEntryLine> findByCostCenterAndEntryDateBetween(CostCenter costCenter, LocalDate entryDateAfter, LocalDate entryDateBefore);
}
