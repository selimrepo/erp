package com.erplancers.erp.projects.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.activity.domain.Note;
import com.erplancers.erp.content.document.domain.Document;
import com.erplancers.erp.content.folder.domain.Folder;
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

public class Assignment extends EntityBase {
    String assignmentName;

    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Folder folder;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Document document;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Project project;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Note note;
    
}
