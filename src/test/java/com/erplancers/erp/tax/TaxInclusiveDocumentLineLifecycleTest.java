package com.erplancers.erp.tax;

import com.erplancers.erp.procurement.domain.PurchaseDetail;
import com.erplancers.erp.procurement.domain.PurchaseDetailLifecycleListener;
import com.erplancers.erp.sales.order.domain.OrderLine;
import com.erplancers.erp.sales.order.domain.OrderLineLifecycleListener;
import com.erplancers.erp.tax.domain.TaxPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxInclusiveDocumentLineLifecycleTest {
    @Test
    void acceptsTaxInclusiveSalesLineWhereUnitPriceIsGross() {
        TaxPolicy policy = inclusivePolicy();
        OrderLine line = new OrderLine();
        line.setQuantity(1L);
        line.setUnitPrice(11_900L);
        line.setLineTotal(11_900L);
        line.setTaxAmount(1_900L);
        line.setTaxPolicy(policy);

        assertThatCode(() -> new OrderLineLifecycleListener().validate(line))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsTaxInclusivePurchaseLineWherePriceIsGross() {
        PurchaseDetail line = new PurchaseDetail();
        line.setQuantity(1L);
        line.setPrice(11_900L);
        line.setLineTotal(11_900L);
        line.setTaxAmount(1_900L);
        line.setTaxPolicy(inclusivePolicy());

        assertThatCode(() -> new PurchaseDetailLifecycleListener().validate(line))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTaxAmountGreaterThanTaxInclusiveGrossAmount() {
        OrderLine line = new OrderLine();
        line.setQuantity(1L);
        line.setUnitPrice(100L);
        line.setLineTotal(100L);
        line.setTaxAmount(101L);
        line.setTaxPolicy(inclusivePolicy());

        assertThatThrownBy(() -> new OrderLineLifecycleListener().validate(line))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tax-inclusive/exclusive policy");
    }

    private TaxPolicy inclusivePolicy() {
        TaxPolicy policy = new TaxPolicy();
        policy.setTaxInclusive(true);
        return policy;
    }
}
