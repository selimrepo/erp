package com.erplancers.erp.crm.activity.repository;

import com.erplancers.erp.crm.activity.domain.Note;
import com.erplancers.erp.projects.domain.Project;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface NoteRepository extends ErpRepository<Note> {
    List<Note> findByTopicContainingIgnoreCase(String topic);

    List<Note> findByTextContainingIgnoreCase(String text);

    List<Note> findByProject(Project project);
}
