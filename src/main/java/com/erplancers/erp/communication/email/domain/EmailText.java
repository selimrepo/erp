package com.erplancers.erp.communication.email.domain;

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

public class EmailText extends EntityBase {
    
    String fromMail;
    String fromName;
    String toMail;
    String toName;
    String subject;
    String body;
    String html;
    String cc;
    String bcc;
    
}
