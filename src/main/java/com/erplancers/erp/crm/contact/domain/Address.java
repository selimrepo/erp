package com.erplancers.erp.crm.contact.domain;

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

public class Address extends EntityBase {
    String firstName;
    String lastName;
    String address1;
    String address2;
    String postcode;
    
}
