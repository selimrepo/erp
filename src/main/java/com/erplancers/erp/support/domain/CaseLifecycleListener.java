package com.erplancers.erp.support.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class CaseLifecycleListener {
    @PrePersist @PreUpdate
    public void validate(Case supportCase) {
        if (supportCase.getStatus() == null || supportCase.getStatus().isBlank())
            throw new IllegalStateException("Support case requires a status");
        if (("RESOLVED".equalsIgnoreCase(supportCase.getStatus()) || "CLOSED".equalsIgnoreCase(supportCase.getStatus()))
                && (supportCase.getResolution() == null || supportCase.getResolution().isBlank()))
            throw new IllegalStateException("Resolved or closed support cases require a resolution");
    }
}
