package com.erplancers.erp.crm.opportunity.domain;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class OpportunityLifecycleListener {
    @PrePersist
    public void validateInsert(Opportunity opportunity) {
        if (!"NEW".equals(opportunity.getStatus()))
            throw new IllegalStateException("Opportunities must start in NEW state");
    }

    @PreUpdate
    public void validateUpdate(Opportunity opportunity) {
        String previous = opportunity.getPersistedWorkflowState();
        if (previous != null && !previous.equals(opportunity.getStatus()) && !opportunity.isTransitionValidated())
            throw new IllegalStateException("Opportunity stages can only change through CRM workflow commands");
    }

    @PostUpdate
    public void clearAuthorization(Opportunity opportunity) { opportunity.setTransitionValidated(false); }
}
