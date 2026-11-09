package com.erplancers.erp.procurement.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import com.erplancers.erp.tax.domain.TaxComponentType;
import com.erplancers.erp.tax.domain.TaxTreatment;
import jakarta.persistence.PreUpdate;
import java.util.Objects;

public class PurchaseDetailLifecycleListener {
    @PostLoad @PostPersist
    public void capture(PurchaseDetail line) {
        line.setPersistedReceivedQuantity(line.getReceivedQuantity());
    }

    @PrePersist @PreUpdate
    public void validate(PurchaseDetail line) {
        if (line.getQuantity() == null || line.getQuantity() <= 0 || line.getPrice() == null || line.getPrice() < 0)
            throw new IllegalStateException("Purchase line quantity must be positive and price non-negative");
        long received = line.getReceivedQuantity() == null ? 0L : line.getReceivedQuantity();
        if (received < 0 || received > line.getQuantity())
            throw new IllegalStateException("Received quantity must be between zero and ordered quantity");
        if (line.getPersistedReceivedQuantity() != null && !Objects.equals(line.getPersistedReceivedQuantity(), line.getReceivedQuantity())
                && !line.isReceiptValidated())
            throw new IllegalStateException("Received quantity can only be changed by the goods-receipt workflow");
        long entered = Math.multiplyExact(line.getQuantity(), line.getPrice());
        long tax = line.getTaxAmount() == null ? 0L : line.getTaxAmount();
        long withholding = line.getWithholdingAmount() == null ? 0L : line.getWithholdingAmount();
        boolean inclusive = line.getTaxPolicy() != null && line.getTaxPolicy().isTaxInclusive();
        long expected = inclusive ? entered - withholding : Math.subtractExact(Math.addExact(entered, tax), withholding);
        if (tax < 0 || withholding < 0 || (inclusive && Math.addExact(tax, withholding) > entered) || expected != line.getLineTotal())
            throw new IllegalStateException("Purchase line total is inconsistent with its tax-inclusive/exclusive policy");
        if (line.getTaxPolicy() != null && line.getTaxPolicy().getTreatment() == TaxTreatment.EXEMPT) {
            if (!java.util.Objects.equals(line.getTaxExemptionCode(), line.getTaxPolicy().getExemptionCode())
                    || !java.util.Objects.equals(line.getTaxEvidenceType(), line.getTaxPolicy().getRequiredEvidenceType())
                    || line.getTaxEvidenceReference() == null || line.getTaxEvidenceReference().isBlank())
                throw new IllegalStateException("Exempt purchase lines require matching exemption code and supporting evidence");
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

    @PostUpdate
    public void captureAfterUpdate(PurchaseDetail line) {
        line.setPersistedReceivedQuantity(line.getReceivedQuantity());
        line.setReceiptValidated(false);
    }

    @jakarta.persistence.PreRemove
    public void preventDelete(PurchaseDetail line) {
        if (line.getPurchase() != null && !"DRAFT".equals(line.getPurchase().getStatus()))
            throw new IllegalStateException("Ordered purchase lines cannot be deleted; use a purchase amendment workflow");
    }
}
