package com.erplancers.erp.common.bi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erplancers.erp.common.bi.domain.BiReportExecution;
import com.erplancers.erp.common.bi.domain.BiSavedReport;
import com.erplancers.erp.common.bi.repository.BiReportExecutionRepository;
import com.erplancers.erp.common.bi.repository.BiSavedReportRepository;
import com.erplancers.erp.common.reporting.ErpBiEngine;
import com.erplancers.erp.common.security.PolicyAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled BI processing backed by JPA repositories. Claims are conditional
 * updates so multiple application instances do not normally process the same
 * schedule or queued execution concurrently.
 */
@Component
@RequiredArgsConstructor
public class BiWorkspaceWorker {
    private static final Logger log = LoggerFactory.getLogger(BiWorkspaceWorker.class);

    private final BiSavedReportRepository savedReports;
    private final BiReportExecutionRepository executions;
    private final ObjectMapper mapper;
    private final ErpBiEngine bi;
    private final PolicyAuthorizationService authorization;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Scheduled(fixedDelayString = "${bi.schedule.poll-ms:60000}")
    public void enqueueDueSchedules() {
        Instant now = Instant.now();
        List<BiSavedReport> due = savedReports
                .findTop100ByEnabledTrueAndScheduleIntervalMinutesIsNotNullAndNextRunAtLessThanEqualOrderByNextRunAtAsc(now);

        for (BiSavedReport report : due) {
            try {
                Instant next = Instant.now().plus(Duration.ofMinutes(report.getScheduleIntervalMinutes()));
                if (savedReports.claimSchedule(report.getId(), report.getNextRunAt(), next, Instant.now()) == 0) {
                    continue;
                }

                Authentication owner = authentication(report.getOwnerUsername(), readStrings(report.getScheduleAuthoritiesJson()));
                if (!authorization.isAllowed(owner, report.getResourceSlug(), "READ")) {
                    log.warn("Skipping scheduled BI report after access was revoked: {}", report.getId());
                    continue;
                }

                enqueue(report.getId(), report.getOwnerUsername(), report.getResourceSlug(),
                        report.getQueryJson(), report.getScheduleEmailsJson(), report.getScheduleAuthoritiesJson());
                savedReports.markRun(report.getId(), Instant.now());
            } catch (Exception ex) {
                log.warn("Unable to enqueue scheduled BI report {}", report.getId(), ex);
            }
        }
    }

    @Scheduled(fixedDelayString = "${bi.execution.poll-ms:2000}")
    public void processOneQueuedExecution() {
        List<BiReportExecution> queued = executions.findTop1ByStatusOrderByCreatedAtAsc("QUEUED");
        if (queued.isEmpty()) return;

        String id = queued.get(0).getId();
        Instant started = Instant.now();
        if (executions.claimQueued(id, started) == 0) return;

        BiReportExecution job = executions.findById(id).orElse(null);
        if (job == null) return;
        try {
            Authentication owner = authentication(job.getRequestedBy(), readStrings(job.getAuthoritiesJson()));
            if (!authorization.isAllowed(owner, job.getResourceSlug(), "READ")) {
                throw new AccessDeniedException("BI READ permission was revoked before execution");
            }

            ErpBiEngine.BiQuery query = mapper.treeToValue(
                    mapper.readTree(job.getQueryJson()), ErpBiEngine.BiQuery.class);
            ErpBiEngine.BiReport result = bi.query(job.getResourceSlug(), query);
            String resultJson = mapper.writeValueAsString(result);
            job.setStatus("SUCCEEDED");
            job.setResultJson(resultJson);
            job.setErrorMessage(null);
            job.setFinishedAt(Instant.now());
            executions.save(job);
            deliver(job, resultJson);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "BI execution failed" : ex.getMessage();
            job.setStatus("FAILED");
            job.setErrorMessage(message.substring(0, Math.min(3900, message.length())));
            job.setFinishedAt(Instant.now());
            executions.save(job);
            log.warn("BI execution {} failed", id, ex);
        }
    }

    @Transactional
    public BiReportExecution enqueue(String reportId, String username, String slug,
                                    String queryJson, String emailsJson, String authoritiesJson) {
        BiReportExecution execution = new BiReportExecution();
        execution.setId(UUID.randomUUID().toString());
        execution.setReportId(reportId);
        execution.setRequestedBy(username);
        execution.setResourceSlug(slug);
        execution.setQueryJson(queryJson);
        execution.setDeliveryEmailsJson(emailsJson == null ? "[]" : emailsJson);
        execution.setAuthoritiesJson(authoritiesJson == null ? "[]" : authoritiesJson);
        execution.setStatus("QUEUED");
        Instant now = Instant.now();
        execution.setCreatedAt(now);
        execution.setUpdatedAt(now);
        return executions.save(execution);
    }

    private void deliver(BiReportExecution job, String resultJson) {
        List<String> emails = readStrings(job.getDeliveryEmailsJson());
        if (emails.isEmpty()) return;
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("BI execution succeeded, but email delivery is not configured for {}", job.getId());
            return;
        }

        String title = job.getReportId() == null ? "ERP BI report"
                : savedReports.findById(job.getReportId()).map(BiSavedReport::getTitle).orElse("ERP BI report");
        for (String email : emails) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("[Erplancers BI] " + title);
                message.setText("Your scheduled BI report completed.\n\nResource: " + job.getResourceSlug()
                        + "\nExecution: " + job.getId() + "\n\nResult JSON:\n"
                        + resultJson.substring(0, Math.min(resultJson.length(), 12000)));
                sender.send(message);
            } catch (RuntimeException ex) {
                log.warn("Unable to deliver BI report to {}", email, ex);
            }
        }
    }

    private Authentication authentication(String username, List<String> authorities) {
        return UsernamePasswordAuthenticationToken.authenticated(username, null,
                authorities.stream().map(SimpleGrantedAuthority::new).toList());
    }

    private List<String> readStrings(String value) {
        try {
            return value == null ? List.of() : mapper.readValue(value,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            log.warn("Invalid JSON list in BI persistence; treating it as empty");
            return List.of();
        }
    }
}
