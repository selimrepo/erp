package com.erplancers.erp.accounting.domain;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import com.erplancers.erp.accounting.domain.JournalEntryLifecycleListener;

@Entity @EntityListeners(JournalEntryLifecycleListener.class) @NoArgsConstructor @Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_journal_idempotency", columnNames = "idempotency_key"))
public class JournalEntry extends EntityBase {
    @ManyToOne(fetch = FetchType.LAZY) private Contact company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private FiscalYear fiscalYear;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private FiscalPeriod fiscalPeriod;
    private String entryId;
    private LocalDate entryDate;
    private String description;
    @Enumerated(EnumType.STRING) private JournalEntryType entryType;
    @Enumerated(EnumType.STRING) private JournalEntryStatus status;
    @Column(name = "idempotency_key", length = 180) private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", length = 64) private String idempotencyPayloadHash;
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<JournalEntryLine> lines = new LinkedHashSet<>();
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private String persistedPostingFingerprint;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean postingValidated;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Transient private boolean postingValidated;
}
