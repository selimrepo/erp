package com.erplancers.erp.projects.domain;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class ProjectTaskLifecycleListener {
    @PrePersist
    public void validateInsert(ProjectTask task) {
        if (!"TODO".equals(task.getStatus()))
            throw new IllegalStateException("Project tasks must start in TODO state");
    }

    @PreUpdate
    public void validateUpdate(ProjectTask task) {
        String previous = task.getPersistedWorkflowState();
        if (previous != null && !previous.equals(task.getStatus()) && !task.isTransitionValidated())
            throw new IllegalStateException("Task state can only change through project task workflow commands");
    }

    @PostUpdate
    public void clearAuthorization(ProjectTask task) { task.setTransitionValidated(false); }
}
