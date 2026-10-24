package com.erplancers.erp.crm.activity.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.projects.domain.Project;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Note extends EntityBase {
    String topic;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Project project;
    String text;
    String fileName;
    
}
