package com.erplancers.erp.survey.repository;

import com.erplancers.erp.survey.domain.SurveyQuestion;
import com.erplancers.erp.survey.domain.SurveyQuestionResponse;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface SurveyQuestionResponseRepository extends ErpRepository<SurveyQuestionResponse> {
    List<SurveyQuestionResponse> findBySurveyQuestion(SurveyQuestion surveyQuestion);

    List<SurveyQuestionResponse> findByAnswerContainingIgnoreCase(String answer);

    List<SurveyQuestionResponse> findByConfidenceBetween(float confidenceAfter, float confidenceBefore);
}
