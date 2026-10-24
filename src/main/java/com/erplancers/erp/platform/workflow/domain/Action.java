package com.erplancers.erp.platform.workflow.domain;

import com.erplancers.erp.common.domain.EntityBase;


import jakarta.persistence.Entity;
import jakarta.persistence.*;


import lombok.*;


@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Action extends EntityBase {
    String actionName;
    String actionDescription;
    String actionType;
    String parameters;
    String actionValue;
    
}
