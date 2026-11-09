package com.erplancers.erp.common.workflow;

import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.finance.domain.Invoice;
import com.erplancers.erp.finance.domain.InvoiceTaxComponent;
import com.erplancers.erp.finance.domain.InvoiceType;
import com.erplancers.erp.finance.domain.InvoiceStatus;
import com.erplancers.erp.finance.domain.CreditNoteReasonCode;
import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.accounting.domain.FiscalYear;
import com.erplancers.erp.accounting.domain.FiscalPeriod;
import com.erplancers.erp.accounting.domain.JournalEntry;
import com.erplancers.erp.accounting.domain.JournalEntryLine;
import com.erplancers.erp.tax.domain.TaxComponentType;
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
            assertThat(invoice.getTaxComponents()).hasSize(1);
            assertThat(order.getLines()).hasSize(1);
            OrderLine line = order.getLines().iterator().next();
            assertThat(line.getTaxPolicy()).isNotNull();
            assertThat(line.getTaxPolicy().getId()).isEqualTo(fixture.policyId());
            assertThat(line.getTaxComponents()).hasSize(1);
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
    void postsWithholdingAndReverseChargeAccountsAndReversesThemOnCreditNote() {
        LedgerFixture ledger = inNewTransaction(() -> {
            Account expense = account("5000", "Purchase expense");
            Account payable = account("3200", "Accounts payable");
            Account inputTax = account("1910", "Input tax");
            Account outputTax = account("3910", "Output tax");
            Account withholding = account("3600", "Withholding payable");
            FiscalYear year = new FiscalYear(); year.setFiscalYear(2026);
            year.setStartDate(LocalDate.of(2026, 1, 1)); year.setEndDate(LocalDate.of(2026, 12, 31));
            entityManager.persist(year);
            FiscalPeriod period = new FiscalPeriod(); period.setFiscalYear(year); period.setPeriodNumber(11);
            period.setName("November 2026"); period.setStartDate(LocalDate.of(2026, 11, 1));
            period.setEndDate(LocalDate.of(2026, 11, 30)); entityManager.persist(period);

            Invoice invoice = new Invoice();
            invoice.setInvoiceName("INV-TAX-COMPONENT-POST");
            invoice.setInvoiceDescription("Multi component purchase invoice");
            invoice.setAmount(11_350L); invoice.setNetAmount(10_000L); invoice.setTaxAmount(1_550L);
            invoice.setWithholdingAmount(200L); invoice.setReverseChargeTaxAmount(1_000L);
            invoice.setPaidAmount(0L); invoice.setCreditedAmount(0L);
            invoice.setInvoiceType(InvoiceType.PURCHASE); invoice.setInvoiceStatus(InvoiceStatus.CONFIRMED);
            invoice.setWorkflowKey("purchase:multi-component-post"); invoice.setCreationValidated(true);
            invoice.getTaxComponents().add(invoiceComponent(invoice, "VAT", TaxComponentType.STANDARD, "10", 1_000L, null, inputTax));
            invoice.getTaxComponents().add(invoiceComponent(invoice, "Compound levy", TaxComponentType.COMPOUND, "5", 550L, null, inputTax));
            invoice.getTaxComponents().add(invoiceComponent(invoice, "Withholding", TaxComponentType.WITHHOLDING, "2", 200L, withholding, null));
            invoice.getTaxComponents().add(invoiceComponent(invoice, "Reverse charge", TaxComponentType.REVERSE_CHARGE, "10", 1_000L, outputTax, inputTax));
            entityManager.persist(invoice); entityManager.flush();
            return new LedgerFixture(invoice.getId(), year.getId(), period.getId(),
                    expense.getId(), payable.getId(), inputTax.getId(), outputTax.getId(), withholding.getId());
        });

        ErpWorkflowRequests.DocumentPosting originalPosting = new ErpWorkflowRequests.DocumentPosting(
                "multi-component-invoice-post", ledger.fiscalYearId(), ledger.fiscalPeriodId(), null,
                LocalDate.of(2026, 11, 9), "Post purchase tax components",
                ledger.expenseAccountId(), ledger.payableAccountId(), null);
        Map<String,Object> posted = workflows.postInvoiceToLedger(ledger.invoiceId(), originalPosting);
        Long originalJournalId = ((Number) posted.get("journalEntry")).longValue();

        ErpWorkflowRequests.DocumentPosting creditPosting = new ErpWorkflowRequests.DocumentPosting(
                "multi-component-credit-post", ledger.fiscalYearId(), ledger.fiscalPeriodId(), null,
                LocalDate.of(2026, 11, 9), "Reverse purchase tax components",
                ledger.expenseAccountId(), ledger.payableAccountId(), null);
        Map<String,Object> credit = workflows.createCreditNote(ledger.invoiceId(),
                new ErpWorkflowRequests.CreditNote("multi-component-credit", 5_675L, "Tax correction",
                        "TAX_CORRECTION", "TAX_CORRECTION_MEMO", "DOC-TAX-2026", creditPosting));
        Long creditId = ((Number) credit.get("creditNote")).longValue();

        inNewTransaction(() -> {
            entityManager.clear();
            Invoice note = entityManager.find(Invoice.class, creditId);
            assertThat(note.getAmount()).isEqualTo(5_675L);
            assertThat(note.getTaxAmount()).isEqualTo(775L);
            assertThat(note.getWithholdingAmount()).isEqualTo(100L);
            assertThat(note.getReverseChargeTaxAmount()).isEqualTo(500L);
            assertThat(note.getLedgerJournalEntry()).isNotNull();
            assertThat(entityManager.createQuery(
                    "select count(l) from JournalEntryLine l where l.journalEntry.id=:id", Long.class)
                    .setParameter("id", originalJournalId).getSingleResult()).isEqualTo(7L);
            assertThat(entityManager.createQuery(
                    "select count(l) from JournalEntryLine l where l.journalEntry.id=:id", Long.class)
                    .setParameter("id", note.getLedgerJournalEntry().getId()).getSingleResult()).isEqualTo(7L);
            return null;
        });
    }

    private Account account(String code, String name) {
        Account account = new Account(); account.setCode(code); account.setName(name);
        account.setPostingAllowed(true); account.setBlocked(false); entityManager.persist(account);
        return account;
    }

    private InvoiceTaxComponent invoiceComponent(Invoice invoice, String name, TaxComponentType type,
                                                  String rate, long amount, Account output, Account input) {
        InvoiceTaxComponent component = invoiceComponent(invoice, name, type, rate, amount);
        component.setOutputTaxAccount(output); component.setInputTaxAccount(input);
        return component;
    }

    @Test
    void creditNoteReversesEachTaxComponentProportionallyAndRequiresTaxCorrectionEvidence() {
        Long originalId = inNewTransaction(() -> {
            Invoice original = new Invoice();
            original.setInvoiceName("INV-CN-ORIGINAL");
            original.setInvoiceDescription("Tax component credit note source");
            original.setAmount(1_100L); original.setNetAmount(1_000L); original.setTaxAmount(200L);
            original.setWithholdingAmount(100L); original.setReverseChargeTaxAmount(500L);
            original.setPaidAmount(0L); original.setCreditedAmount(0L);
            original.setInvoiceType(InvoiceType.SALE); original.setInvoiceStatus(InvoiceStatus.CONFIRMED);
            original.setWorkflowKey("sales:credit-note-source"); original.setCreationValidated(true);
            InvoiceTaxComponent vat = invoiceComponent(original, "VAT", TaxComponentType.STANDARD, "20", 200L);
            InvoiceTaxComponent withholding = invoiceComponent(original, "Withholding", TaxComponentType.WITHHOLDING, "10", 100L);
            InvoiceTaxComponent reverse = invoiceComponent(original, "Reverse charge", TaxComponentType.REVERSE_CHARGE, "50", 500L);
            original.getTaxComponents().addAll(List.of(vat, withholding, reverse));
            entityManager.persist(original); entityManager.flush();
            return original.getId();
        });

        assertThatThrownBy(() -> workflows.createCreditNote(originalId,
                new ErpWorkflowRequests.CreditNote("cn-evidence-missing", 550L, "Correct tax", "TAX_CORRECTION",
                        "TAX_CORRECTION_MEMO", null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("evidence");

        Map<String,Object> result = workflows.createCreditNote(originalId,
                new ErpWorkflowRequests.CreditNote("cn-tax-component", 550L, "Correct tax", "TAX_CORRECTION",
                        "TAX_CORRECTION_MEMO", "DOC-2026-77", null));
        inNewTransaction(() -> {
            entityManager.clear();
            Invoice note = entityManager.find(Invoice.class, ((Number) result.get("creditNote")).longValue());
            assertThat(note.getAmount()).isEqualTo(550L);
            assertThat(note.getNetAmount()).isEqualTo(500L);
            assertThat(note.getTaxAmount()).isEqualTo(100L);
            assertThat(note.getWithholdingAmount()).isEqualTo(50L);
            assertThat(note.getReverseChargeTaxAmount()).isEqualTo(250L);
            assertThat(note.getCreditNoteReasonCode()).isEqualTo(CreditNoteReasonCode.TAX_CORRECTION);
            assertThat(note.getTaxEvidenceReference()).isEqualTo("DOC-2026-77");
            assertThat(note.getTaxComponents()).hasSize(3);
            return null;
        });
    }

    private InvoiceTaxComponent invoiceComponent(Invoice invoice, String name, TaxComponentType type,
                                                  String rate, long amount) {
        InvoiceTaxComponent component = new InvoiceTaxComponent();
        component.setInvoice(invoice); component.setName(name); component.setComponentType(type);
        component.setRate(new java.math.BigDecimal(rate)); component.setAmount(amount);
        return component;
    }

    private record LedgerFixture(Long invoiceId, Long fiscalYearId, Long fiscalPeriodId,
                                  Long expenseAccountId, Long payableAccountId, Long inputTaxAccountId,
                                  Long outputTaxAccountId, Long withholdingAccountId) {}

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
            assertThat(purchase.getPurchaseDetails()).hasSize(1);
            assertThat(purchase.getPurchaseDetails().iterator().next().getTaxComponents()).hasSize(1);
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
