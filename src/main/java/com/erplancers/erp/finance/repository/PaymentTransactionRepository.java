package com.erplancers.erp.finance.repository;

import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.PaymentTransaction;
import com.erplancers.erp.common.repository.ErpRepository;

import java.util.List;

public interface PaymentTransactionRepository extends ErpRepository<PaymentTransaction> {
    List<PaymentTransaction> findAllByAmount(Long amount);

    List<PaymentTransaction> findAllByQuantity(Long quantity);

    List<PaymentTransaction> findAllByPaymentBetween(Long paymentAfter, Long paymentBefore);

    List<PaymentTransaction> findAllByCurrency(Currency currency);
}
