package com.erplancers.erp.survey.domain;

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

public class SurveyQuestion extends EntityBase {
    
    String question;
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Set<SurveyQuestionResponse> surveyQuestionResponses;

}
