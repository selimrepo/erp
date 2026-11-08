package com.erplancers.erp.common.workflow;

import com.erplancers.erp.common.domain.EntityBase;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/** Validates local state transitions; cross-aggregate work belongs in transactional services. */
public class WorkflowLifecycleListener {
    private static final Map<String, Map<String, Set<String>>> TRANSITIONS = Map.of(
        "Order", Map.of("PROCESSING", Set.of("SHIPPED", "CANCELLED")),
        "Purchase", Map.of("DRAFT", Set.of("ORDERED", "CANCELLED"), "ORDERED", Set.of("PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED"), "PARTIALLY_RECEIVED", Set.of("PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED")),
        "Invoice", Map.of("DRAFT", Set.of("CONFIRMED", "CANCELLED"), "CONFIRMED", Set.of("POSTED", "PARTIALLY_PAID", "PAID", "CANCELLED"), "POSTED", Set.of("PARTIALLY_PAID", "PAID", "CANCELLED"), "PARTIALLY_PAID", Set.of("PARTIALLY_PAID", "PAID"), "PAID", Set.of()),
        "Quote", Map.of("DRAFT", Set.of("SENT", "ACCEPTED", "REJECTED", "EXPIRED"), "SENT", Set.of("ACCEPTED", "REJECTED", "EXPIRED"), "ACCEPTED", Set.of("CONVERTED", "REJECTED"), "CONVERTED", Set.of()),
        "Case", Map.of("OPEN", Set.of("IN_PROGRESS", "WAITING", "RESOLVED", "CANCELLED"), "IN_PROGRESS", Set.of("WAITING", "RESOLVED", "CANCELLED"), "WAITING", Set.of("IN_PROGRESS", "RESOLVED", "CANCELLED"), "RESOLVED", Set.of("CLOSED", "OPEN"), "CLOSED", Set.of("OPEN")),
        "Project", Map.of("PLANNED", Set.of("ACTIVE", "CANCELLED"), "ACTIVE", Set.of("ON_HOLD", "COMPLETED", "CANCELLED"), "ON_HOLD", Set.of("ACTIVE", "CANCELLED"), "COMPLETED", Set.of("ARCHIVED"), "ARCHIVED", Set.of()),
        "ProjectTask", Map.of("TODO", Set.of("IN_PROGRESS", "BLOCKED", "CANCELLED"), "IN_PROGRESS", Set.of("BLOCKED", "DONE", "CANCELLED"), "BLOCKED", Set.of("IN_PROGRESS", "CANCELLED"), "DONE", Set.of()),
        "Opportunity", Map.of("NEW", Set.of("QUALIFIED", "PROPOSAL", "WON", "LOST"), "QUALIFIED", Set.of("PROPOSAL", "WON", "LOST"), "PROPOSAL", Set.of("WON", "LOST"), "WON", Set.of(), "LOST", Set.of()),
        "JournalEntry", Map.of("DRAFT", Set.of("POSTED", "CANCELLED"), "POSTED", Set.of(), "CANCELLED", Set.of())
    );

    @PostLoad public void captureLoadedState(Object entity) {
        if (entity instanceof EntityBase base) base.setPersistedWorkflowState(state(entity));
    }
    @PrePersist public void validateInitialState(Object entity) {
        String current = state(entity);
        Map<String, Set<String>> transitions = TRANSITIONS.get(entity.getClass().getSimpleName());
        if (current != null && transitions != null && !transitions.containsKey(current))
            throw new IllegalStateException("Invalid initial " + entity.getClass().getSimpleName() + " state: " + current);
    }
    @PreUpdate public void validateTransition(Object entity) {
        if (!(entity instanceof EntityBase base)) return;
        String previous = base.getPersistedWorkflowState(), current = state(entity);
        if (previous == null || current == null || previous.equals(current)) return;
        Map<String, Set<String>> transitions = TRANSITIONS.get(entity.getClass().getSimpleName());
        if (transitions == null) return;
        Set<String> allowed = transitions.get(previous);
        if (allowed == null || !allowed.contains(current))
            throw new IllegalStateException("Illegal " + entity.getClass().getSimpleName() + " state transition: " + previous + " -> " + current);
        base.setPersistedWorkflowState(current);
    }
    @PostPersist public void capturePersistedState(Object entity) {
        if (entity instanceof EntityBase base) base.setPersistedWorkflowState(state(entity));
    }
    private String state(Object entity) {
        for (String getter : new String[]{"getOrderStatus", "getInvoiceStatus", "getStatus"}) {
            try {
                Object value = entity.getClass().getMethod(getter).invoke(entity);
                if (value != null) return value.toString().toUpperCase(java.util.Locale.ROOT);
            } catch (NoSuchMethodException ignored) {
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Cannot read workflow state from " + entity.getClass().getName(), ex);
            }
        }
        return null;
    }
}
