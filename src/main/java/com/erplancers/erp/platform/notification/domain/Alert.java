package com.erplancers.erp.platform.notification.domain;

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
@Table


public class Alert extends EntityBase {
    String alertName;
    String alertDescription;
    String alertType;
    String alertValue;
    Boolean alertEnabled;
    Boolean alertConfirmed;
    Boolean isRead;
    String urlRedirect;
    
}
