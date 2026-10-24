package com.erplancers.erp.platform.notification.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Announcement extends EntityBase {
    Boolean read;
    Boolean isActive;
    Boolean topic;
    String title;
    String description;
    String location;
    String message;
    Boolean sent;
    Boolean autoPublish;
    LocalDateTime dateStart;
    LocalDateTime dateEnd;
    LocalDateTime dateCreated;
    
}
