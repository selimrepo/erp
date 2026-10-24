package com.erplancers.erp.common.bi.repository;

import com.erplancers.erp.common.bi.domain.BiReportExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface BiReportExecutionRepository extends JpaRepository<BiReportExecution, String> {
    List<BiReportExecution> findByReportId(String reportId);
    List<BiReportExecution> findTop1ByStatusOrderByCreatedAtAsc(String status);
    long countByStatus(String status);

    @Modifying
    @Transactional
    @Query("update BiReportExecution e set e.status = 'RUNNING', e.startedAt = :now, e.updatedAt = :now where e.id = :id and e.status = 'QUEUED'")
    int claimQueued(@Param("id") String id, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("delete from BiReportExecution e where e.reportId = :reportId")
    int deleteForReport(@Param("reportId") String reportId);
}
