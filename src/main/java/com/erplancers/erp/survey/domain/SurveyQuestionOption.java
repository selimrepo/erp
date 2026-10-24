package com.erplancers.erp.survey.domain;

import com.erplancers.erp.common.domain.EntityBase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table

public class SurveyQuestionOption extends EntityBase {
    
    String surveyCategory;
    String surveyStatus;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    SurveyResponse surveyResponse;
}
