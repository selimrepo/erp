package com.erplancers.erp.survey.repository;

import com.erplancers.erp.survey.domain.SurveyQuestion;
import com.erplancers.erp.survey.domain.SurveyQuestionResponse;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;
import java.util.Set;

public interface SurveyQuestionRepository extends ErpRepository<SurveyQuestion> {
    List<SurveyQuestion> findByQuestion(String question);

    List<SurveyQuestion> findBySurveyQuestionResponsesContaining(Set<SurveyQuestionResponse> surveyQuestionResponses);
}
