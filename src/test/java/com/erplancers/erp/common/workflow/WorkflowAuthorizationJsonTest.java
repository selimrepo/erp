package com.erplancers.erp.common.workflow;

import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.sales.order.domain.Order;
import com.erplancers.erp.sales.quote.domain.Quote;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowAuthorizationJsonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void clientsCannotSetSalesOrderWorkflowAuthorizationFlags() throws Exception {
        Order order = mapper.readValue("""
                {
                  "orderCreationValidated": true,
                  "fulfillmentValidated": true,
                  "persistedWorkflowState": "PROCESSING"
                }
                """, Order.class);

        assertFalse(order.isOrderCreationValidated());
        assertFalse(order.isFulfillmentValidated());
        assertNull(order.getPersistedWorkflowState());

        String json = mapper.writeValueAsString(order);
        assertFalse(json.contains("orderCreationValidated"));
        assertFalse(json.contains("fulfillmentValidated"));
        assertFalse(json.contains("persistedWorkflowState"));
    }

    @Test
    void clientsCannotSetInvoiceOrQuoteWorkflowAuthorizationFlags() throws Exception {
        Invoice invoice = mapper.readValue("""
                {
                  "creationValidated": true,
                  "settlementValidated": true,
                  "ledgerPostingValidated": true,
                  "persistedAmountFingerprint": "forged"
                }
                """, Invoice.class);
        Quote quote = mapper.readValue("""
                {
                  "conversionValidated": true,
                  "transitionValidated": true
                }
                """, Quote.class);

        assertFalse(invoice.isCreationValidated());
        assertFalse(invoice.isSettlementValidated());
        assertFalse(invoice.isLedgerPostingValidated());
        assertNull(invoice.getPersistedAmountFingerprint());
        assertFalse(quote.isConversionValidated());
        assertFalse(quote.isTransitionValidated());
    }
}
