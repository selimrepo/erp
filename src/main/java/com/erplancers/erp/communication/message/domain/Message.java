package com.erplancers.erp.communication.message.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Message extends EntityBase {
    
    String message;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact sender;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contact> receivers;
    LocalDateTime date;
    String type;
    String status;
    String comment;
    String topic;
}
