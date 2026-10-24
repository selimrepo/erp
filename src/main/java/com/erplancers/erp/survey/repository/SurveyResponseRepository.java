package com.erplancers.erp.survey.repository;

import com.erplancers.erp.survey.domain.SurveyQuestionResponse;
import com.erplancers.erp.survey.domain.SurveyResponse;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface SurveyResponseRepository extends ErpRepository<SurveyResponse> {
    List<SurveyResponse> findBySurveyQuestionResponsesContaining(Set<SurveyQuestionResponse> surveyQuestionResponses);

    List<SurveyResponse> findByHappinessBetween(float happinessAfter, float happinessBefore);
}
