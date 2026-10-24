package com.erplancers.erp.content.knowledge.repository;

import com.erplancers.erp.content.knowledge.domain.Knowledgebase;
import com.erplancers.erp.content.knowledge.domain.KnowledgebaseCategory;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface KnowledgebaseCategoryRepository extends ErpRepository<KnowledgebaseCategory> {
    List<KnowledgebaseCategory> findByKnowledgeBasesContaining(Set<Knowledgebase> knowledgeBases);

    List<KnowledgebaseCategory> findByNameContainsIgnoreCase(String name);
}
