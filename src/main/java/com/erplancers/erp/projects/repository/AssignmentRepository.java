package com.erplancers.erp.projects.repository;

import com.erplancers.erp.projects.domain.Assignment;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;


public interface AssignmentRepository extends ErpRepository<Assignment> {
    List<Assignment> findByAssignmentNameContainingIgnoreCase(String assignmentName);

    List<Assignment> findByFolder_Name(String folderName);

    List<Assignment> findByNote_TopicContainsIgnoreCase(String noteName);

    List<Assignment> findByNote_TextContainsIgnoreCase(String noteNote);
}
