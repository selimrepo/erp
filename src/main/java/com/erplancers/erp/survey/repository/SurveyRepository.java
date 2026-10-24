package com.erplancers.erp.survey.repository;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.survey.domain.Survey;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDate;
import java.util.List;

public interface SurveyRepository extends ErpRepository<Survey> {
    List<Survey> findByDateBetween(LocalDate dateAfter, LocalDate dateBefore);

    List<Survey> findByProduct(Product product);

    List<Survey> findByCustomer(Contact customer);

    List<Survey> findByTotalConfidenceBetween(float totalConfidenceAfter, float totalConfidenceBefore);
}
