package com.erplancers.erp.common.workflow;

import com.erplancers.erp.accounting.domain.*;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.finance.domain.*;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.stock.domain.*;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.procurement.domain.*;
import com.erplancers.erp.sales.order.domain.*;
import com.erplancers.erp.sales.quote.domain.Quote;
import com.erplancers.erp.sales.quote.domain.QuoteLine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ErpWorkflowService {
    private final EntityManager em;
    public ErpWorkflowService(EntityManager em) { this.em = em; }

    @Transactional
    public Map<String,Object> createSalesOrder(ErpWorkflowRequests.SalesOrder request) {
        List<Order> existing = em.createQuery("select o from Order o where o.idempotencyKey=:key", Order.class)
                .setParameter("key", request.idempotencyKey()).setMaxResults(1).getResultList();
        if (!existing.isEmpty()) {
            if (!Objects.equals(existing.get(0).getCustomer().getId(), request.customerId()))
                throw new IllegalArgumentException("Idempotency key was already used for a different sales order");
            return result("order", existing.get(0).getId(), "idempotentReplay", true);
        }
        Customer customer = required(Customer.class, request.customerId());
        Contact contact = required(Contact.class, request.shippingContactId());
        Currency currency = required(Currency.class, request.currencyId());
        Store store = em.find(Store.class, request.storeId(), LockModeType.PESSIMISTIC_WRITE);
        if (store == null) throw new IllegalArgumentException("Store not found: " + request.storeId());
        Order order = new Order();
        order.setCustomer(customer); order.setShippingContact(contact); order.setCurrency(currency);
        order.setComment(request.comment()); order.setOrderStatus(OrderStatus.PROCESSING);
        order.setIdempotencyKey(request.idempotencyKey()); order.setTotalAmount(0L);
        em.persist(order); em.flush();
        long total = 0, totalTax = 0;
        for (int i=0;i<request.lines().size();i++) {
            ErpWorkflowRequests.SalesLine input = request.lines().get(i);
            Product product = required(Product.class, input.productId());
            ProductStockLocation stock = stockForUpdate(product.getId(), store.getId());
            long available = stock.getQuantity() == null ? 0 : stock.getQuantity();
            if (available < input.quantity()) throw new IllegalStateException("Insufficient stock for product " + product.getId() + ": available " + available);
            long netAmount = Math.multiplyExact(input.quantity(), input.unitPrice());
            TaxCalculation tax = calculateTax(netAmount, input.taxRateId());
            long lineTotal = Math.addExact(netAmount, tax.amount());
            total = Math.addExact(total, lineTotal);
            totalTax = Math.addExact(totalTax, tax.amount());
            stock.setQuantity(available - input.quantity());
            OrderLine line = new OrderLine(); line.setOrder(order); line.setProduct(product);
            line.setQuantity(input.quantity()); line.setUnitPrice(input.unitPrice()); line.setTaxRate(tax.rate()); line.setTaxAmount(tax.amount()); line.setLineTotal(lineTotal);
            order.getLines().add(line);
            movement(product, store, input.quantity(), "SALE_RESERVATION", "ORDER", order.getId().toString(), request.idempotencyKey()+":sale:"+i);
        }
        order.setTotalAmount(total);
        if (request.createInvoice()) {
            Invoice invoice = new Invoice(); invoice.setInvoiceName("INV-S-" + order.getId());
            invoice.setInvoiceDescription("Sales order " + order.getId()); invoice.setAmount(total);
            invoice.setPaidAmount(0L); invoice.setCurrency(currency); invoice.setInvoiceType(InvoiceType.SALE);
            invoice.setNetAmount(total - totalTax); invoice.setTaxAmount(totalTax);
            invoice.setInvoiceStatus(InvoiceStatus.CONFIRMED); invoice.setWorkflowKey("sales:"+request.idempotencyKey());
            invoice.setBillingContacts(Set.of(contact)); invoice.setShippingContacts(Set.of(contact));
            em.persist(invoice); order.setInvoice(invoice);
        }
        em.flush();
        return result("order", order.getId(), "invoiceId", order.getInvoice()==null?null:order.getInvoice().getId(),
                "totalAmount", total, "status", order.getOrderStatus().name(), "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> convertQuoteToOrder(Long quoteId, ErpWorkflowRequests.QuoteConversion request) {
        Quote quote = em.find(Quote.class, quoteId, LockModeType.PESSIMISTIC_WRITE);
        if (quote == null) throw new IllegalArgumentException("Quote not found: " + quoteId);
        if ("CONVERTED".equals(quote.getStatus())) {
            if (!Objects.equals(quote.getConversionIdempotencyKey(), request.idempotencyKey()))
                throw new IllegalStateException("Quote has already been converted");
            return result("quote", quoteId, "order", quote.getConvertedOrder().getId(), "status", "CONVERTED", "idempotentReplay", true);
        }
        if (!"ACCEPTED".equals(quote.getStatus())) throw new IllegalStateException("Only accepted quotes can be converted");
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now()))
            throw new IllegalStateException("Quote has expired");
        if (quote.getContactBilling() == null || quote.getContactShipping() == null || quote.getCurrency() == null)
            throw new IllegalStateException("Quote requires billing contact, shipping contact and currency");
        List<Customer> customers = em.createQuery("select c from Customer c where c.contact.id=:contactId", Customer.class)
                .setParameter("contactId", quote.getContactBilling().getId()).setMaxResults(2).getResultList();
        if (customers.size() != 1) throw new IllegalStateException("Quote billing contact must resolve to exactly one customer");
        if (quote.getLines() == null || quote.getLines().isEmpty()) throw new IllegalStateException("Quote has no product lines");
        List<ErpWorkflowRequests.SalesLine> lines = quote.getLines().stream()
                .map(line -> new ErpWorkflowRequests.SalesLine(line.getProduct().getId(), line.getQuantity(), line.getUnitPrice(), line.getTaxRate()==null?null:line.getTaxRate().getId()))
                .toList();
        ErpWorkflowRequests.SalesOrder orderRequest = new ErpWorkflowRequests.SalesOrder(
                request.idempotencyKey(), customers.get(0).getId(), quote.getContactShipping().getId(),
                quote.getCurrency().getId(), request.storeId(), lines, request.comment(), request.createInvoice());
        Map<String,Object> created = createSalesOrder(orderRequest);
        Order order = required(Order.class, ((Number) created.get("order")).longValue());
        quote.setConvertedOrder(order);
        quote.setConversionIdempotencyKey(request.idempotencyKey());
        quote.setStatus("CONVERTED");
        em.flush();
        return result("quote", quoteId, "order", order.getId(), "invoiceId", created.get("invoiceId"),
                "status", "CONVERTED", "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> createPurchase(ErpWorkflowRequests.PurchaseOrder request) {
        List<Purchase> existing = em.createQuery("select p from Purchase p where p.idempotencyKey=:key", Purchase.class)
                .setParameter("key", request.idempotencyKey()).setMaxResults(1).getResultList();
        if (!existing.isEmpty()) {
            if (!Objects.equals(existing.get(0).getSupplier().getId(), request.supplierId()))
                throw new IllegalArgumentException("Idempotency key was already used for a different purchase");
            return result("purchase", existing.get(0).getId(), "idempotentReplay", true);
        }
        Supplier supplier = required(Supplier.class, request.supplierId());
        Store store = required(Store.class, request.storeId());
        Currency currency = required(Currency.class, request.currencyId());
        Purchase purchase = new Purchase(); purchase.setSupplier(supplier); purchase.setStore(store); purchase.setCurrency(currency); purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setStatus("ORDERED"); purchase.setIdempotencyKey(request.idempotencyKey()); purchase.setTotalAmount(0L);
        em.persist(purchase); em.flush();
        long total=0, totalTax=0;
        for (ErpWorkflowRequests.PurchaseLine input : request.lines()) {
            Product product = required(Product.class, input.productId());
            long netAmount = Math.multiplyExact(input.quantity(), input.unitPrice());
            TaxCalculation tax = calculateTax(netAmount, input.taxRateId());
            long lineTotal = Math.addExact(netAmount, tax.amount()); total=Math.addExact(total,lineTotal); totalTax=Math.addExact(totalTax,tax.amount());
            PurchaseDetail detail = new PurchaseDetail(); detail.setPurchase(purchase); detail.setSupplier(supplier);
            detail.setProduct(product); detail.setQuantity(input.quantity()); detail.setPrice(input.unitPrice());
            detail.setTaxRate(tax.rate()); detail.setTaxAmount(tax.amount()); detail.setLineTotal(lineTotal);
            purchase.getPurchaseDetails().add(detail);
        }
        purchase.setTotalAmount(total); purchase.setTaxAmount(totalTax); em.flush();
        return result("purchase", purchase.getId(), "totalAmount", total, "status", purchase.getStatus(), "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> receivePurchase(Long purchaseId, ErpWorkflowRequests.ReceivePurchase request, Long storeId) {
        return receivePurchaseLines(purchaseId, request.idempotencyKey(), null, storeId);
    }

    @Transactional
    public Map<String,Object> receivePurchasePartial(Long purchaseId, ErpWorkflowRequests.PartialReceivePurchase request, Long storeId) {
        return receivePurchaseLines(purchaseId, request.idempotencyKey(), request.lines(), storeId);
    }

    private Map<String,Object> receivePurchaseLines(Long purchaseId, String idempotencyKey,
            List<ErpWorkflowRequests.PurchaseReceiptLineInput> requestedLines, Long storeId) {
        List<PurchaseReceipt> previous = em.createQuery("select r from PurchaseReceipt r where r.idempotencyKey=:key", PurchaseReceipt.class)
                .setParameter("key", idempotencyKey).setMaxResults(1).getResultList();
        if (!previous.isEmpty()) {
            if (!Objects.equals(previous.get(0).getPurchase().getId(), purchaseId))
                throw new IllegalArgumentException("Receipt idempotency key belongs to another purchase");
            return result("purchase", purchaseId, "receipt", previous.get(0).getId(), "status", previous.get(0).getPurchase().getStatus(), "idempotentReplay", true);
        }
        Purchase purchase = em.find(Purchase.class, purchaseId, LockModeType.PESSIMISTIC_WRITE);
        if (purchase == null) throw new IllegalArgumentException("Purchase not found: " + purchaseId);
        if (!Set.of("ORDERED", "PARTIALLY_RECEIVED").contains(purchase.getStatus()))
            throw new IllegalStateException("Only ordered or partially received purchases can receive goods");
        if (purchase.getStore()==null || !Objects.equals(purchase.getStore().getId(),storeId))
            throw new IllegalArgumentException("Receipt warehouse must match the purchase warehouse");
        Store store = em.find(Store.class, purchase.getStore().getId(), LockModeType.PESSIMISTIC_WRITE);
        if (purchase.getPurchaseDetails().isEmpty()) throw new IllegalStateException("Purchase has no detail lines");

        Map<Long, Long> quantities = new LinkedHashMap<>();
        if (requestedLines == null) {
            for (PurchaseDetail detail : purchase.getPurchaseDetails()) {
                long received = detail.getReceivedQuantity() == null ? 0L : detail.getReceivedQuantity();
                long remaining = detail.getQuantity() - received;
                if (remaining > 0) quantities.put(detail.getId(), remaining);
            }
        } else {
            for (ErpWorkflowRequests.PurchaseReceiptLineInput line : requestedLines) {
                if (quantities.putIfAbsent(line.purchaseDetailId(), line.quantity()) != null)
                    throw new IllegalArgumentException("Duplicate purchase detail in receipt: " + line.purchaseDetailId());
            }
        }
        if (quantities.isEmpty()) throw new IllegalStateException("There are no outstanding quantities to receive");

        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setPurchase(purchase); receipt.setIdempotencyKey(idempotencyKey); receipt.setReceivedAt(LocalDateTime.now());
        em.persist(receipt);
        long receivedTotal = 0L;
        for (Map.Entry<Long, Long> entry : quantities.entrySet()) {
            PurchaseDetail detail = purchase.getPurchaseDetails().stream()
                    .filter(d -> Objects.equals(d.getId(), entry.getKey())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Purchase detail does not belong to this purchase: " + entry.getKey()));
            long alreadyReceived = detail.getReceivedQuantity() == null ? 0L : detail.getReceivedQuantity();
            long nextReceived = Math.addExact(alreadyReceived, entry.getValue());
            if (entry.getValue() <= 0 || nextReceived > detail.getQuantity())
                throw new IllegalArgumentException("Receipt quantity exceeds outstanding quantity for purchase detail " + detail.getId());
            detail.setReceivedQuantity(nextReceived);
            ProductStockLocation stock = stockForUpdateOrCreate(detail.getProduct(), store);
            long onHand = stock.getQuantity() == null ? 0L : stock.getQuantity();
            stock.setQuantity(Math.addExact(onHand, entry.getValue()));
            movement(detail.getProduct(), store, entry.getValue(), "PURCHASE_RECEIPT", "PURCHASE", purchaseId.toString(),
                    idempotencyKey + ":receipt:" + detail.getId());
            StockItem item = new StockItem(); item.setProduct(detail.getProduct()); item.setSupplier(purchase.getSupplier());
            item.setStore(store); item.setDateIn(LocalDate.now()); em.persist(item);
            PurchaseReceiptLine receiptLine = new PurchaseReceiptLine();
            receiptLine.setReceipt(receipt); receiptLine.setPurchaseDetail(detail); receiptLine.setQuantity(entry.getValue());
            receipt.getLines().add(receiptLine);
            receivedTotal = Math.addExact(receivedTotal, entry.getValue());
        }
        boolean fullyReceived = purchase.getPurchaseDetails().stream().allMatch(d ->
                (d.getReceivedQuantity() == null ? 0L : d.getReceivedQuantity()) >= d.getQuantity());
        purchase.setStatus(fullyReceived ? "RECEIVED" : "PARTIALLY_RECEIVED");
        purchase.setReceiptIdempotencyKey(idempotencyKey);
        if (fullyReceived && purchase.getInvoice() == null) {
            Invoice invoice = new Invoice(); invoice.setInvoiceName("INV-P-" + purchaseId);
            invoice.setInvoiceDescription("Supplier invoice for purchase " + purchaseId);
            invoice.setAmount(purchase.getTotalAmount()); invoice.setPaidAmount(0L);
            invoice.setTaxAmount(purchase.getTaxAmount()); invoice.setNetAmount(purchase.getTotalAmount()-purchase.getTaxAmount());
            invoice.setCurrency(purchase.getCurrency()); invoice.setInvoiceType(InvoiceType.PURCHASE);
            invoice.setInvoiceStatus(InvoiceStatus.CONFIRMED); invoice.setWorkflowKey("purchase:" + purchase.getIdempotencyKey());
            if (purchase.getSupplier().getContact()!=null) invoice.setBillingContacts(Set.of(purchase.getSupplier().getContact()));
            em.persist(invoice); purchase.setInvoice(invoice);
            purchase.setApprovedDate(LocalDateTime.now());
        }
        em.flush();
        return result("purchase", purchaseId, "receipt", receipt.getId(), "receivedQuantity", receivedTotal,
                "status", purchase.getStatus(), "invoiceId", purchase.getInvoice()==null?null:purchase.getInvoice().getId(),
                "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> transitionSalesOrder(Long orderId, ErpWorkflowRequests.OrderTransition request, boolean ship) {
        Order order = em.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) throw new IllegalArgumentException("Order not found: " + orderId);
        if (ship && order.getOrderStatus() == OrderStatus.SHIPPED)
            return result("order", orderId, "status", "SHIPPED", "idempotentReplay", true);
        if (!ship && order.getOrderStatus() == OrderStatus.CANCELLED)
            return result("order", orderId, "status", "CANCELLED", "idempotentReplay", true);
        if (order.getOrderStatus() != OrderStatus.PROCESSING)
            throw new IllegalStateException("Only PROCESSING orders can be shipped or cancelled");
        List<StockMovement> reservations = em.createQuery(
                "select m from StockMovement m where m.referenceType='ORDER' and m.referenceId=:id and m.movementType='SALE_RESERVATION'",
                StockMovement.class).setParameter("id", orderId.toString())
                .setLockMode(ship ? LockModeType.NONE : LockModeType.PESSIMISTIC_WRITE).getResultList();
        int index = 0;
        for (StockMovement reservation : reservations) {
            if (ship) {
                movement(reservation.getProduct(), reservation.getStore(), reservation.getQuantity(),
                        "SALE_SHIPMENT", "ORDER", orderId.toString(), request.idempotencyKey()+":ship:"+(index++));
            } else {
                ProductStockLocation stock = stockForUpdateOrCreate(reservation.getProduct(), reservation.getStore());
                long quantity = stock.getQuantity() == null ? 0 : stock.getQuantity();
                stock.setQuantity(Math.addExact(quantity, reservation.getQuantity()));
                movement(reservation.getProduct(), reservation.getStore(), reservation.getQuantity(),
                        "SALE_RESERVATION_RELEASE", "ORDER", orderId.toString(), request.idempotencyKey()+":cancel:"+(index++));
            }
        }
        order.setOrderStatus(ship ? OrderStatus.SHIPPED : OrderStatus.CANCELLED);
        em.flush();
        return result("order", orderId, "status", order.getOrderStatus().name(), "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> payInvoice(Long invoiceId, ErpWorkflowRequests.InvoicePayment request) {
        List<PaymentTransaction> existing = em.createQuery("select p from PaymentTransaction p where p.idempotencyKey=:key", PaymentTransaction.class)
                .setParameter("key", request.idempotencyKey()).setMaxResults(1).getResultList();
        if (!existing.isEmpty()) {
            if (existing.get(0).getInvoice()==null || !Objects.equals(existing.get(0).getInvoice().getId(), invoiceId))
                throw new IllegalArgumentException("Idempotency key was already used for a different invoice payment");
            PaymentTransaction prior = existing.get(0);
            if (!Objects.equals(prior.getAmount(), request.amount())
                    || !Objects.equals(prior.getCurrency() == null ? null : prior.getCurrency().getId(), request.currencyId())
                    || prior.getPaymentTransactionDirection() != PaymentTransactionDirection.valueOf(request.direction().toUpperCase(Locale.ROOT)))
                throw new IllegalArgumentException("Idempotency key was already used with a different payment payload");
            return result("payment", prior.getId(), "idempotentReplay", true);
        }
        Invoice invoice = em.find(Invoice.class, invoiceId, LockModeType.PESSIMISTIC_WRITE);
        if (invoice == null) throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        if (invoice.getInvoiceStatus()==InvoiceStatus.CANCELLED || invoice.getInvoiceStatus()==InvoiceStatus.DRAFT)
            throw new IllegalStateException("Only confirmed or posted invoices can be settled");
        long amount = invoice.getAmount()==null?0:invoice.getAmount();
        long paid = invoice.getPaidAmount()==null?0:invoice.getPaidAmount();
        if (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE) throw new IllegalStateException("Credit notes cannot receive payments");
        long credited = invoice.getCreditedAmount()==null?0L:invoice.getCreditedAmount();
        long outstanding = amount-paid-credited;
        if (request.amount()>outstanding) throw new IllegalArgumentException("Payment exceeds outstanding invoice balance " + outstanding);
        Currency currency=required(Currency.class,request.currencyId());
        if (invoice.getCurrency() == null || !Objects.equals(invoice.getCurrency().getId(), currency.getId()))
            throw new IllegalArgumentException("Payment currency must match the invoice currency");
        Contact contact=required(Contact.class,request.contactId());
        PaymentTransactionDirection direction=PaymentTransactionDirection.valueOf(request.direction().toUpperCase(Locale.ROOT));
        if ((invoice.getInvoiceType()==InvoiceType.SALE && direction!=PaymentTransactionDirection.SALES)
                || (invoice.getInvoiceType()==InvoiceType.PURCHASE && direction!=PaymentTransactionDirection.PURCHASE))
            throw new IllegalArgumentException("Payment direction does not match invoice type");
        PaymentTransaction payment=new PaymentTransaction(); payment.setInvoice(invoice); payment.setContact(contact);
        payment.setCurrency(currency); payment.setAmount(request.amount()); payment.setPayment(request.amount());
        payment.setPaymentTransactionDirection(direction); payment.setStatus("COMPLETED"); payment.setPaymentDate(LocalDate.now()); payment.setReferenceId(request.reference());
        payment.setDescription(request.description()); payment.setIdempotencyKey(request.idempotencyKey());
        em.persist(payment); invoice.setPaidAmount(Math.addExact(paid,request.amount()));
        long settled = Math.addExact(invoice.getPaidAmount(), credited);
        invoice.setInvoiceStatus(settled >= amount ? (credited > 0 ? InvoiceStatus.SETTLED : InvoiceStatus.PAID)
                : (credited > 0 ? InvoiceStatus.PARTIALLY_SETTLED : InvoiceStatus.PARTIALLY_PAID));
        em.flush();
        return result("payment",payment.getId(),"invoiceId",invoiceId,"paidAmount",invoice.getPaidAmount(),
                "outstandingAmount",amount-invoice.getPaidAmount(),"invoiceStatus",invoice.getInvoiceStatus().name(),"idempotentReplay",false);
    }

    @Transactional
    public Map<String,Object> importBankStatement(ErpWorkflowRequests.BankStatementImport request) {
        List<BankStatement> existing = em.createQuery("select s from BankStatement s where s.idempotencyKey=:key", BankStatement.class)
                .setParameter("key", request.idempotencyKey()).setMaxResults(1).getResultList();
        if (!existing.isEmpty()) {
            BankStatement prior = existing.get(0);
            if (!Objects.equals(prior.getStatementReference(), request.statementReference())
                    || !Objects.equals(prior.getCurrency().getId(), request.currencyId()))
                throw new IllegalArgumentException("Bank statement idempotency key was reused with a different statement");
            return result("statement", prior.getId(), "status", prior.getStatus(), "idempotentReplay", true);
        }
        if (request.periodEnd().isBefore(request.periodStart())) throw new IllegalArgumentException("Statement period end precedes start");
        Currency currency = required(Currency.class, request.currencyId());
        BankStatement statement = new BankStatement();
        statement.setIdempotencyKey(request.idempotencyKey()); statement.setStatementReference(request.statementReference());
        statement.setCurrency(currency); statement.setPeriodStart(request.periodStart()); statement.setPeriodEnd(request.periodEnd());
        em.persist(statement); em.flush();
        Set<String> externalIds = new HashSet<>();
        int matched = 0;
        for (ErpWorkflowRequests.BankStatementLineInput input : request.lines()) {
            if (!externalIds.add(input.externalLineId())) throw new IllegalArgumentException("Duplicate external bank line id: " + input.externalLineId());
            if (input.transactionDate().isBefore(request.periodStart()) || input.transactionDate().isAfter(request.periodEnd()))
                throw new IllegalArgumentException("Bank line date is outside statement period");
            if (input.amount() == 0) throw new IllegalArgumentException("Bank statement line amount cannot be zero");
            BankStatementLine line = new BankStatementLine();
            line.setStatement(statement); line.setExternalLineId(input.externalLineId()); line.setTransactionDate(input.transactionDate());
            line.setAmount(input.amount()); line.setReference(input.reference()); line.setDescription(input.description());
            em.persist(line); statement.getLines().add(line);
            if (input.reference() != null && !input.reference().isBlank()) {
                PaymentTransactionDirection direction = input.amount() > 0 ? PaymentTransactionDirection.SALES : PaymentTransactionDirection.PURCHASE;
                List<PaymentTransaction> candidates = em.createQuery(
                        "select p from PaymentTransaction p where p.currency.id=:currency and p.amount=:amount and p.referenceId=:reference and p.paymentTransactionDirection=:direction and p.status='COMPLETED' and p.paymentDate between :start and :end",
                        PaymentTransaction.class).setParameter("currency", currency.getId())
                        .setParameter("amount", Math.abs(input.amount())).setParameter("reference", input.reference())
                        .setParameter("direction", direction).setParameter("start", request.periodStart()).setParameter("end", request.periodEnd())
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(2).getResultList();
                if (candidates.size() == 1) {
                    Long paymentId = candidates.get(0).getId();
                    Long alreadyMatched = em.createQuery("select count(l) from BankStatementLine l where l.matchedPayment.id=:id", Long.class)
                            .setParameter("id", paymentId).getSingleResult();
                    if (alreadyMatched == 0) {
                        line.setMatchedPayment(candidates.get(0)); line.setReconciliationStatus("MATCHED");
                        line.setReconciledAt(java.time.LocalDateTime.now()); matched++;
                    } else line.setReconciliationStatus("ALREADY_RECONCILED");
                } else if (candidates.size() > 1) line.setReconciliationStatus("AMBIGUOUS");
            }
        }
        statement.setStatus(matched == statement.getLines().size() ? "RECONCILED" : (matched > 0 ? "PARTIALLY_RECONCILED" : "IMPORTED"));
        em.flush();
        return result("statement", statement.getId(), "lineCount", statement.getLines().size(),
                "matchedCount", matched, "status", statement.getStatus(), "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> reconcileBankStatementLine(Long lineId, Long paymentId) {
        BankStatementLine line = em.find(BankStatementLine.class, lineId, LockModeType.PESSIMISTIC_WRITE);
        if (line == null) throw new IllegalArgumentException("Bank statement line not found: " + lineId);
        if (line.getMatchedPayment() != null) {
            if (Objects.equals(line.getMatchedPayment().getId(), paymentId))
                return result("line", lineId, "payment", paymentId, "idempotentReplay", true);
            throw new IllegalStateException("Bank statement line is already reconciled");
        }
        PaymentTransaction payment = em.find(PaymentTransaction.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (payment == null) throw new IllegalArgumentException("Payment not found: " + paymentId);
        BankStatement statement = line.getStatement();
        long expectedAmount = line.getAmount() > 0 ? line.getAmount() : Math.negateExact(line.getAmount());
        PaymentTransactionDirection expectedDirection = line.getAmount() > 0 ? PaymentTransactionDirection.SALES : PaymentTransactionDirection.PURCHASE;
        if (!Objects.equals(payment.getCurrency().getId(), statement.getCurrency().getId())
                || !Objects.equals(payment.getAmount(), expectedAmount)
                || payment.getPaymentTransactionDirection() != expectedDirection
                || !Objects.equals(payment.getReferenceId(), line.getReference())
                || payment.getPaymentDate() == null || payment.getPaymentDate().isBefore(statement.getPeriodStart())
                || payment.getPaymentDate().isAfter(statement.getPeriodEnd()))
            throw new IllegalArgumentException("Payment does not match bank line amount, currency, direction, reference and date");
        Long count = em.createQuery("select count(l) from BankStatementLine l where l.matchedPayment.id=:id", Long.class)
                .setParameter("id", paymentId).getSingleResult();
        if (count > 0) throw new IllegalStateException("Payment has already been reconciled");
        line.setMatchedPayment(payment); line.setReconciliationStatus("MATCHED"); line.setReconciledAt(java.time.LocalDateTime.now());
        long unmatched = em.createQuery("select count(l) from BankStatementLine l where l.statement.id=:id and l.matchedPayment is null", Long.class)
                .setParameter("id", statement.getId()).getSingleResult();
        statement.setStatus(unmatched <= 1 ? "RECONCILED" : "PARTIALLY_RECONCILED");
        em.flush();
        return result("line", lineId, "payment", paymentId, "statementStatus", statement.getStatus(), "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> createCreditNote(Long invoiceId, ErpWorkflowRequests.CreditNote request) {
        List<Invoice> prior = em.createQuery("select i from Invoice i where i.workflowKey=:key", Invoice.class)
                .setParameter("key", "credit-note:" + request.idempotencyKey()).setMaxResults(1).getResultList();
        if (!prior.isEmpty()) {
            Invoice note = prior.get(0);
            if (!Objects.equals(note.getOriginalInvoice().getId(), invoiceId) || !Objects.equals(note.getAmount(), request.amount()))
                throw new IllegalArgumentException("Credit note idempotency key was reused with a different payload");
            return result("creditNote", note.getId(), "originalInvoice", invoiceId, "idempotentReplay", true);
        }
        Invoice original = em.find(Invoice.class, invoiceId, LockModeType.PESSIMISTIC_WRITE);
        if (original == null) throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        if (original.getInvoiceType() == InvoiceType.CREDIT_NOTE || original.getInvoiceStatus() == InvoiceStatus.DRAFT
                || original.getInvoiceStatus() == InvoiceStatus.CANCELLED)
            throw new IllegalStateException("Credit notes require an active sales or purchase invoice");
        long amount = original.getAmount() == null ? 0L : original.getAmount();
        long paid = original.getPaidAmount() == null ? 0L : original.getPaidAmount();
        long credited = original.getCreditedAmount() == null ? 0L : original.getCreditedAmount();
        long outstanding = amount - paid - credited;
        if (request.amount() > outstanding) throw new IllegalArgumentException("Credit note exceeds outstanding invoice balance " + outstanding);
        Invoice note = new Invoice();
        note.setInvoiceName("CN-" + original.getId() + "-" + UUID.randomUUID().toString().substring(0, 8));
        note.setInvoiceDescription("Credit note for invoice " + original.getInvoiceName());
        note.setAmount(request.amount()); note.setPaidAmount(0L); note.setCreditedAmount(0L);
        long originalNet = original.getNetAmount()==null?amount:original.getNetAmount();
        long originalTax = original.getTaxAmount()==null?0L:original.getTaxAmount();
        long noteTax = amount == 0 ? 0L : BigDecimal.valueOf(request.amount()).multiply(BigDecimal.valueOf(originalTax))
                .divide(BigDecimal.valueOf(amount), 0, java.math.RoundingMode.HALF_UP).longValueExact();
        note.setTaxAmount(noteTax); note.setNetAmount(request.amount()-noteTax);
        note.setCurrency(original.getCurrency()); note.setInvoiceType(InvoiceType.CREDIT_NOTE);
        note.setInvoiceStatus(InvoiceStatus.CONFIRMED); note.setOriginalInvoice(original);
        note.setCreditNoteReason(request.reason()); note.setWorkflowKey("credit-note:" + request.idempotencyKey());
        note.setBillingContacts(original.getBillingContacts()); note.setShippingContacts(original.getShippingContacts());
        em.persist(note);
        original.setCreditedAmount(Math.addExact(credited, request.amount()));
        long totalSettled = Math.addExact(paid, original.getCreditedAmount());
        original.setInvoiceStatus(totalSettled >= amount
                ? (paid > 0 ? InvoiceStatus.SETTLED : InvoiceStatus.CREDITED)
                : (paid > 0 ? InvoiceStatus.PARTIALLY_SETTLED : InvoiceStatus.PARTIALLY_CREDITED));
        em.flush();
        return result("creditNote", note.getId(), "originalInvoice", invoiceId,
                "creditedAmount", original.getCreditedAmount(), "outstandingAmount", amount-totalSettled,
                "status", original.getInvoiceStatus().name(), "idempotentReplay", false);
    }

    @Transactional
    public Map<String,Object> postInvoiceToLedger(Long invoiceId, ErpWorkflowRequests.DocumentPosting request) {
        Invoice invoice = em.find(Invoice.class, invoiceId, LockModeType.PESSIMISTIC_WRITE);
        if (invoice == null) throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        if (invoice.getInvoiceStatus() == InvoiceStatus.DRAFT || invoice.getInvoiceStatus() == InvoiceStatus.CANCELLED)
            throw new IllegalStateException("Only confirmed invoices can be posted to the ledger");
        long amount = invoice.getAmount() == null ? 0L : invoice.getAmount();
        if (amount <= 0) throw new IllegalStateException("Invoice amount must be positive before ledger posting");
        boolean sale = invoice.getInvoiceType() == InvoiceType.SALE || (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE && invoice.getOriginalInvoice() != null && invoice.getOriginalInvoice().getInvoiceType() == InvoiceType.SALE);
        String entryId = (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE ? "CN-" : "INV-") + invoiceId;
        List<JournalEntry> alreadyPosted = em.createQuery("select j from JournalEntry j where j.entryId=:entryId", JournalEntry.class).setParameter("entryId", entryId).setMaxResults(1).getResultList();
        if (!alreadyPosted.isEmpty()) return result("invoice", invoiceId, "journalEntry", alreadyPosted.get(0).getId(), "idempotentReplay", true);
        JournalPost journal = new JournalPost(request.idempotencyKey(), request.fiscalYearId(), request.fiscalPeriodId(),
                request.companyId(), entryId, request.entryDate(), request.description(),
                invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE ? "REVERSAL" : (sale ? "SALES" : "PURCHASE"),
                List.of(new JournalLine(request.debitAccountId(), BigDecimal.valueOf(amount), BigDecimal.ZERO, request.description()),
                        new JournalLine(request.creditAccountId(), BigDecimal.ZERO, BigDecimal.valueOf(amount), request.description())));
        Map<String,Object> posted = postJournal(journal);
        invoice.setLedgerJournalEntry(required(JournalEntry.class, ((Number) posted.get("journalEntry")).longValue()));
        invoice.setInvoiceStatus(InvoiceStatus.POSTED);
        em.flush();
        return result("invoice", invoiceId, "journalEntry", posted.get("journalEntry"),
                "status", invoice.getInvoiceStatus().name(), "idempotentReplay", posted.get("idempotentReplay"));
    }

    @Transactional
    public Map<String,Object> postInvoiceAutomatically(Long invoiceId, ErpWorkflowRequests.AutoDocumentPosting request) {
        Invoice invoice = em.find(Invoice.class, invoiceId, LockModeType.PESSIMISTIC_WRITE);
        if (invoice == null) throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        String sourceId = (invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE ? "CN-" : "INV-") + invoiceId;
        List<JournalEntry> sourceEntries = em.createQuery("select j from JournalEntry j where j.entryId=:sourceId", JournalEntry.class)
                .setParameter("sourceId", sourceId).setMaxResults(1).getResultList();
        if (!sourceEntries.isEmpty()) return result("invoice", invoiceId, "journalEntry", sourceEntries.get(0).getId(), "idempotentReplay", true);
        if (invoice.getInvoiceStatus() != InvoiceStatus.CONFIRMED)
            throw new IllegalStateException("Only confirmed, unposted invoices can be posted automatically");
        String eventType;
        boolean creditNote = invoice.getInvoiceType() == InvoiceType.CREDIT_NOTE;
        InvoiceType sourceType = creditNote && invoice.getOriginalInvoice()!=null ? invoice.getOriginalInvoice().getInvoiceType() : invoice.getInvoiceType();
        if (sourceType == InvoiceType.SALE) eventType = creditNote ? "SALES_CREDIT_NOTE" : "SALES_INVOICE";
        else if (sourceType == InvoiceType.PURCHASE) eventType = creditNote ? "PURCHASE_CREDIT_NOTE" : "PURCHASE_INVOICE";
        else throw new IllegalStateException("Unsupported invoice type for account determination");
        List<AccountDeterminationRule> rules;
        if (request.companyId() == null) {
            rules = em.createQuery("select r from AccountDeterminationRule r where r.active=true and r.eventType=:event and r.company is null order by r.priority asc", AccountDeterminationRule.class)
                    .setParameter("event", eventType).setMaxResults(1).getResultList();
        } else {
            rules = em.createQuery("select r from AccountDeterminationRule r where r.active=true and r.eventType=:event and (r.company.id=:company or r.company is null) order by case when r.company.id=:company then 0 else 1 end, r.priority asc", AccountDeterminationRule.class)
                    .setParameter("event", eventType).setParameter("company", request.companyId()).setMaxResults(1).getResultList();
        }
        if (rules.isEmpty()) throw new IllegalStateException("No active account determination rule configured for " + eventType);
        AccountDeterminationRule rule = rules.get(0);
        Account debit = rule.getDebitAccount(), credit = rule.getCreditAccount(), tax = rule.getTaxAccount();
        long gross = invoice.getAmount()==null?0L:invoice.getAmount();
        long taxAmount = invoice.getTaxAmount()==null?0L:invoice.getTaxAmount();
        long net = invoice.getNetAmount()==null?gross-taxAmount:invoice.getNetAmount();
        if (gross <= 0 || net < 0 || taxAmount < 0 || Math.addExact(net,taxAmount)!=gross)
            throw new IllegalStateException("Invoice net, tax and gross totals are inconsistent");
        List<ErpWorkflowRequests.JournalLine> lines = new ArrayList<>();
        if (!creditNote && sourceType == InvoiceType.SALE) {
            lines.add(new ErpWorkflowRequests.JournalLine(debit.getId(), BigDecimal.valueOf(gross), BigDecimal.ZERO, "Accounts receivable"));
            lines.add(new ErpWorkflowRequests.JournalLine(credit.getId(), BigDecimal.ZERO, BigDecimal.valueOf(net), "Sales revenue"));
            if (taxAmount > 0) {
                if (tax == null) throw new IllegalStateException("Sales tax account is missing from posting rule");
                lines.add(new ErpWorkflowRequests.JournalLine(tax.getId(), BigDecimal.ZERO, BigDecimal.valueOf(taxAmount), "Output tax"));
            }
        } else if (!creditNote) {
            lines.add(new ErpWorkflowRequests.JournalLine(debit.getId(), BigDecimal.valueOf(net), BigDecimal.ZERO, "Purchase expense or inventory"));
            if (taxAmount > 0) {
                if (tax == null) throw new IllegalStateException("Input tax account is missing from posting rule");
                lines.add(new ErpWorkflowRequests.JournalLine(tax.getId(), BigDecimal.valueOf(taxAmount), BigDecimal.ZERO, "Input tax"));
            }
            lines.add(new ErpWorkflowRequests.JournalLine(credit.getId(), BigDecimal.ZERO, BigDecimal.valueOf(gross), "Accounts payable"));
        } else if (sourceType == InvoiceType.SALE) {
            lines.add(new ErpWorkflowRequests.JournalLine(credit.getId(), BigDecimal.valueOf(net), BigDecimal.ZERO, "Sales returns"));
            if (taxAmount > 0) {
                if (tax == null) throw new IllegalStateException("Sales tax account is missing from posting rule");
                lines.add(new ErpWorkflowRequests.JournalLine(tax.getId(), BigDecimal.valueOf(taxAmount), BigDecimal.ZERO, "Output tax reversal"));
            }
            lines.add(new ErpWorkflowRequests.JournalLine(debit.getId(), BigDecimal.ZERO, BigDecimal.valueOf(gross), "Accounts receivable credit"));
        } else {
            lines.add(new ErpWorkflowRequests.JournalLine(credit.getId(), BigDecimal.valueOf(gross), BigDecimal.ZERO, "Accounts payable credit"));
            lines.add(new ErpWorkflowRequests.JournalLine(debit.getId(), BigDecimal.ZERO, BigDecimal.valueOf(net), "Purchase return"));
            if (taxAmount > 0) {
                if (tax == null) throw new IllegalStateException("Input tax account is missing from posting rule");
                lines.add(new ErpWorkflowRequests.JournalLine(tax.getId(), BigDecimal.ZERO, BigDecimal.valueOf(taxAmount), "Input tax reversal"));
            }
        }
        JournalPost journal = new JournalPost(request.idempotencyKey(), request.fiscalYearId(), request.fiscalPeriodId(),
                request.companyId(), sourceId, request.entryDate(), request.description(),
                creditNote ? "REVERSAL" : (sourceType == InvoiceType.SALE ? "SALES" : "PURCHASE"), lines);
        Map<String,Object> posted = postJournal(journal);
        invoice.setInvoiceStatus(InvoiceStatus.POSTED);
        em.flush();
        return result("invoice", invoiceId, "journalEntry", posted.get("journalEntry"), "eventType", eventType,
                "status", "POSTED", "idempotentReplay", posted.get("idempotentReplay"));
    }

    @Transactional
    public Map<String,Object> postPaymentToLedger(Long paymentId, ErpWorkflowRequests.DocumentPosting request) {
        PaymentTransaction payment = em.find(PaymentTransaction.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (payment == null) throw new IllegalArgumentException("Payment not found: " + paymentId);
        if (!"COMPLETED".equalsIgnoreCase(payment.getStatus()))
            throw new IllegalStateException("Only completed payments can be posted");
        long amount = payment.getAmount() == null ? 0L : payment.getAmount();
        if (amount <= 0) throw new IllegalStateException("Payment amount must be positive");
        String entryId = "PAY-" + paymentId;
        List<JournalEntry> alreadyPosted = em.createQuery("select j from JournalEntry j where j.entryId=:entryId", JournalEntry.class).setParameter("entryId", entryId).setMaxResults(1).getResultList();
        if (!alreadyPosted.isEmpty()) return result("payment", paymentId, "journalEntry", alreadyPosted.get(0).getId(), "idempotentReplay", true);
        JournalPost journal = new JournalPost(request.idempotencyKey(), request.fiscalYearId(), request.fiscalPeriodId(),
                request.companyId(), entryId, request.entryDate(), request.description(), "PAYMENT",
                List.of(new JournalLine(request.debitAccountId(), BigDecimal.valueOf(amount), BigDecimal.ZERO, request.description()),
                        new JournalLine(request.creditAccountId(), BigDecimal.ZERO, BigDecimal.valueOf(amount), request.description())));
        Map<String,Object> posted = postJournal(journal);
        payment.setLedgerJournalEntry(required(JournalEntry.class, ((Number) posted.get("journalEntry")).longValue()));
        em.flush();
        return result("payment", paymentId, "journalEntry", posted.get("journalEntry"),
                "status", "POSTED", "idempotentReplay", posted.get("idempotentReplay"));
    }

    @Transactional
    public Map<String,Object> postPaymentAutomatically(Long paymentId, ErpWorkflowRequests.AutoDocumentPosting request) {
        PaymentTransaction payment = em.find(PaymentTransaction.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (payment == null) throw new IllegalArgumentException("Payment not found: " + paymentId);
        if (payment.getLedgerJournalEntry() != null)
            return result("payment", paymentId, "journalEntry", payment.getLedgerJournalEntry().getId(), "idempotentReplay", true);
        if (!"COMPLETED".equalsIgnoreCase(payment.getStatus())) throw new IllegalStateException("Only completed payments can be posted");
        String eventType = payment.getPaymentTransactionDirection() == PaymentTransactionDirection.SALES ? "CUSTOMER_RECEIPT" : "SUPPLIER_PAYMENT";
        List<AccountDeterminationRule> rules;
        if (request.companyId() == null) {
            rules = em.createQuery("select r from AccountDeterminationRule r where r.active=true and r.eventType=:event and r.company is null order by r.priority asc", AccountDeterminationRule.class)
                    .setParameter("event", eventType).setMaxResults(1).getResultList();
        } else {
            rules = em.createQuery("select r from AccountDeterminationRule r where r.active=true and r.eventType=:event and (r.company.id=:company or r.company is null) order by case when r.company.id=:company then 0 else 1 end, r.priority asc", AccountDeterminationRule.class)
                    .setParameter("event", eventType).setParameter("company", request.companyId()).setMaxResults(1).getResultList();
        }
        if (rules.isEmpty()) throw new IllegalStateException("No active account determination rule configured for " + eventType);
        AccountDeterminationRule rule = rules.get(0);
        long amount = payment.getAmount()==null?0L:payment.getAmount();
        if (amount <= 0) throw new IllegalStateException("Payment amount must be positive");
        JournalPost journal = new JournalPost(request.idempotencyKey(), request.fiscalYearId(), request.fiscalPeriodId(),
                request.companyId(), "PAY-" + paymentId, request.entryDate(), request.description(), "PAYMENT",
                List.of(new JournalLine(rule.getDebitAccount().getId(), BigDecimal.valueOf(amount), BigDecimal.ZERO, "Bank/cash or payable"),
                        new JournalLine(rule.getCreditAccount().getId(), BigDecimal.ZERO, BigDecimal.valueOf(amount), "Receivable or bank/cash")));
        Map<String,Object> posted = postJournal(journal);
        payment.setLedgerJournalEntry(required(JournalEntry.class, ((Number) posted.get("journalEntry")).longValue()));
        em.flush();
        return result("payment", paymentId, "journalEntry", posted.get("journalEntry"), "eventType", eventType,
                "status", "POSTED", "idempotentReplay", posted.get("idempotentReplay"));
    }

    @Transactional
    public Map<String,Object> postJournal(ErpWorkflowRequests.JournalPost request) {
        List<JournalEntry> existing=em.createQuery("select j from JournalEntry j where j.idempotencyKey=:key",JournalEntry.class)
                .setParameter("key",request.idempotencyKey()).setMaxResults(1).getResultList();
        if(!existing.isEmpty()) {
            JournalEntry prior = existing.get(0);
            if (!Objects.equals(prior.getEntryId(), request.entryId())
                    || !Objects.equals(prior.getFiscalYear().getId(), request.fiscalYearId())
                    || !Objects.equals(prior.getFiscalPeriod().getId(), request.fiscalPeriodId()))
                throw new IllegalArgumentException("Idempotency key was already used for a different journal");
            return result("journalEntry",prior.getId(),"idempotentReplay",true);
        }
        FiscalYear year=required(FiscalYear.class,request.fiscalYearId());
        FiscalPeriod period=required(FiscalPeriod.class,request.fiscalPeriodId());
        if (year.isClosed() || period.isClosed()) throw new IllegalStateException("Cannot post into a closed fiscal year or period");
        if (period.getFiscalYear()==null || !Objects.equals(period.getFiscalYear().getId(),year.getId()))
            throw new IllegalArgumentException("Fiscal period does not belong to the selected fiscal year");
        if (year.getStartDate()==null || year.getEndDate()==null || period.getStartDate()==null || period.getEndDate()==null
                || request.entryDate().isBefore(year.getStartDate()) || request.entryDate().isAfter(year.getEndDate())
                || request.entryDate().isBefore(period.getStartDate()) || request.entryDate().isAfter(period.getEndDate()))
            throw new IllegalArgumentException("Journal date is outside the selected fiscal period");
        if (request.lines().size()<2) throw new IllegalArgumentException("A posted journal requires at least two lines");
        if(request.companyId()!=null) required(Contact.class,request.companyId());
        BigDecimal debit=BigDecimal.ZERO,credit=BigDecimal.ZERO;
        for(var line:request.lines()) {
            if(line.debit().signum()<0||line.credit().signum()<0||line.debit().signum()>0&&line.credit().signum()>0)
                throw new IllegalArgumentException("Each journal line must have one non-negative debit or credit amount");
            if(line.debit().signum()==0&&line.credit().signum()==0) throw new IllegalArgumentException("Empty journal line");
            Account account=required(Account.class,line.accountId());
            if(account.isBlocked()||!account.isPostingAllowed()) throw new IllegalStateException("Account is blocked or posting is not allowed: "+account.getCode());
            debit=debit.add(line.debit()); credit=credit.add(line.credit());
        }
        if(debit.compareTo(credit)!=0) throw new IllegalArgumentException("Journal is not balanced: debit="+debit+", credit="+credit);
        JournalEntry entry=new JournalEntry(); entry.setCompany(request.companyId()==null?null:required(Contact.class,request.companyId()));
        entry.setFiscalYear(year); entry.setFiscalPeriod(period); entry.setEntryId(request.entryId());
        entry.setEntryDate(request.entryDate()); entry.setDescription(request.description());
        entry.setEntryType(JournalEntryType.valueOf(request.entryType().toUpperCase(Locale.ROOT)));
        entry.setStatus(JournalEntryStatus.POSTED); entry.setIdempotencyKey(request.idempotencyKey());
        em.persist(entry); em.flush();
        for(var input:request.lines()) {
            JournalEntryLine line=new JournalEntryLine(); line.setJournalEntry(entry); line.setAccount(required(Account.class,input.accountId()));
            line.setDebit(input.debit()); line.setCredit(input.credit()); line.setDescription(input.description());
            line.setEntryDate(request.entryDate()); line.setFiscalYear(year); line.setFiscalPeriod(period);
            line.setJournalEntryStatus(JournalEntryStatus.POSTED); entry.getLines().add(line);
        }
        em.flush();
        return result("journalEntry",entry.getId(),"debit",debit,"credit",credit,"status","POSTED","idempotentReplay",false);
    }

    private ProductStockLocation stockForUpdate(Long productId, Long storeId) {
        if (em.find(Store.class, storeId, LockModeType.PESSIMISTIC_WRITE)==null) throw new IllegalArgumentException("Store not found: "+storeId);
        List<ProductStockLocation> rows=em.createQuery("select s from ProductStockLocation s where s.product.id=:productId and s.store.id=:storeId order by s.id",ProductStockLocation.class)
                .setParameter("productId",productId).setParameter("storeId",storeId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
        if(rows.isEmpty()) throw new IllegalStateException("No stock balance exists for product "+productId+" in store "+storeId);
        return rows.get(0);
    }
    private ProductStockLocation stockForUpdateOrCreate(Product product, Store store) {
        if (em.find(Store.class, store.getId(), LockModeType.PESSIMISTIC_WRITE)==null) throw new IllegalArgumentException("Store not found: "+store.getId());
        List<ProductStockLocation> rows=em.createQuery("select s from ProductStockLocation s where s.product.id=:productId and s.store.id=:storeId order by s.id",ProductStockLocation.class)
                .setParameter("productId",product.getId()).setParameter("storeId",store.getId()).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
        if(!rows.isEmpty()) return rows.get(0);
        ProductStockLocation stock=new ProductStockLocation(); stock.setProduct(product); stock.setStore(store); stock.setQuantity(0L); em.persist(stock); return stock;
    }
    private void movement(Product product, Store store, long quantity, String type, String refType, String refId, String key) {
        StockMovement m=new StockMovement(); m.setProduct(product); m.setStore(store); m.setQuantity(quantity);
        m.setMovementType(type); m.setReferenceType(refType); m.setReferenceId(refId); m.setIdempotencyKey(key); m.setOccurredAt(Instant.now()); em.persist(m);
    }
    private TaxCalculation calculateTax(long netAmount, Long taxRateId) {
        if (taxRateId == null) return new TaxCalculation(null, 0L);
        com.erplancers.erp.tax.domain.TaxRate rate = required(com.erplancers.erp.tax.domain.TaxRate.class, taxRateId);
        float percentage = rate.getRate();
        if (!Float.isFinite(percentage) || percentage < 0f || percentage > 100f)
            throw new IllegalStateException("Tax rate must be between 0 and 100 percent");
        long tax = BigDecimal.valueOf(netAmount).multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP).longValueExact();
        return new TaxCalculation(rate, tax);
    }
    private record TaxCalculation(com.erplancers.erp.tax.domain.TaxRate rate, long amount) {}

    private <T> T required(Class<T> type, Long id) {
        T entity=em.find(type,id); if(entity==null) throw new IllegalArgumentException(type.getSimpleName()+" not found: "+id); return entity;
    }
    private Map<String,Object> result(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<>(); for(int i=0;i<pairs.length;i+=2) result.put(String.valueOf(pairs[i]),pairs[i+1]); return result;
    }
}
