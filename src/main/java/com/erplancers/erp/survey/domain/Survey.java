package com.erplancers.erp.survey.domain;

import com.erplancers.erp.common.domain.EntityBase;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.inventory.product.domain.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class Survey extends EntityBase {
    LocalDate date;
    @ManyToOne
    Contact customer;
    @ManyToOne
    Product product;
    float totalConfidence;
    @OneToMany
    Set<SurveyQuestion>  surveyQuestions;
    @OneToMany
    Set<SurveyResponse> surveyResponses;
}
