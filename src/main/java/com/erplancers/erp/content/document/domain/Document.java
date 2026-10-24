package com.erplancers.erp.content.document.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.CascadeType;
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

public class Document extends EntityBase {
    String documentName;
    String documentDescription;
    Boolean isTemplate;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<DocumentRevision> revisions;
    @OneToMany
    Set<DocumentLink> documentLinks;
    
}
