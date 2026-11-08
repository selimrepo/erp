package com.erplancers.erp.tax.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class TaxRateLifecycleListener {
    @PrePersist @PreUpdate
    public void validateRate(TaxRate rate) {
        if (!Float.isFinite(rate.getRate()) || rate.getRate() < 0f || rate.getRate() > 100f)
            throw new IllegalArgumentException("Tax rate must be finite and between 0 and 100 percent");
    }
}
