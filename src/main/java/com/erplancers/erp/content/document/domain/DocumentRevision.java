package com.erplancers.erp.content.document.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class DocumentRevision extends EntityBase {
    String changeLog;
    String documentType;
    String fileName;
    String filePath;
    String fileSize;
    String fileExtension;
    String mimeType;
    Long revisionNumber;
    
}
