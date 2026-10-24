package com.erplancers.erp.support.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "erpcase")

public class Case extends EntityBase {
    
    String name;
    String description;
    String category;
    String status;
    String type;
    String location;
    String resolution;
    String state;
    String workLog;
}
