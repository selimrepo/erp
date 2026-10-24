package com.erplancers.erp.common.repository;

import com.erplancers.erp.common.domain.EntityBase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;



@NoRepositoryBean
public interface ErpRepository<T extends EntityBase> extends  JpaRepository<T, Long>, JpaSpecificationExecutor<T> {



}
