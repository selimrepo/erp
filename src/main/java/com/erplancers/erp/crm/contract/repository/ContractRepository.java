package com.erplancers.erp.crm.contract.repository;

import com.erplancers.erp.crm.contract.domain.Contract;
import com.erplancers.erp.common.repository.ErpRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ContractRepository extends ErpRepository<Contract> {
    List<Contract> findByContractNameContainingIgnoreCase(String contractName);
    List<Contract> findByCurrency_TitleContains(String symbol);

    List<Contract> findByCustomerSignedTimeBetweenOrCompanySignedTimeBetween(LocalDateTime customerSignedTimeAfter, LocalDateTime customerSignedTimeBefore, LocalDateTime companySignedTimeAfter, LocalDateTime companySignedTimeBefore);
    List<Contract> findByCustomer_NameContainsOrCustomer_LastNameContains(String name,String lastname);
}
