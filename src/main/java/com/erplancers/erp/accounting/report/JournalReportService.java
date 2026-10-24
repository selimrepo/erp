package com.erplancers.erp.accounting.report;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.accounting.domain.JournalEntryLine;
import com.erplancers.erp.accounting.repository.JournalEntryLineRepository;
import com.erplancers.erp.accounting.report.dto.GeneralLedgerLine;
import com.erplancers.erp.accounting.report.dto.GeneralLedgerReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional(readOnly = true)
public class JournalReportService {

    private final JournalEntryLineRepository lineRepository;

    public JournalReportService(
            JournalEntryLineRepository lineRepository) {

        this.lineRepository = lineRepository;
    }

    public GeneralLedgerReport generalLedger(
            Long accountId,
            LocalDate from,
            LocalDate to) {

        List<JournalEntryLine> lines =
                lineRepository.findByAccountIdAndEntryDateBetween(
                        accountId,
                        from,
                        to
                );

        BigDecimal balance = BigDecimal.ZERO;

        List<GeneralLedgerLine> result = new ArrayList<>();

        for (JournalEntryLine line : lines) {

            BigDecimal debit = value(line.getDebit());
            BigDecimal credit = value(line.getCredit());

            balance = balance
                    .add(debit)
                    .subtract(credit);

            result.add(new GeneralLedgerLine(
                    line.getJournalEntry().getId(),
                    line.getJournalEntry().getEntryId(),
                    line.getJournalEntry().getEntryDate(),
                    line.getDescription(),
                    debit,
                    credit,
                    balance
            ));
        }

        Account account = lines.isEmpty()
                ? null
                : lines.getFirst().getAccount();

        return new GeneralLedgerReport(
                accountId,
                account != null ? account.getCode() : null,
                account != null ? account.getName() : null,
                from,
                to,
                result
        );
    }

    private BigDecimal value(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}