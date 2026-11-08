package com.erplancers.erp.support.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@EntityListeners(CaseLifecycleListener.class)
@NoArgsConstructor
@Getter
@Setter
@Table(name = "erpcase")

public class Case extends EntityBase {
    
    String name;
    @Column(length = 24, nullable = false) String status = "OPEN";
    String description;
    String category;
    String type;
    String location;
    String resolution;
    String state;
    String workLog;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean transitionValidated;
}
