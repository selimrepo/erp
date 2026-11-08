package com.erplancers.erp.demo;

import com.erplancers.erp.accounting.domain.*;
import com.erplancers.erp.crm.contact.domain.Contact;
import com.erplancers.erp.crm.contact.domain.Location;
import com.erplancers.erp.crm.customer.domain.Customer;
import com.erplancers.erp.finance.domain.*;
import com.erplancers.erp.finance.domain.Currency;
import com.erplancers.erp.inventory.product.domain.Product;
import com.erplancers.erp.inventory.stock.domain.*;
import com.erplancers.erp.inventory.warehouse.domain.Store;
import com.erplancers.erp.procurement.domain.*;
import com.erplancers.erp.sales.domain.Sale;
import com.erplancers.erp.sales.order.domain.Order;
import com.erplancers.erp.sales.order.domain.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Connects generic JavaFaker fixtures into coherent ERP business workflows.
 * It runs after DemoDataSeeder and only when demo data is explicitly enabled.
 */
@Component
@ConditionalOnProperty(prefix = "erp.demo-data", name = "enabled", havingValue = "true")
@org.springframework.core.annotation.Order(Ordered.LOWEST_PRECEDENCE)
public class DemoBusinessScenarioSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoBusinessScenarioSeeder.class);

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Contact> contacts = all(Contact.class);
        List<Location> locations = all(Location.class);
        List<Customer> customers = all(Customer.class);
        List<Currency> currencies = all(Currency.class);
        List<Order> orders = all(Order.class);
        List<Invoice> invoices = all(Invoice.class);
        List<Product> products = all(Product.class);
        List<Supplier> suppliers = all(Supplier.class);
        List<Store> stores = all(Store.class);
        List<StockItem> stockItems = all(StockItem.class);
        List<ProductStockLocation> stockLocations = all(ProductStockLocation.class);
        List<Purchase> purchases = all(Purchase.class);
        List<PurchaseDetail> purchaseDetails = all(PurchaseDetail.class);
        List<Sale> sales = all(Sale.class);
        List<PaymentTransaction> payments = all(PaymentTransaction.class);
        List<JournalEntry> journals = all(JournalEntry.class);
        List<Account> accounts = all(Account.class);
        List<FiscalYear> years = all(FiscalYear.class);
        List<FiscalPeriod> periods = all(FiscalPeriod.class);

        if (customers.isEmpty() || contacts.isEmpty()) {
            log.warn("Demo business workflows skipped: customer/contact fixtures are unavailable.");
            return;
        }

        Currency currency = first(currencies);
        Contact company = first(contacts);
        FiscalYear year = first(years);
        FiscalPeriod period = first(periods);
        Account debitAccount = accounts.size() > 0 ? accounts.get(0) : null;
        Account creditAccount = accounts.size() > 1 ? accounts.get(1) : debitAccount;

        // Customer -> order -> invoice.
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            Customer customer = customers.get(i % customers.size());
            Contact contact = contacts.get(i % contacts.size());
            Invoice invoice = invoices.isEmpty() ? null : invoices.get(i % invoices.size());

            set(order, "customer", customer);
            set(order, "shippingContact", contact);
            set(order, "currency", currency);
            set(order, "totalAmount", 12500L + i * 3750L);
            set(order, "orderStatus", OrderStatus.values()[i % OrderStatus.values().length]);
            set(order, "comment", "Demo satış siparişi #" + (i + 1));

            if (invoice != null) {
                set(order, "invoice", invoice);
                set(invoice, "invoiceName", "SAT-" + String.format("%05d", i + 1));
                set(invoice, "invoiceDescription", "Sipariş #" + (i + 1) + " için satış faturası");
                set(invoice, "amount", 12500L + i * 3750L);
                set(invoice, "currency", currency);
                set(invoice, "invoiceType", InvoiceType.SALE);
                set(invoice, "invoiceStatus", InvoiceStatus.POSTED);
                set(invoice, "billingContacts", new LinkedHashSet<>(List.of(contact)));
                set(invoice, "shippingContacts", new LinkedHashSet<>(List.of(contact)));
            }
        }

        // Product -> stock item -> supplier + warehouse store; product stock location -> physical location.
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            Supplier supplier = suppliers.isEmpty() ? null : suppliers.get(i % suppliers.size());
            Store store = stores.isEmpty() ? null : stores.get(i % stores.size());
            Location location = locations.isEmpty() ? null : locations.get(i % locations.size());
            Contact locationOwner = contacts.get(i % contacts.size());

            set(product, "buyPrice", 650L + i * 95L);
            set(product, "sellPrice", 990L + i * 145L);
            if (supplier != null) set(product, "suppliers", new LinkedHashSet<>(List.of(supplier)));

            if (!stockItems.isEmpty()) {
                StockItem item = stockItems.get(i % stockItems.size());
                set(item, "product", product);
                set(item, "supplier", supplier);
                set(item, "store", store);
                set(item, "dateIn", LocalDate.now().minusDays(3L + i));
            }

            if (!stockLocations.isEmpty()) {
                ProductStockLocation stockLocation = stockLocations.get(i % stockLocations.size());
                set(stockLocation, "product", product);
                if (store != null) set(stockLocation, "store", store);
                if (location != null) set(stockLocation, "locations", new LinkedHashSet<>(List.of(location)));
                if (supplier != null) set(stockLocation, "suppliers", new LinkedHashSet<>(List.of(supplier)));
            }

            if (store != null) {
                set(store, "contact", locationOwner);
                if (supplier != null) set(store, "suppliers", new LinkedHashSet<>(List.of(supplier)));
            }
            if (location != null) set(location, "owner", locationOwner);

            if (!sales.isEmpty() && !stockItems.isEmpty()) {
                Sale sale = sales.get(i % sales.size());
                set(sale, "product", product);
                set(sale, "stockItem", stockItems.get(i % stockItems.size()));
                set(sale, "supplier", supplier);
                set(sale, "date", LocalDate.now().minusDays(i));
            }
        }

        // Supplier -> purchase -> purchase detail -> product; incoming stock receipts are linked above.
        for (int i = 0; i < purchases.size(); i++) {
            Purchase purchase = purchases.get(i);
            set(purchase, "supplier", suppliers.isEmpty() ? null : suppliers.get(i % suppliers.size()));
            set(purchase, "store", stores.isEmpty() ? null : stores.get(i % stores.size()));
            set(purchase, "currency", currency);
            set(purchase, "status", "ORDERED");
            set(purchase, "purchaseDate", LocalDateTime.now().minusDays(14L + i));
            set(purchase, "approvedDate", LocalDateTime.now().minusDays(10L + i));

            if (!purchaseDetails.isEmpty()) {
                PurchaseDetail detail = purchaseDetails.get(i % purchaseDetails.size());
                set(detail, "purchase", purchase);
                set(detail, "product", products.isEmpty() ? null : products.get(i % products.size()));
                set(detail, "supplier", suppliers.isEmpty() ? null : suppliers.get(i % suppliers.size()));
                set(detail, "price", 820L + i * 110L);
                set(detail, "quantity", 8L + i);
                set(purchase, "purchaseDetails", new LinkedHashSet<>(List.of(detail)));
            }
        }

        // Completed cash movements, plus balanced double-entry journal entries.
        for (int i = 0; i < payments.size(); i++) {
            PaymentTransaction payment = payments.get(i);
            long amount = 12500L + i * 3750L;
            set(payment, "currency", currency);
            if (!invoices.isEmpty()) set(payment, "invoice", invoices.get(i % invoices.size()));
            set(payment, "contact", contacts.get(i % contacts.size()));
            set(payment, "amount", amount);
            set(payment, "payment", amount);
            set(payment, "quantity", 1L);
            set(payment, "status", "COMPLETED");
            set(payment, "referenceId", "DEMO-PAY-" + String.format("%05d", i + 1));
            set(payment, "description", "Demo tahsilat / ödeme hareketi");
            set(payment, "paymentTransactionDirection", PaymentTransactionDirection.SALES);
        }

        if (!journals.isEmpty() && debitAccount != null && creditAccount != null && year != null && period != null) {
            for (int i = 0; i < journals.size(); i++) {
                JournalEntry journal = journals.get(i);
                if (hasJournalLines(journal)) continue;

                LocalDate date = LocalDate.now().minusDays(i + 1L);
                set(journal, "company", company);
                set(journal, "fiscalYear", year);
                set(journal, "fiscalPeriod", period);
                set(journal, "entryId", "DEMO-FIS-" + String.format("%05d", i + 1));
                set(journal, "entryDate", date);
                set(journal, "description", "Satış faturası muhasebe kaydı");
                set(journal, "entryType", JournalEntryType.SALES);
                set(journal, "status", JournalEntryStatus.POSTED);

                BigDecimal amount = BigDecimal.valueOf(12500L + i * 3750L);
                persistLine(journal, debitAccount, currency, year, period, date, amount, BigDecimal.ZERO,
                        "120 Alıcılar - borç");
                persistLine(journal, creditAccount, currency, year, period, date, BigDecimal.ZERO, amount,
                        "600 Yurtiçi Satışlar - alacak");
            }
        } else {
            log.warn("Journal lines not generated: chart of accounts and fiscal-year/period fixtures are required.");
        }

        em.flush();
        log.info("Demo workflows linked: orders={}, invoices={}, products={}, purchases={}, payments={}, journals={}",
                orders.size(), invoices.size(), products.size(), purchases.size(), payments.size(), journals.size());
    }

    private void persistLine(JournalEntry journal, Account account, com.erplancers.erp.finance.domain.Currency currency,
                             FiscalYear year, FiscalPeriod period, LocalDate date,
                             BigDecimal debit, BigDecimal credit, String description) {
        JournalEntryLine line = new JournalEntryLine();
        set(line, "journalEntry", journal);
        set(line, "account", account);
        set(line, "currency", currency);
        set(line, "fiscalYear", year);
        set(line, "fiscalPeriod", period);
        set(line, "entryDate", date);
        set(line, "debit", debit);
        set(line, "credit", credit);
        set(line, "foreignDebit", debit);
        set(line, "foreignCredit", credit);
        set(line, "journalEntryStatus", JournalEntryStatus.POSTED);
        set(line, "description", description);
        em.persist(line);
    }

    private boolean hasJournalLines(JournalEntry journal) {
        return em.createQuery(
                "select count(l) from JournalEntryLine l where l.journalEntry = :journal", Long.class)
                .setParameter("journal", journal)
                .getSingleResult() > 0;
    }

    private <T> List<T> all(Class<T> type) {
        try {
            String entityName = em.getMetamodel().entity(type).getName();
            return em.createQuery("select e from " + entityName + " e", type).getResultList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private <T> T first(List<T> values) {
        return values.isEmpty() ? null : values.get(0);
    }

    private void set(Object target, String fieldName, Object value) {
        if (target == null || value == null) return;
        for (Class<?> type = target.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                if (field.getType().isAssignableFrom(value.getClass())) field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // Search inherited fields.
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Cannot set demo field " + fieldName, ex);
            }
        }
    }
}
