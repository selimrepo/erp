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
@Table(name = "bi_report_execution")
@Getter
@Setter
@NoArgsConstructor
public class BiReportExecution {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "report_id", length = 36)
    private String reportId;
    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;
    @Column(name = "resource_slug", nullable = false, length = 160)
    private String resourceSlug;
    @Column(name = "query_json", nullable = false, columnDefinition = "text")
    private String queryJson;
    @Column(name = "delivery_emails_json", nullable = false, columnDefinition = "text")
    private String deliveryEmailsJson = "[]";
    @Column(name = "authorities_json", nullable = false, columnDefinition = "text")
    private String authoritiesJson = "[]";
    @Column(nullable = false, length = 20)
    private String status = "QUEUED";
    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;
    @Column(name = "error_message", length = 4000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;

    @jakarta.persistence.PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @jakarta.persistence.PreUpdate
    void beforeUpdate() { updatedAt = Instant.now(); }
}
