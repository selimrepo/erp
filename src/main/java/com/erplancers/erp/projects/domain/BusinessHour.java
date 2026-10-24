package com.erplancers.erp.projects.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class BusinessHour extends EntityBase {
    String businessHourName;
    LocalTime startTime;
    LocalTime endTime;
    Boolean active;
    Boolean completed;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Project project;
    String dayofWeek;
    
}
