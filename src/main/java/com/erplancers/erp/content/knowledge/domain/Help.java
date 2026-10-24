package com.erplancers.erp.content.knowledge.domain;

import com.erplancers.erp.common.domain.EntityBase;

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

public class Help extends EntityBase {
    
    String topic;
    String description;
    @ManyToOne
    HelpCategory category;
}
