package com.erplancers.erp.support.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class CaseLifecycleListener {
    @PrePersist
    public void validateInsert(Case supportCase) { validate(supportCase); }

    @PreUpdate
    public void validateUpdate(Case supportCase) {
        validate(supportCase);
        String previous = supportCase.getPersistedWorkflowState();
        if (previous != null && !previous.equalsIgnoreCase(supportCase.getStatus()) && !supportCase.isTransitionValidated())
            throw new IllegalStateException("Support case state can only change through support workflow commands");
    }

    @jakarta.persistence.PostUpdate
    public void clearAuthorization(Case supportCase) { supportCase.setTransitionValidated(false); }

    private void validate(Case supportCase) {
        if (supportCase.getStatus() == null || supportCase.getStatus().isBlank())
            throw new IllegalStateException("Support case requires a status");
        if (("RESOLVED".equalsIgnoreCase(supportCase.getStatus()) || "CLOSED".equalsIgnoreCase(supportCase.getStatus()))
                && (supportCase.getResolution() == null || supportCase.getResolution().isBlank()))
            throw new IllegalStateException("Resolved or closed support cases require a resolution");
    }
}
