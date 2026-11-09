package com.erplancers.erp.accounting;

import com.erplancers.erp.accounting.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
class JournalPostingIntegrationTest {
    @Autowired EntityManager entityManager;

    @Test
    void rejectsUnbalancedJournalEvenWhenPostingCommandWasInvoked() {
        JournalEntry journal = draftJournal();
        journal.getLines().add(line(journal, account("1000"), "100.00", "0"));
        journal.getLines().add(line(journal, account("4000"), "0", "90.00"));
        entityManager.persist(journal);
        entityManager.flush();

        journal.setPostingValidated(true);
        journal.setStatus(JournalEntryStatus.POSTED);

        assertThatThrownBy(() -> entityManager.flush())
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Journal debits and credits must be equal and greater than zero");
    }

    @Test
    void acceptsBalancedJournalInOpenFiscalPeriod() {
        JournalEntry journal = draftJournal();
        journal.getLines().add(line(journal, account("1001"), "100.00", "0"));
        journal.getLines().add(line(journal, account("4001"), "0", "100.00"));
        entityManager.persist(journal);
        entityManager.flush();

        journal.setPostingValidated(true);
        journal.setStatus(JournalEntryStatus.POSTED);

        assertThatCode(() -> entityManager.flush()).doesNotThrowAnyException();
    }

    private JournalEntry draftJournal() {
        LocalDate date = LocalDate.of(2026, 11, 9);
        FiscalYear year = new FiscalYear();
        year.setFiscalYear(2026);
        year.setStartDate(LocalDate.of(2026, 1, 1));
        year.setEndDate(LocalDate.of(2026, 12, 31));
        entityManager.persist(year);

        FiscalPeriod period = new FiscalPeriod();
        period.setFiscalYear(year);
        period.setPeriodNumber(11);
        period.setName("November 2026");
        period.setStartDate(LocalDate.of(2026, 11, 1));
        period.setEndDate(LocalDate.of(2026, 11, 30));
        entityManager.persist(period);

        JournalEntry journal = new JournalEntry();
        journal.setEntryId("IT-" + System.nanoTime());
        journal.setEntryDate(date);
        journal.setDescription("Integration test journal");
        journal.setFiscalYear(year);
        journal.setFiscalPeriod(period);
        journal.setStatus(JournalEntryStatus.DRAFT);
        journal.setIdempotencyKey("it-" + System.nanoTime());
        journal.setIdempotencyPayloadHash("a".repeat(64));
        return journal;
    }

    private Account account(String code) {
        Account account = new Account();
        account.setCode(code);
        account.setName("Test account " + code);
        account.setPostingAllowed(true);
        account.setBlocked(false);
        entityManager.persist(account);
        return account;
    }

    private JournalEntryLine line(JournalEntry journal, Account account, String debit, String credit) {
        JournalEntryLine line = new JournalEntryLine();
        line.setJournalEntry(journal);
        line.setAccount(account);
        line.setDebit(new BigDecimal(debit));
        line.setCredit(new BigDecimal(credit));
        return line;
    }
}
