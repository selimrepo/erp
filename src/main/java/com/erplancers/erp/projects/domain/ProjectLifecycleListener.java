package com.erplancers.erp.projects.domain;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class ProjectLifecycleListener {
    @PrePersist
    public void validateInsert(Project project) {
        if (!"PLANNED".equals(project.getStatus()))
            throw new IllegalStateException("Projects must start in PLANNED state");
    }

    @PreUpdate
    public void validateUpdate(Project project) {
        String previous = project.getPersistedWorkflowState();
        if (previous != null && !previous.equals(project.getStatus()) && !project.isTransitionValidated())
            throw new IllegalStateException("Project state can only change through project workflow commands");
    }

    @PostUpdate
    public void clearAuthorization(Project project) { project.setTransitionValidated(false); }
}
