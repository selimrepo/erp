package com.erplancers.erp.common.bi.repository;

import com.erplancers.erp.common.bi.domain.BiQueryAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BiQueryAuditRepository extends JpaRepository<BiQueryAudit, Long> {
    @Query("select count(a) from BiQueryAudit a where a.createdAt >= :since")
    long countSince(@Param("since") Instant since);

    @Query("select count(a) from BiQueryAudit a where a.createdAt >= :since and a.succeeded = false")
    long countFailuresSince(@Param("since") Instant since);
}
