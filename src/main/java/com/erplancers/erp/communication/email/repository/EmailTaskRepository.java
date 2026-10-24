package com.erplancers.erp.communication.email.repository;

import com.erplancers.erp.communication.email.domain.EmailTask;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailTaskRepository extends ErpRepository<EmailTask> {


    List<EmailTask> findByInQueue(Boolean inQueue);

    List<EmailTask> findByDeadlineBetween(LocalDateTime deadlineAfter, LocalDateTime deadlineBefore);
}
