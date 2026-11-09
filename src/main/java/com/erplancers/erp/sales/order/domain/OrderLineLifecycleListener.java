package com.erplancers.erp.sales.order.domain;

import jakarta.persistence.PrePersist;
import com.erplancers.erp.tax.domain.TaxComponentType;
import com.erplancers.erp.tax.domain.TaxTreatment;
import jakarta.persistence.PreUpdate;

public class OrderLineLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(OrderLine line) {
        if (line.getQuantity() == null || line.getQuantity() <= 0 || line.getUnitPrice() == null || line.getUnitPrice() < 0)
            throw new IllegalStateException("Order line quantity must be positive and unit price non-negative");
        long entered = Math.multiplyExact(line.getQuantity(), line.getUnitPrice());
        long tax = line.getTaxAmount() == null ? 0L : line.getTaxAmount();
        long withholding = line.getWithholdingAmount() == null ? 0L : line.getWithholdingAmount();
        boolean inclusive = line.getTaxPolicy() != null && line.getTaxPolicy().isTaxInclusive();
        long expected = inclusive ? entered - withholding : Math.subtractExact(Math.addExact(entered, tax), withholding);
        if (tax < 0 || withholding < 0 || (inclusive && Math.addExact(tax, withholding) > entered) || expected != line.getLineTotal())
            throw new IllegalStateException("Order line total is inconsistent with its tax-inclusive/exclusive policy");
        if (line.getTaxPolicy() != null && line.getTaxPolicy().getTreatment() == TaxTreatment.EXEMPT) {
            if (!java.util.Objects.equals(line.getTaxExemptionCode(), line.getTaxPolicy().getExemptionCode())
                    || !java.util.Objects.equals(line.getTaxEvidenceType(), line.getTaxPolicy().getRequiredEvidenceType())
                    || line.getTaxEvidenceReference() == null || line.getTaxEvidenceReference().isBlank())
                throw new IllegalStateException("Exempt order lines require matching exemption code and supporting evidence");
        }
        if (line.getTaxComponents() != null && !line.getTaxComponents().isEmpty()) {
            long componentTax = line.getTaxComponents().stream()
                    .filter(c -> c.getComponentType() == TaxComponentType.STANDARD || c.getComponentType() == TaxComponentType.COMPOUND)
                    .mapToLong(c -> c.getAmount() == null ? 0L : c.getAmount()).sum();
            long componentWithholding = line.getTaxComponents().stream()
                    .filter(c -> c.getComponentType() == TaxComponentType.WITHHOLDING)
                    .mapToLong(c -> c.getAmount() == null ? 0L : c.getAmount()).sum();
            long componentReverse = line.getTaxComponents().stream()
                    .filter(c -> c.getComponentType() == TaxComponentType.REVERSE_CHARGE)
                    .mapToLong(c -> c.getAmount() == null ? 0L : c.getAmount()).sum();
            if (componentTax != tax || componentWithholding != withholding || componentReverse != (line.getReverseChargeTaxAmount() == null ? 0L : line.getReverseChargeTaxAmount()))
                throw new IllegalStateException("Document line tax component totals do not match the stored tax amounts");
        }
    }

    @jakarta.persistence.PreRemove
    public void preventDelete(OrderLine line) {
        throw new IllegalStateException("Reserved sales order lines cannot be deleted; cancel the order instead");
    }
}
