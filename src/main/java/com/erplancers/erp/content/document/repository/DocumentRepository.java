package com.erplancers.erp.content.document.repository;

import com.erplancers.erp.content.document.domain.Document;
import com.erplancers.erp.common.repository.ErpRepository;

import javax.print.Doc;
import java.util.List;

public interface DocumentRepository extends ErpRepository<Document> {
    List<Document> findByDocumentNameContainingIgnoreCase(String documentName);

    List<Doc> findByIsTemplate(Boolean isTemplate);
}
