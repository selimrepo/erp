package com.erplancers.erp.accounting.report;

import com.erplancers.erp.accounting.domain.CostCenter;
import com.erplancers.erp.accounting.domain.JournalEntry;
import com.erplancers.erp.accounting.domain.JournalEntryLine;
import com.erplancers.erp.accounting.report.dto.CostCenterLine;
import com.erplancers.erp.accounting.report.dto.CostCenterReport;
import com.erplancers.erp.accounting.repository.CostCenterRepository;
import com.erplancers.erp.accounting.repository.JournalEntryLineRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class CostCenterReportService {
    final JournalEntryLineRepository journalEntryLineRepository;
    final CostCenterRepository costCenterRepository;
    public List<CostCenterLine> lines(
            Long costCenterId,
            LocalDate from,
            LocalDate to) {
        CostCenter costCenter=costCenterRepository.findById(costCenterId).get();
        return journalEntryLineRepository
                .findByCostCenterAndEntryDateBetween(
                        costCenter,
                        from,
                        to
                )
                .stream()
                .map(line -> new CostCenterLine(
                        line.getJournalEntry().getId(),
                        line.getJournalEntry().getEntryId(),
                        line.getJournalEntry().getEntryDate(),
                        line.getAccount().getId(),
                        line.getAccount().getCode(),
                        line.getAccount().getName(),
                        safe(line.getDebit()),
                        safe(line.getCredit()),
                        safe(line.getDebit())
                                .subtract(safe(line.getCredit())),
                        line.getDescription()
                ))
                .toList();
    }
    public CostCenterReport generate(
            Long costCenterId,
            LocalDate from,
            LocalDate to) {

        CostCenter costCenter =
                costCenterRepository.findById(costCenterId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cost center not found: "
                                                + costCenterId));

        List<JournalEntryLine> source =
                journalEntryLineRepository
                        .findByCostCenterAndEntryDateBetween(
                                costCenter,
                                from,
                                to
                        );

        List<CostCenterLine> lines =
                source.stream()
                        .map(line -> {

                            BigDecimal debit =
                                    safe(line.getDebit());

                            BigDecimal credit =
                                    safe(line.getCredit());

                            return new CostCenterLine(
                                    line.getJournalEntry().getId(),
                                    line.getJournalEntry().getEntryId(),
                                    line.getJournalEntry().getEntryDate(),

                                    line.getAccount().getId(),
                                    line.getAccount().getCode(),
                                    line.getAccount().getName(),

                                    debit,
                                    credit,
                                    debit.subtract(credit),

                                    line.getDescription()
                            );
                        })
                        .toList();

        BigDecimal totalDebit =
                lines.stream()
                        .map(CostCenterLine::debit)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalCredit =
                lines.stream()
                        .map(CostCenterLine::credit)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal balance =
                totalDebit.subtract(totalCredit);

        return new CostCenterReport(
                costCenter.getId(),
                costCenter.getCode(),
                costCenter.getName(),
                from,
                to,
                lines,
                totalDebit,
                totalCredit,
                balance
        );
    }
    private BigDecimal safe(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
}
}
