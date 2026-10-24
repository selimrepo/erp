package com.erplancers.erp.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;



@MappedSuperclass
@NoArgsConstructor
@Getter
@Setter
public class EntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Long id;
    @JsonIgnore
    @Version
    long version;






}
