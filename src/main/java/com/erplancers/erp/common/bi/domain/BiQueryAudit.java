package com.erplancers.erp.common.bi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bi_query_audit")
@Getter
@Setter
@NoArgsConstructor
public class BiQueryAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String username;
    @Column(name = "resource_slug", nullable = false, length = 160)
    private String resourceSlug;
    @Column(name = "elapsed_ms", nullable = false)
    private long elapsedMs;
    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit;
    @Column(nullable = false)
    private boolean succeeded;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @jakarta.persistence.PrePersist
    void beforeInsert() { if (createdAt == null) createdAt = Instant.now(); }
}
