package com.erplancers.erp.communication.email.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.communication.message.domain.Message;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Email extends EntityBase {
    
    String emailName;
    @ManyToOne
    EmailText emailText;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Message> messages;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Contact> receivingContacts;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Contact sendingContact;
    String mailType;
    String username;
    String password;
    String intent;
    Boolean activated;

    
}
