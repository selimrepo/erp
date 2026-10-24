package com.erplancers.erp.accounting.report;

import com.erplancers.erp.accounting.domain.JournalEntryLine;
import com.erplancers.erp.accounting.repository.JournalEntryLineRepository;
import com.erplancers.erp.accounting.report.dto.TrialBalanceLine;
import com.erplancers.erp.accounting.report.dto.TrialBalanceReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TrialBalanceReportService {

    private final JournalEntryLineRepository lineRepository;

    public TrialBalanceReportService(
            JournalEntryLineRepository lineRepository) {

        this.lineRepository = lineRepository;
    }

    public TrialBalanceReport generate(
            LocalDate from,
            LocalDate to) {

        List<JournalEntryLine> lines =
                lineRepository.findByEntryDateBetween(from, to);

        Map<Long, List<JournalEntryLine>> grouped =
                lines.stream()
                        .collect(Collectors.groupingBy(
                                line -> line.getAccount().getId()
                        ));

        List<TrialBalanceLine> result =
                grouped.values()
                        .stream()
                        .map(this::createLine)
                        .sorted((a, b) ->
                                a.accountCode()
                                        .compareTo(b.accountCode()))
                        .toList();

        BigDecimal totalDebit = result.stream()
                .map(TrialBalanceLine::debit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = result.stream()
                .map(TrialBalanceLine::credit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TrialBalanceReport(
                from,
                to,
                result,
                totalDebit,
                totalCredit
        );
    }

    private TrialBalanceLine createLine(
            List<JournalEntryLine> lines) {

        JournalEntryLine first = lines.getFirst();

        BigDecimal debit = lines.stream()
                .map(JournalEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credit = lines.stream()
                .map(JournalEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TrialBalanceLine(
                first.getAccount().getId(),
                first.getAccount().getCode(),
                first.getAccount().getName(),
                debit,
                credit,
                debit.subtract(credit)
        );
    }
}