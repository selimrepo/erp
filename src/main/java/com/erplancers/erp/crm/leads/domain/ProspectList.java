package com.erplancers.erp.crm.leads.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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

public class ProspectList extends EntityBase {
    
    String name;
    String description;
    String status;
    String type;
    String category;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<Prospect> prospects;
}
