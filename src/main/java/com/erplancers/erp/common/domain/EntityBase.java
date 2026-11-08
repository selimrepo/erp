package com.erplancers.erp.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import com.erplancers.erp.common.workflow.WorkflowLifecycleListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;



@MappedSuperclass
@EntityListeners(WorkflowLifecycleListener.class)
@NoArgsConstructor
@Getter
@Setter
public class EntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Long id;
    @JsonIgnore
    @Version
    long version;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient
    private String persistedWorkflowState;


    public String getPersistedWorkflowState() { return persistedWorkflowState; }
    public void setPersistedWorkflowState(String state) { this.persistedWorkflowState = state; }
}
