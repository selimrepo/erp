package com.erplancers.erp.projects.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@EntityListeners(ProjectTaskLifecycleListener.class)
@NoArgsConstructor
@Getter
@Setter
@Table

public class ProjectTask extends EntityBase {
    
    String name;
    @Column(length = 24, nullable = false) private String status = "TODO";
    String description;
    @Transient private boolean transitionValidated;
}
