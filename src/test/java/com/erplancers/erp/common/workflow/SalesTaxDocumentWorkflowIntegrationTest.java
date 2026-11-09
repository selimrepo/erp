package com.erplancers.erp.common.workflow;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.procurement.domain.Supplier;
import com.erplancers.erp.procurement.domain.Purchase;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.stock.domain.ProductStockLocation;
import com.erplancers.erp.inventory.stock.domain.StockMovement;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.sales.order.domain.Order;
import com.erplancers.erp.sales.order.domain.OrderLine;
import com.erplancers.erp.tax.domain.TaxPolicy;
import com.erplancers.erp.tax.service.TaxPolicyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ErpWorkflowService.class, TaxPolicyService.class})
class SalesTaxDocumentWorkflowIntegrationTest {
    @Autowired EntityManager entityManager;
    @Autowired ErpWorkflowService workflows;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void createsTaxedSalesOrderInvoiceAndStockMovementAtomically() {
        Fixture fixture = seedFixture();
        ErpWorkflowRequests.SalesOrder request = salesRequest(fixture, "sale-tax-success", null);

        Map<String, Object> result = workflows.createSalesOrder(request);

        inNewTransaction(() -> {
            entityManager.clear();
            ProductStockLocation stock = entityManager.find(ProductStockLocation.class, fixture.stockId());
            Invoice invoice = entityManager.find(Invoice.class, ((Number) result.get("invoiceId")).longValue());
            Order order = entityManager.find(Order.class, ((Number) result.get("order")).longValue());
            assertThat(stock.getQuantity()).isEqualTo(8L);
            assertThat(invoice.getAmount()).isEqualTo(2_400L);
            assertThat(invoice.getNetAmount()).isEqualTo(2_000L);
            assertThat(invoice.getTaxAmount()).isEqualTo(400L);
            assertThat(order.getLines()).hasSize(1);
            OrderLine line = order.getLines().iterator().next();
            assertThat(line.getTaxPolicy()).isNotNull();
            assertThat(line.getTaxPolicy().getId()).isEqualTo(fixture.policyId());
            assertThat(entityManager.createQuery(
                    "select count(m) from StockMovement m where m.idempotencyKey=:key", Long.class)
                    .setParameter("key", "sale-tax-success:sale:0").getSingleResult()).isEqualTo(1L);
            return null;
        });
    }

    @Test
    void rollsBackStockOrderAndInvoiceWhenAtomicLedgerPostingFails() {
        Fixture fixture = seedFixture();
        ErpWorkflowRequests.DocumentPosting invalidPosting = new ErpWorkflowRequests.DocumentPosting(
                "sale-tax-rollback-ledger", 999_991L, 999_992L, null, LocalDate.now(),
                "Expected rollback", 999_993L, 999_994L, 999_995L);
        ErpWorkflowRequests.SalesOrder request = salesRequest(fixture, "sale-tax-rollback", invalidPosting);

        assertThatThrownBy(() -> workflows.createSalesOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");

        inNewTransaction(() -> {
            entityManager.clear();
            ProductStockLocation stock = entityManager.find(ProductStockLocation.class, fixture.stockId());
            assertThat(stock.getQuantity()).isEqualTo(10L);
            assertThat(entityManager.createQuery(
                    "select count(o) from Order o where o.idempotencyKey=:key", Long.class)
                    .setParameter("key", "sale-tax-rollback").getSingleResult()).isZero();
            assertThat(entityManager.createQuery(
                    "select count(i) from Invoice i where i.workflowKey=:key", Long.class)
                    .setParameter("key", "sales:sale-tax-rollback").getSingleResult()).isZero();
            assertThat(entityManager.createQuery(
                    "select count(m) from StockMovement m where m.idempotencyKey like :key", Long.class)
                    .setParameter("key", "sale-tax-rollback:%").getSingleResult()).isZero();
            return null;
        });
    }

    @Test
    void rollsBackPurchaseReceiptStockInvoiceAndLedgerWhenPostingFails() {
        Fixture fixture = seedFixture();
        Map<String,Object> created = workflows.createPurchase(new ErpWorkflowRequests.PurchaseOrder(
                "purchase-tax-rollback", fixture.supplierId(), fixture.storeId(), fixture.currencyId(),
                List.of(new ErpWorkflowRequests.PurchaseLine(fixture.productId(), 2L, 1_000L)),
                "Purchase tax rollback"));
        Long purchaseId = ((Number) created.get("purchase")).longValue();
        ErpWorkflowRequests.DocumentPosting invalidPosting = new ErpWorkflowRequests.DocumentPosting(
                "purchase-tax-rollback-ledger", 999_991L, 999_992L, null, LocalDate.now(),
                "Expected purchase rollback", 999_993L, 999_994L, 999_995L);

        assertThatThrownBy(() -> workflows.receivePurchase(purchaseId,
                new ErpWorkflowRequests.ReceivePurchase("purchase-tax-receipt-rollback", invalidPosting),
                fixture.storeId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");

        inNewTransaction(() -> {
            entityManager.clear();
            ProductStockLocation stock = entityManager.find(ProductStockLocation.class, fixture.stockId());
            Purchase purchase = entityManager.find(Purchase.class, purchaseId);
            assertThat(stock.getQuantity()).isEqualTo(10L);
            assertThat(purchase.getStatus()).isEqualTo("ORDERED");
            assertThat(purchase.getInvoice()).isNull();
            assertThat(entityManager.createQuery(
                    "select count(r) from PurchaseReceipt r where r.idempotencyKey=:key", Long.class)
                    .setParameter("key", "purchase-tax-receipt-rollback").getSingleResult()).isZero();
            assertThat(entityManager.createQuery(
                    "select count(m) from StockMovement m where m.idempotencyKey like :key", Long.class)
                    .setParameter("key", "purchase-tax-receipt-rollback:%").getSingleResult()).isZero();
            return null;
        });
    }

    private Fixture seedFixture() {
        return inNewTransaction(() -> {
            Contact contact = new Contact();
            contact.setName("Integration buyer");
            entityManager.persist(contact);

            Supplier supplier = new Supplier(); supplier.setContact(contact); supplier.setName("Integration supplier");
            entityManager.persist(supplier);

            Customer customer = new Customer();
            customer.setContact(contact);
            entityManager.persist(customer);

            Currency currency = new Currency();
            currency.setTitle("Turkish Lira");
            currency.setCurrency("TRY");
            entityManager.persist(currency);

            Store store = new Store();
            entityManager.persist(store);

            Product product = new Product();
            product.setProductName("Tax integration product");
            entityManager.persist(product);

            ProductStockLocation stock = new ProductStockLocation();
            stock.setProduct(product);
            stock.setStore(store);
            stock.setQuantity(10L);
            entityManager.persist(stock);

            TaxPolicy policy = new TaxPolicy();
            policy.setName("Integration sales VAT");
            policy.setTransactionType("SALE");
            policy.setRate(new java.math.BigDecimal("20"));
            policy.setValidFrom(LocalDate.now().minusDays(1));
            policy.setValidUntil(LocalDate.now().plusDays(1));
            policy.setPriority(1);
            policy.setActive(true);
            entityManager.persist(policy);
            TaxPolicy purchasePolicy = new TaxPolicy();
            purchasePolicy.setName("Integration purchase VAT"); purchasePolicy.setTransactionType("PURCHASE");
            purchasePolicy.setRate(new java.math.BigDecimal("10"));
            purchasePolicy.setValidFrom(LocalDate.now().minusDays(1)); purchasePolicy.setValidUntil(LocalDate.now().plusDays(1));
            purchasePolicy.setPriority(1); purchasePolicy.setActive(true);
            entityManager.persist(purchasePolicy);
            entityManager.flush();
            return new Fixture(customer.getId(), supplier.getId(), contact.getId(), currency.getId(), store.getId(),
                    product.getId(), stock.getId(), policy.getId());
        });
    }

    private ErpWorkflowRequests.SalesOrder salesRequest(Fixture fixture, String key,
                                                         ErpWorkflowRequests.DocumentPosting posting) {
        return new ErpWorkflowRequests.SalesOrder(key, fixture.customerId(), fixture.contactId(),
                fixture.currencyId(), fixture.storeId(),
                List.of(new ErpWorkflowRequests.SalesLine(fixture.productId(), 2L, 1_000L)),
                "Tax policy integration", true, posting);
    }

    private <T> T inNewTransaction(java.util.concurrent.Callable<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> {
            try {
                return action.call();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private record Fixture(Long customerId, Long supplierId, Long contactId, Long currencyId, Long storeId,
                           Long productId, Long stockId, Long policyId) {}
}
