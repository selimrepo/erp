package com.erplancers.erp.finance.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTransactionLifecycleListenerTest {
    private final PaymentTransactionLifecycleListener listener = new PaymentTransactionLifecycleListener();

    @Test
    void completedPaymentFinancialFactsCannotBeChanged() {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setStatus("COMPLETED");
        payment.setInvoice(new Invoice());
        payment.setCurrency(new Currency());
        payment.setPaymentTransactionDirection(PaymentTransactionDirection.SALES);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(100L);
        payment.setPayment(100L);
        listener.capture(payment);
        payment.setAmount(120L);
        payment.setPayment(120L);
        assertThrows(IllegalStateException.class, () -> listener.validate(payment));
    }

    @Test
    void completedPaymentRequiresFinancialReferences() {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setStatus("COMPLETED");
        payment.setAmount(100L);
        payment.setPaymentDate(LocalDate.now());
        assertThrows(IllegalStateException.class, () -> listener.validate(payment));
    }
}
