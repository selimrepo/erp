package com.erplancers.erp.survey.repository;

import com.erplancers.erp.survey.domain.SurveyQuestionOption;
import com.erplancers.erp.survey.domain.SurveyResponse;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface SurveyQuestionOptionRepository extends ErpRepository<SurveyQuestionOption> {
    List<SurveyQuestionOption> findBySurveyCategoryContainingIgnoreCase(String surveyCategory);

    List<SurveyQuestionOption> findBySurveyResponse(SurveyResponse surveyResponse);
}
