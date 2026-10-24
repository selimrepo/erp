package com.erplancers.erp.support.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class CaseEvent extends EntityBase {
    String trackerKey;
    String caseEventName;
    String caseEventDescription;
    String caseEventType;
    String caseEventStatus;
    LocalDate caseEventDate;
    @OneToMany
    Set<CaseUpdate> caseUpdates;
    
}
