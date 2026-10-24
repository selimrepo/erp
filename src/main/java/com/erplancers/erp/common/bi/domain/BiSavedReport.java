package com.erplancers.erp.common.bi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bi_saved_report")
@Getter
@Setter
@NoArgsConstructor
public class BiSavedReport {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "owner_username", nullable = false, length = 120)
    private String ownerUsername;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(name = "resource_slug", nullable = false, length = 160)
    private String resourceSlug;
    @Column(name = "query_json", nullable = false, columnDefinition = "text")
    private String queryJson;
    @Column(name = "shared_with_json", nullable = false, columnDefinition = "text")
    private String sharedWithJson = "[]";
    @Column(name = "shared_roles_json", nullable = false, columnDefinition = "text")
    private String sharedRolesJson = "[]";
    @Column(name = "schedule_interval_minutes")
    private Integer scheduleIntervalMinutes;
    @Column(name = "schedule_emails_json", nullable = false, columnDefinition = "text")
    private String scheduleEmailsJson = "[]";
    @Column(name = "schedule_authorities_json", nullable = false, columnDefinition = "text")
    private String scheduleAuthoritiesJson = "[]";
    @Column(name = "next_run_at")
    private Instant nextRunAt;
    @Column(name = "last_run_at")
    private Instant lastRunAt;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @jakarta.persistence.PreUpdate
    void beforeUpdate() { updatedAt = Instant.now(); }
}
