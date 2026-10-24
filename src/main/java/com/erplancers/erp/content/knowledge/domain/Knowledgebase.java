package com.erplancers.erp.content.knowledge.domain;

import com.erplancers.erp.common.domain.EntityBase;

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

public class Knowledgebase extends EntityBase {
    
    String knowledgebaseName;
    String knowledgebaseDescription;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    KnowledgebaseCategory category;
}
