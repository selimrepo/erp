package com.erplancers.erp.common.workflow;

import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.support.domain.Case;
import com.erplancers.erp.tax.domain.TaxRate;
import com.erplancers.erp.tax.domain.TaxRateLifecycleListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowLifecycleListenerTest {
    private final WorkflowLifecycleListener listener = new WorkflowLifecycleListener();

    @Test
    void allowsSupportCaseProgression() {
        Case supportCase = new Case();
        supportCase.setStatus("OPEN");
        listener.validateInitialState(supportCase);
        listener.capturePersistedState(supportCase);
        supportCase.setStatus("IN_PROGRESS");
        assertDoesNotThrow(() -> listener.validateTransition(supportCase));
    }

    @Test
    void rejectsIllegalSupportCaseTransition() {
        Case supportCase = new Case();
        supportCase.setStatus("OPEN");
        listener.validateInitialState(supportCase);
        listener.capturePersistedState(supportCase);
        supportCase.setStatus("CLOSED");
        assertThrows(IllegalStateException.class, () -> listener.validateTransition(supportCase));
    }

    @Test
    void allowsInvoiceToMoveFromConfirmedToPosted() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceStatus(com.erplancers.erp.finance.domain.InvoiceStatus.CONFIRMED);
        listener.validateInitialState(invoice);
        listener.capturePersistedState(invoice);
        invoice.setInvoiceStatus(com.erplancers.erp.finance.domain.InvoiceStatus.POSTED);
        assertDoesNotThrow(() -> listener.validateTransition(invoice));
    }

    @Test
    void taxRateMustBeWithinPercentageRange() {
        TaxRateLifecycleListener taxListener = new TaxRateLifecycleListener();
        TaxRate rate = new TaxRate();
        rate.setRate(20.0f);
        assertDoesNotThrow(() -> taxListener.validateRate(rate));
        rate.setRate(101.0f);
        assertThrows(IllegalArgumentException.class, () -> taxListener.validateRate(rate));
    }
}
