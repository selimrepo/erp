package com.erplancers.erp.support.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class CaseUpdate extends EntityBase {
    String trackerKey;
    String caseUpdateType;
    String caseUpdateStatus;
    LocalDate caseUpdateDate;
    Boolean trackerStatus;
    LocalDateTime caseUpdateDateTime;
    
}
