package com.erplancers.erp.accounting.report;

import com.erplancers.erp.accounting.domain.FiscalPeriod;
import com.erplancers.erp.accounting.domain.FiscalYear;
import com.erplancers.erp.accounting.domain.JournalEntryLine;
import com.erplancers.erp.accounting.repository.FiscalPeriodRepository;
import com.erplancers.erp.accounting.repository.FiscalYearRepository;
import com.erplancers.erp.accounting.repository.JournalEntryLineRepository;
import com.erplancers.erp.accounting.report.dto.AccountClosingBalance;
import com.erplancers.erp.accounting.report.dto.AccountFiscalSummary;
import com.erplancers.erp.accounting.report.dto.AccountOpeningBalance;
import com.erplancers.erp.accounting.report.dto.FiscalPeriodSummary;
import com.erplancers.erp.accounting.report.dto.PeriodComparison;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class FiscalReportService {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public FiscalReportService(
            FiscalYearRepository fiscalYearRepository,
            FiscalPeriodRepository fiscalPeriodRepository,
            JournalEntryLineRepository journalEntryLineRepository) {

        this.fiscalYearRepository = fiscalYearRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    /*
     * ---------------------------------------------------------
     * ACCOUNT SUMMARY
     * ---------------------------------------------------------
     */

    public List<AccountFiscalSummary> accountSummary(
            Long fiscalYearId) {

        return journalEntryLineRepository
                .findByFiscalYearId(fiscalYearId)
                .stream()
                .collect(Collectors.groupingBy(
                        line -> line.getAccount().getId()
                ))
                .values()
                .stream()
                .map(this::toAccountSummary)
                .sorted((a, b) ->
                        a.accountCode()
                                .compareTo(b.accountCode()))
                .toList();
    }

    private AccountFiscalSummary toAccountSummary(
            List<JournalEntryLine> lines) {

        JournalEntryLine first = lines.getFirst();

        BigDecimal debit = lines.stream()
                .map(JournalEntryLine::getDebit)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credit = lines.stream()
                .map(JournalEntryLine::getCredit)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AccountFiscalSummary(
                first.getAccount().getId(),
                first.getAccount().getCode(),
                first.getAccount().getName(),
                debit,
                credit,
                debit.subtract(credit)
        );
    }

    /*
     * ---------------------------------------------------------
     * PERIOD SUMMARY
     * ---------------------------------------------------------
     */

    public List<FiscalPeriodSummary> periodSummary(
            Long fiscalYearId) {

        return journalEntryLineRepository
                .findByFiscalYearId(fiscalYearId)
                .stream()
                .collect(Collectors.groupingBy(
                        line ->
                                line.getJournalEntry()
                                        .getFiscalPeriod()
                                        .getId()
                ))
                .entrySet()
                .stream()
                .map(entry -> {

                    BigDecimal debit = entry.getValue()
                            .stream()
                            .map(JournalEntryLine::getDebit)
                            .map(this::safe)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

                    BigDecimal credit = entry.getValue()
                            .stream()
                            .map(JournalEntryLine::getCredit)
                            .map(this::safe)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

                    return new FiscalPeriodSummary(
                            entry.getKey(),
                            debit,
                            credit,
                            debit.subtract(credit)
                    );
                })
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * OPENING BALANCES
     * ---------------------------------------------------------
     */

    public List<AccountOpeningBalance> openingBalances(
            Long fiscalYearId,
            LocalDate openingDate) {

        return journalEntryLineRepository
                .findByFiscalYearId(fiscalYearId)
                .stream()
                .filter(line ->
                        line.getJournalEntry()
                                .getEntryDate()
                                .isBefore(openingDate)
                )
                .collect(Collectors.groupingBy(
                        line -> line.getAccount().getId()
                ))
                .values()
                .stream()
                .map(lines -> {

                    JournalEntryLine first =
                            lines.getFirst();

                    BigDecimal debit = lines.stream()
                            .map(JournalEntryLine::getDebit)
                            .map(this::safe)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

                    BigDecimal credit = lines.stream()
                            .map(JournalEntryLine::getCredit)
                            .map(this::safe)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

                    return new AccountOpeningBalance(
                            first.getAccount().getId(),
                            first.getAccount().getCode(),
                            first.getAccount().getName(),
                            debit,
                            credit,
                            debit.subtract(credit)
                    );
                })
                .sorted((a, b) ->
                        a.accountCode()
                                .compareTo(b.accountCode()))
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * CLOSING BALANCES
     * ---------------------------------------------------------
     */

    public List<AccountClosingBalance> closingBalances(
            Long fiscalYearId,
            LocalDate closingDate) {

        return journalEntryLineRepository
                .findByFiscalYearId(fiscalYearId)
                .stream()
                .filter(line ->
                        !line.getJournalEntry()
                                .getEntryDate()
                                .isAfter(closingDate)
                )
                .collect(Collectors.groupingBy(
                        line -> line.getAccount().getId()
                ))
                .values()
                .stream()
                .map(lines -> {

                    JournalEntryLine first =
                            lines.getFirst();

                    BigDecimal debit = lines.stream()
                            .map(JournalEntryLine::getDebit)
                            .map(this::safe)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

                    BigDecimal credit = lines.stream()
                            .map(JournalEntryLine::getCredit)
                            .map(this::safe)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

                    return new AccountClosingBalance(
                            first.getAccount().getId(),
                            first.getAccount().getCode(),
                            first.getAccount().getName(),
                            debit,
                            credit,
                            debit.subtract(credit)
                    );
                })
                .sorted((a, b) ->
                        a.accountCode()
                                .compareTo(b.accountCode()))
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * PERIOD COMPARISON
     * ---------------------------------------------------------
     */

    public List<PeriodComparison> comparePeriods(
            Long firstPeriodId,
            Long secondPeriodId) {

        List<JournalEntryLine> first =
                journalEntryLineRepository
                        .findAll().stream().filter(i->i.getFiscalPeriod().getId()==firstPeriodId).toList();

        List<JournalEntryLine> second =
                journalEntryLineRepository
                        .findAll().stream().filter(i->i.getFiscalPeriod().getId()==secondPeriodId).toList();

        Map<Long, List<JournalEntryLine>> firstMap =
                first.stream()
                        .collect(Collectors.groupingBy(
                                line -> line.getAccount().getId()
                        ));

        Map<Long, List<JournalEntryLine>> secondMap =
                second.stream()
                        .collect(Collectors.groupingBy(
                                line -> line.getAccount().getId()
                        ));

        return Stream.concat(
                        firstMap.keySet().stream(),
                        secondMap.keySet().stream()
                )
                .distinct()
                .map(accountId -> {

                    List<JournalEntryLine> firstLines =
                            firstMap.getOrDefault(
                                    accountId,
                                    List.of()
                            );

                    List<JournalEntryLine> secondLines =
                            secondMap.getOrDefault(
                                    accountId,
                                    List.of()
                            );

                    JournalEntryLine accountLine =
                            Stream.concat(
                                            firstLines.stream(),
                                            secondLines.stream()
                                    )
                                    .findFirst()
                                    .orElseThrow();

                    BigDecimal firstDebit =
                            debit(firstLines);

                    BigDecimal firstCredit =
                            credit(firstLines);

                    BigDecimal secondDebit =
                            debit(secondLines);

                    BigDecimal secondCredit =
                            credit(secondLines);

                    BigDecimal firstBalance =
                            firstDebit.subtract(firstCredit);

                    BigDecimal secondBalance =
                            secondDebit.subtract(secondCredit);

                    return new PeriodComparison(
                            accountId,
                            accountLine.getAccount().getCode(),
                            accountLine.getAccount().getName(),

                            firstDebit,
                            firstCredit,
                            firstBalance,

                            secondDebit,
                            secondCredit,
                            secondBalance,

                            secondDebit.subtract(firstDebit),
                            secondCredit.subtract(firstCredit),
                            secondBalance.subtract(firstBalance)
                    );
                })
                .sorted((a, b) ->
                        a.accountCode()
                                .compareTo(b.accountCode()))
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * HELPERS
     * ---------------------------------------------------------
     */

    private BigDecimal debit(
            List<JournalEntryLine> lines) {

        return lines.stream()
                .map(JournalEntryLine::getDebit)
                .map(this::safe)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private BigDecimal credit(
            List<JournalEntryLine> lines) {

        return lines.stream()
                .map(JournalEntryLine::getCredit)
                .map(this::safe)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}