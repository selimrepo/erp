package com.erplancers.erp.content.knowledge.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class HelpCategory extends EntityBase {
    
    String categoryName;
    String categoryDescription;
    @OneToMany
    Set<Help> helps;
}
