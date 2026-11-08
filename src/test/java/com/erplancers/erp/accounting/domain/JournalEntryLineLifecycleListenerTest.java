package com.erplancers.erp.accounting.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class JournalEntryLineLifecycleListenerTest {
    private final JournalEntryLineLifecycleListener listener = new JournalEntryLineLifecycleListener();

    @Test
    void permitsLinesOnlyWhileJournalIsDraft() {
        JournalEntry journal = new JournalEntry();
        journal.setStatus(JournalEntryStatus.DRAFT);
        JournalEntryLine line = new JournalEntryLine();
        line.setJournalEntry(journal);
        line.setAccount(new Account());
        line.setDebit(BigDecimal.TEN);
        line.setCredit(BigDecimal.ZERO);
        assertDoesNotThrow(() -> listener.validateInsert(line));

        journal.setStatus(JournalEntryStatus.POSTED);
        assertThrows(IllegalStateException.class, () -> listener.preventUpdate(line));
        assertThrows(IllegalStateException.class, () -> listener.preventDelete(line));
    }

    @Test
    void rejectsEmptyOrTwoSidedJournalLine() {
        JournalEntry journal = new JournalEntry();
        journal.setStatus(JournalEntryStatus.DRAFT);
        JournalEntryLine line = new JournalEntryLine();
        line.setJournalEntry(journal);
        line.setAccount(new Account());
        line.setDebit(BigDecimal.ONE);
        line.setCredit(BigDecimal.ONE);
        assertThrows(IllegalStateException.class, () -> listener.validateInsert(line));
    }
}
