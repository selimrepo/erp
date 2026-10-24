package com.erplancers.erp.common.bi.repository;

import com.erplancers.erp.common.bi.domain.BiSavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface BiSavedReportRepository extends JpaRepository<BiSavedReport, String> {
    List<BiSavedReport> findTop100ByEnabledTrueAndScheduleIntervalMinutesIsNotNullAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant now);

    @Modifying
    @Transactional
    @Query("update BiSavedReport r set r.nextRunAt = :next, r.updatedAt = :now where r.id = :id and r.nextRunAt = :expected and r.enabled = true")
    int claimSchedule(@Param("id") String id, @Param("expected") Instant expected,
                      @Param("next") Instant next, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("update BiSavedReport r set r.lastRunAt = :now, r.updatedAt = :now where r.id = :id")
    int markRun(@Param("id") String id, @Param("now") Instant now);

    long countByEnabledTrueAndScheduleIntervalMinutesIsNotNull();
}
