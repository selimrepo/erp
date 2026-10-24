package com.erplancers.erp.accounting.repository;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.accounting.domain.BalanceType;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AccountRepository extends ErpRepository<Account> {
   List<Account> findByaccountParent_Id(Long accountParent);

   List<Account> findByContact_Id(Long contactId);

   List<Account> findByAccountChildren_IdIn(ArrayList<Long> ids);

   List<Account> findByBalanceType(BalanceType balanceType);

   List<Account> findByCurrency(Currency currency);

   List<Account> findByLevel(Integer level);

   List<Account> findByBlocked(boolean blocked);

   List<Account> findByAccountChildren_IdContaining(ArrayList<Long> ids);

}
