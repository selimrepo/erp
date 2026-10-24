package com.erplancers.erp.common.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erplancers.erp.common.reporting.ErpBiEngine;
import com.erplancers.erp.common.security.PolicyAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Persistent BI workspace: saved report definitions, explicit user/role sharing,
 * scheduled delivery, durable execution queue and operational telemetry.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bi/workspace")
public class BiWorkspaceController {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ErpBiEngine bi;
    private final PolicyAuthorizationService authorization;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @GetMapping("/reports")
    public List<ReportView> reports(Authentication authentication) {
        requireAuthenticated(authentication);
        List<ReportView> all = jdbc.query("""
                SELECT id, owner_username, title, resource_slug, query_json, shared_with_json,
                       shared_roles_json, schedule_interval_minutes, schedule_emails_json,
                       next_run_at, last_run_at, enabled, created_at, updated_at
                  FROM bi_saved_report ORDER BY updated_at DESC
                """, (rs, n) -> view(rs.getString("id"), rs.getString("owner_username"),
                rs.getString("title"), rs.getString("resource_slug"), rs.getString("query_json"),
                rs.getString("shared_with_json"), rs.getString("shared_roles_json"),
                (Integer) rs.getObject("schedule_interval_minutes"), rs.getString("schedule_emails_json"),
                rs.getTimestamp("next_run_at"), rs.getTimestamp("last_run_at"), rs.getBoolean("enabled"),
                rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), authentication));
        return all.stream().filter(Objects::nonNull).toList();
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportView create(Authentication authentication, @RequestBody SaveReportRequest request) throws Exception {
        requireAuthenticated(authentication);
        validate(request);
        requireRead(authentication, request.resourceSlug());
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant next = nextRun(request.scheduleIntervalMinutes(), request.enabled());
        jdbc.update("""
                INSERT INTO bi_saved_report
                  (id, owner_username, title, resource_slug, query_json, shared_with_json, shared_roles_json,
                   schedule_interval_minutes, schedule_emails_json, schedule_authorities_json, next_run_at,
                   enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, authentication.getName(), cleanTitle(request.title()), request.resourceSlug(),
                mapper.writeValueAsString(request.query()), json(safeList(request.sharedWith())), json(safeList(request.sharedRoles()).stream().map(BiWorkspaceController::normalizeRole).toList()),
                request.scheduleIntervalMinutes(), json(request.scheduleEmails()), json(authorities(authentication)),
                next == null ? null : Timestamp.from(next), Boolean.TRUE.equals(request.enabled()),
                Timestamp.from(now), Timestamp.from(now));
        return getReport(authentication, id);
    }

    @GetMapping("/reports/{id}")
    public ReportView getReport(Authentication authentication, @PathVariable String id) {
        requireAuthenticated(authentication);
        ReportView report = findReport(id);
        requireCanReadReport(authentication, report);
        return report;
    }

    @PutMapping("/reports/{id}")
    public ReportView update(Authentication authentication, @PathVariable String id,
                             @RequestBody SaveReportRequest request) throws Exception {
        requireAuthenticated(authentication);
        ReportView existing = findReport(id);
        requireOwner(authentication, existing);
        validate(request);
        requireRead(authentication, request.resourceSlug());
        Instant next = nextRun(request.scheduleIntervalMinutes(), request.enabled());
        jdbc.update("""
                UPDATE bi_saved_report
                   SET title = ?, resource_slug = ?, query_json = ?, shared_with_json = ?,
                       shared_roles_json = ?, schedule_interval_minutes = ?, schedule_emails_json = ?,
                       schedule_authorities_json = ?, next_run_at = ?, enabled = ?, updated_at = ?
                 WHERE id = ?
                """, cleanTitle(request.title()), request.resourceSlug(), mapper.writeValueAsString(request.query()),
                json(safeList(request.sharedWith())), json(safeList(request.sharedRoles()).stream().map(BiWorkspaceController::normalizeRole).toList()), request.scheduleIntervalMinutes(),
                json(request.scheduleEmails()), json(authorities(authentication)),
                next == null ? null : Timestamp.from(next), Boolean.TRUE.equals(request.enabled()),
                Timestamp.from(Instant.now()), id);
        return getReport(authentication, id);
    }

    @DeleteMapping("/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable String id) {
        ReportView report = findReport(id);
        requireOwner(authentication, report);
        jdbc.update("DELETE FROM bi_report_execution WHERE report_id = ?", id);
        jdbc.update("DELETE FROM bi_saved_report WHERE id = ?", id);
    }

    @PostMapping("/reports/{id}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionView execute(Authentication authentication, @PathVariable String id) {
        ReportView report = getReport(authentication, id);
        requireRead(authentication, report.resourceSlug());
        return enqueue(report.id(), authentication.getName(), report.resourceSlug(), report.query(),
                List.of(), authorities(authentication));
    }

    @GetMapping("/executions/{id}")
    public ExecutionView execution(Authentication authentication, @PathVariable String id) {
        requireAuthenticated(authentication);
        List<ExecutionView> found = jdbc.query("""
                SELECT id, report_id, requested_by, resource_slug, status, result_json, error_message,
                       created_at, started_at, finished_at
                  FROM bi_report_execution WHERE id = ?
                """, (rs, n) -> new ExecutionView(rs.getString("id"), rs.getString("report_id"),
                rs.getString("requested_by"), rs.getString("resource_slug"), rs.getString("status"),
                parseJson(rs.getString("result_json")), rs.getString("error_message"),
                instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("finished_at"))), id);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found");
        ExecutionView view = found.get(0);
        if (!view.requestedBy().equals(authentication.getName()) && !isAdmin(authentication)) {
            if (view.reportId() == null) throw new AccessDeniedException("Execution access denied");
            ReportView report = findReport(view.reportId());
            requireCanReadReport(authentication, report);
        }
        return view;
    }

    @GetMapping("/monitoring")
    public Map<String, Object> monitoring(Authentication authentication) {
        requireAuthenticated(authentication);
        if (!isAdmin(authentication)) throw new AccessDeniedException("BI monitoring requires ADMIN role");
        Map<String, Object> counts = new LinkedHashMap<>();
        jdbc.query("SELECT status, COUNT(*) AS total FROM bi_report_execution GROUP BY status",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    counts.put(rs.getString("status"), rs.getLong("total"));
                });
        Long queries24h = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bi_query_audit WHERE created_at >= ?", Long.class,
                Timestamp.from(Instant.now().minus(Duration.ofHours(24))));
        Long failures24h = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bi_query_audit WHERE created_at >= ? AND succeeded = FALSE", Long.class,
                Timestamp.from(Instant.now().minus(Duration.ofHours(24))));
        Timestamp oldestQueuedAt = jdbc.queryForObject(
                "SELECT MIN(created_at) FROM bi_report_execution WHERE status = 'QUEUED'", Timestamp.class);
        long oldestQueueSeconds = oldestQueuedAt == null ? 0
                : Math.max(0, Duration.between(oldestQueuedAt.toInstant(), Instant.now()).getSeconds());
        counts.put("queriesLast24Hours", queries24h == null ? 0 : queries24h);
        counts.put("queryFailuresLast24Hours", failures24h == null ? 0 : failures24h);
        counts.put("oldestQueuedExecutionSeconds", oldestQueueSeconds);
        counts.put("savedReports", jdbc.queryForObject("SELECT COUNT(*) FROM bi_saved_report", Long.class));
        counts.put("scheduledReports", jdbc.queryForObject(
                "SELECT COUNT(*) FROM bi_saved_report WHERE enabled = TRUE AND schedule_interval_minutes IS NOT NULL", Long.class));
        return counts;
    }

    // Scheduling moved to BiWorkspaceWorker (JPA).
    public void enqueueDueSchedules() {
        List<ScheduledReport> due = jdbc.query("""
                SELECT id, owner_username, resource_slug, query_json, schedule_interval_minutes,
                       schedule_emails_json, schedule_authorities_json, next_run_at
                  FROM bi_saved_report
                 WHERE enabled = TRUE AND schedule_interval_minutes IS NOT NULL
                   AND next_run_at IS NOT NULL AND next_run_at <= CURRENT_TIMESTAMP
                 ORDER BY next_run_at LIMIT 100
                """, (rs, n) -> new ScheduledReport(rs.getString("id"), rs.getString("owner_username"),
                rs.getString("resource_slug"), rs.getString("query_json"), rs.getInt("schedule_interval_minutes"),
                rs.getString("schedule_emails_json"), rs.getString("schedule_authorities_json"),
                rs.getTimestamp("next_run_at")));
        for (ScheduledReport report : due) {
            try {
                Instant next = Instant.now().plus(Duration.ofMinutes(report.intervalMinutes()));
                int claimed = jdbc.update("""
                        UPDATE bi_saved_report SET next_run_at = ?, updated_at = ?
                         WHERE id = ? AND next_run_at = ? AND enabled = TRUE
                        """, Timestamp.from(next), Timestamp.from(Instant.now()), report.id(),
                        report.nextRunAt());
                if (claimed == 0) continue;
                Authentication owner = authentication(report.owner(), readStrings(report.authoritiesJson()));
                if (!authorization.isAllowed(owner, report.resourceSlug(), "READ")) {
                    System.err.println("Skipping scheduled BI report after access was revoked: " + report.id());
                    continue;
                }
                enqueue(report.id(), report.owner(), report.resourceSlug(),
                        mapper.readTree(report.queryJson()), readStrings(report.emailsJson()),
                        readStrings(report.authoritiesJson()));
                jdbc.update("UPDATE bi_saved_report SET last_run_at = ? WHERE id = ?",
                        Timestamp.from(Instant.now()), report.id());
            } catch (Exception ex) {
                System.err.println("Unable to enqueue scheduled BI report " + report.id() + ": " + ex.getMessage());
            }
        }
    }

    // Scheduling moved to BiWorkspaceWorker (JPA).
    public void processOneQueuedExecution() {
        List<QueuedExecution> queued = jdbc.query("""
                SELECT id, report_id, requested_by, resource_slug, query_json,
                       delivery_emails_json, authorities_json
                  FROM bi_report_execution WHERE status = 'QUEUED'
                 ORDER BY created_at LIMIT 1
                """, (rs, n) -> new QueuedExecution(rs.getString("id"), rs.getString("report_id"),
                rs.getString("requested_by"), rs.getString("resource_slug"), rs.getString("query_json"),
                rs.getString("delivery_emails_json"), rs.getString("authorities_json")));
        if (queued.isEmpty()) return;
        QueuedExecution job = queued.get(0);
        Instant now = Instant.now();
        if (jdbc.update("""
                UPDATE bi_report_execution SET status = 'RUNNING', started_at = ?, updated_at = ?
                 WHERE id = ? AND status = 'QUEUED'
                """, Timestamp.from(now), Timestamp.from(now), job.id()) == 0) return;
        try {
            Authentication owner = authentication(job.requestedBy(), readStrings(job.authoritiesJson()));
            if (!authorization.isAllowed(owner, job.resourceSlug(), "READ")) {
                throw new AccessDeniedException("BI READ permission was revoked before execution");
            }
            ErpBiEngine.BiQuery query = mapper.treeToValue(mapper.readTree(job.queryJson()), ErpBiEngine.BiQuery.class);
            ErpBiEngine.BiReport result = bi.query(job.resourceSlug(), query);
            String resultJson = mapper.writeValueAsString(result);
            jdbc.update("""
                    UPDATE bi_report_execution SET status = 'SUCCEEDED', result_json = ?,
                           finished_at = ?, updated_at = ? WHERE id = ?
                    """, resultJson, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), job.id());
            deliver(job, resultJson);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "BI execution failed" : ex.getMessage();
            jdbc.update("""
                    UPDATE bi_report_execution SET status = 'FAILED', error_message = ?,
                           finished_at = ?, updated_at = ? WHERE id = ?
                    """, message.substring(0, Math.min(3900, message.length())),
                    Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), job.id());
        }
    }

    private ExecutionView enqueue(String reportId, String username, String slug, JsonNode query,
                                  List<String> emails, List<String> authorities) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO bi_report_execution
                  (id, report_id, requested_by, resource_slug, query_json, delivery_emails_json,
                   authorities_json, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'QUEUED', ?, ?)
                """, id, reportId, username, slug, query.toString(), json(emails), json(authorities),
                Timestamp.from(now), Timestamp.from(now));
        return new ExecutionView(id, reportId, username, slug, "QUEUED", null, null, now, null, null);
    }

    private void deliver(QueuedExecution job, String resultJson) {
        List<String> emails = readStrings(job.emailsJson());
        if (emails.isEmpty()) return;
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            System.err.println("BI execution succeeded, but email delivery is not configured for " + job.id());
            return;
        }
        String title = job.reportId() == null ? "ERP BI report" : jdbc.query(
                "SELECT title FROM bi_saved_report WHERE id = ?", rs -> rs.next() ? rs.getString(1) : "ERP BI report",
                job.reportId());
        for (String email : emails) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("[Erplancers BI] " + title);
                message.setText("Your scheduled BI report completed.\n\nResource: " + job.resourceSlug()
                        + "\nExecution: " + job.id() + "\n\nResult JSON:\n"
                        + resultJson.substring(0, Math.min(resultJson.length(), 12000)));
                sender.send(message);
            } catch (RuntimeException ex) {
                System.err.println("Unable to deliver BI report to " + email + ": " + ex.getMessage());
            }
        }
    }

    private ReportView findReport(String id) {
        List<ReportView> found = jdbc.query("""
                SELECT id, owner_username, title, resource_slug, query_json, shared_with_json,
                       shared_roles_json, schedule_interval_minutes, schedule_emails_json,
                       next_run_at, last_run_at, enabled, created_at, updated_at
                  FROM bi_saved_report WHERE id = ?
                """, (rs, n) -> view(rs.getString("id"), rs.getString("owner_username"),
                rs.getString("title"), rs.getString("resource_slug"), rs.getString("query_json"),
                rs.getString("shared_with_json"), rs.getString("shared_roles_json"),
                (Integer) rs.getObject("schedule_interval_minutes"), rs.getString("schedule_emails_json"),
                rs.getTimestamp("next_run_at"), rs.getTimestamp("last_run_at"), rs.getBoolean("enabled"),
                rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), null), id);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved report not found");
        return found.get(0);
    }

    private ReportView view(String id, String owner, String title, String slug, String query,
                            String sharedUsersJson, String sharedRolesJson, Integer interval,
                            String emailsJson, Timestamp next, Timestamp last, boolean enabled,
                            Timestamp created, Timestamp updated, Authentication authentication) {
        if (authentication != null) {
            List<String> users = readStrings(sharedUsersJson);
            List<String> roles = readStrings(sharedRolesJson);
            boolean shared = users.stream().anyMatch(u -> u.equalsIgnoreCase(authentication.getName()))
                    || authorities(authentication).stream().map(BiWorkspaceController::normalizeRole).anyMatch(roles::contains);
            if (!owner.equalsIgnoreCase(authentication.getName()) && !isAdmin(authentication) && !shared) return null;
            if (!authorization.isAllowed(authentication, slug, "READ")) return null;
        }
        return new ReportView(id, owner, title, slug, parseJson(query), readStrings(sharedUsersJson),
                readStrings(sharedRolesJson), interval, readStrings(emailsJson), instant(next), instant(last),
                enabled, instant(created), instant(updated));
    }

    private void requireCanReadReport(Authentication authentication, ReportView report) {
        requireAuthenticated(authentication);
        boolean shared = report.sharedWith().stream().anyMatch(u -> u.equalsIgnoreCase(authentication.getName()))
                || authorities(authentication).stream().map(BiWorkspaceController::normalizeRole)
                    .anyMatch(report.sharedRoles()::contains);
        if (!report.owner().equalsIgnoreCase(authentication.getName()) && !isAdmin(authentication) && !shared) {
            throw new AccessDeniedException("Saved report access denied");
        }
        requireRead(authentication, report.resourceSlug());
    }

    private void requireOwner(Authentication authentication, ReportView report) {
        requireAuthenticated(authentication);
        if (!report.owner().equalsIgnoreCase(authentication.getName()) && !isAdmin(authentication)) {
            throw new AccessDeniedException("Only the report owner or an administrator may change this report");
        }
    }

    private void validate(SaveReportRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()
                || request.title().trim().length() > 200 || request.resourceSlug() == null
                || !request.resourceSlug().matches("[a-zA-Z0-9_-]{1,160}") || request.query() == null
                || !request.query().isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A title, resource slug and query object are required");
        }
        try {
            ErpBiEngine.BiQuery query = mapper.treeToValue(request.query(), ErpBiEngine.BiQuery.class);
            if (query.dimensions().size() > 12 || query.measures().size() > 30 || query.limit() > 1000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query exceeds BI workspace limits");
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid BI query definition", ex);
        }
        if (request.scheduleIntervalMinutes() != null
                && (request.scheduleIntervalMinutes() < 15 || request.scheduleIntervalMinutes() > 43200)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule interval must be between 15 minutes and 30 days");
        }
        List<String> emails = safeList(request.scheduleEmails());
        if (emails.size() > 20 || emails.stream().anyMatch(e -> !EMAIL.matcher(e).matches())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at most 20 valid delivery email addresses");
        }
        if (safeList(request.sharedWith()).size() > 100 || safeList(request.sharedRoles()).size() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many report sharing entries");
        }
    }

    private ReportView getReportInternal(Authentication authentication, String id) {
        ReportView report = findReport(id);
        requireCanReadReport(authentication, report);
        return report;
    }

    private void requireRead(Authentication authentication, String slug) {
        if (!authorization.isAllowed(authentication, slug, "READ")) {
            throw new AccessDeniedException("BI READ access denied for resource: " + slug);
        }
    }
    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new AccessDeniedException("Authentication required");
    }
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> normalizeRole(a.getAuthority()).equals("ADMIN"));
    }
    private static String normalizeRole(String value) {
        return value == null ? "" : value.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT);
    }
    private static List<String> authorities(Authentication authentication) {
        return authentication == null ? List.of() : authentication.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList();
    }
    private Authentication authentication(String username, List<String> roles) {
        return UsernamePasswordAuthenticationToken.authenticated(username, null,
                roles.stream().map(SimpleGrantedAuthority::new).toList());
    }
    private String cleanTitle(String title) { return title.trim(); }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value == null ? List.of() : value); }
        catch (Exception ex) { throw new IllegalArgumentException("Unable to serialize BI workspace data", ex); }
    }
    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull)
                .map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }
    private List<String> readStrings(String value) {
        try { return value == null ? List.of() : mapper.readValue(value, STRINGS); }
        catch (Exception ex) { return List.of(); }
    }
    private JsonNode parseJson(String value) {
        try { return value == null ? null : mapper.readTree(value); }
        catch (Exception ex) { return null; }
    }
    private Instant instant(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant(); }
    private Instant nextRun(Integer minutes, Boolean enabled) {
        return minutes == null || !Boolean.TRUE.equals(enabled) ? null : Instant.now().plus(Duration.ofMinutes(minutes));
    }

    public record SaveReportRequest(String title, String resourceSlug, JsonNode query,
                                    List<String> sharedWith, List<String> sharedRoles,
                                    Integer scheduleIntervalMinutes, List<String> scheduleEmails,
                                    Boolean enabled) {}
    public record ReportView(String id, String owner, String title, String resourceSlug, JsonNode query,
                             List<String> sharedWith, List<String> sharedRoles, Integer scheduleIntervalMinutes,
                             List<String> scheduleEmails, Instant nextRunAt, Instant lastRunAt,
                             boolean enabled, Instant createdAt, Instant updatedAt) {}
    public record ExecutionView(String id, String reportId, String requestedBy, String resourceSlug,
                                String status, Object result, String error, Instant createdAt,
                                Instant startedAt, Instant finishedAt) {}
    private record ScheduledReport(String id, String owner, String resourceSlug, String queryJson,
                                   int intervalMinutes, String emailsJson, String authoritiesJson, Timestamp nextRunAt) {}
    private record QueuedExecution(String id, String reportId, String requestedBy, String resourceSlug,
                                   String queryJson, String emailsJson, String authoritiesJson) {}
}
