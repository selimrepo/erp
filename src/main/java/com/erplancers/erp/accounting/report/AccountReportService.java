package com.erplancers.erp.accounting.report;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.accounting.domain.JournalEntryLine;
import com.erplancers.erp.accounting.repository.AccountRepository;
import com.erplancers.erp.accounting.repository.JournalEntryLineRepository;
import com.erplancers.erp.accounting.report.dto.AccountBalanceReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


@Service
@Transactional(readOnly = true)
public class AccountReportService {

    private final AccountRepository accountRepository;
    private final JournalEntryLineRepository lineRepository;

    public AccountReportService(
            AccountRepository accountRepository,
            JournalEntryLineRepository lineRepository) {

        this.accountRepository = accountRepository;
        this.lineRepository = lineRepository;
    }

    public AccountBalanceReport balance(Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found: " + accountId));

        List<JournalEntryLine> lines =
                lineRepository.findByAccount(account);

        BigDecimal debit = lines.stream()
                .map(JournalEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credit = lines.stream()
                .map(JournalEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AccountBalanceReport(
                account.getId(),
                account.getCode(),
                account.getName(),
                debit,
                credit,
                debit.subtract(credit)
        );
    }
}