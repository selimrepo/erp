package com.erplancers.erp.common.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpWorkflowRequests {
    private ErpWorkflowRequests() {}
    public record SalesLine(@NotNull Long productId, @NotNull @Positive Long quantity, @NotNull @PositiveOrZero Long unitPrice, Long taxRateId, @Size(max = 32) String jurisdictionCode, @Size(max = 60) String taxCategory, String exemptionCode, String evidenceReference) { public SalesLine(Long productId, Long quantity, Long unitPrice, Long taxRateId) { this(productId, quantity, unitPrice, taxRateId, null, null, null, null); } public SalesLine(Long productId, Long quantity, Long unitPrice, Long taxRateId, String jurisdictionCode, String taxCategory) { this(productId, quantity, unitPrice, taxRateId, jurisdictionCode, taxCategory, null, null); } public SalesLine(Long productId, Long quantity, Long unitPrice) { this(productId, quantity, unitPrice, null, null, null, null, null); } }
    public record SalesOrder(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long customerId, @NotNull Long shippingContactId,
                             @NotNull Long currencyId, @NotNull Long storeId, @NotEmpty List<@Valid SalesLine> lines,
                             String comment, boolean createInvoice, @Valid DocumentPosting accountingPosting) { public SalesOrder(String idempotencyKey, Long customerId, Long shippingContactId, Long currencyId, Long storeId, List<SalesLine> lines, String comment, boolean createInvoice) { this(idempotencyKey, customerId, shippingContactId, currencyId, storeId, lines, comment, createInvoice, null); } }
    public record PurchaseLine(@NotNull Long productId, @NotNull @Positive Long quantity, @NotNull @PositiveOrZero Long unitPrice, Long taxRateId, @Size(max = 32) String jurisdictionCode, @Size(max = 60) String taxCategory, String exemptionCode, String evidenceReference) { public PurchaseLine(Long productId, Long quantity, Long unitPrice, Long taxRateId) { this(productId, quantity, unitPrice, taxRateId, null, null, null, null); } public PurchaseLine(Long productId, Long quantity, Long unitPrice, Long taxRateId, String jurisdictionCode, String taxCategory) { this(productId, quantity, unitPrice, taxRateId, jurisdictionCode, taxCategory, null, null); } public PurchaseLine(Long productId, Long quantity, Long unitPrice) { this(productId, quantity, unitPrice, null, null, null, null, null); } }
    public record PurchaseOrder(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long supplierId, @NotNull Long storeId, @NotNull Long currencyId,
                                @NotEmpty List<@Valid PurchaseLine> lines, String notes) {}
    public record StockAdjustmentRequest(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long productId,
                                         @NotNull Long storeId, @NotNull Long deltaQuantity,
                                         @NotBlank @Size(max = 500) String reason) {}
    public record StockTransferRequest(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long productId,
                                       @NotNull Long sourceStoreId, @NotNull Long destinationStoreId,
                                       @NotNull @Positive Long quantity) {}
    public record ReceivePurchase(@NotBlank @Size(max = 160) String idempotencyKey, @Valid DocumentPosting accountingPosting) { public ReceivePurchase(String idempotencyKey) { this(idempotencyKey, null); } }
    public record PurchaseReceiptLineInput(@NotNull Long purchaseDetailId, @NotNull @Positive Long quantity) {}
    public record PartialReceivePurchase(@NotBlank @Size(max = 160) String idempotencyKey, @NotEmpty List<@Valid PurchaseReceiptLineInput> lines, @Valid DocumentPosting accountingPosting) { public PartialReceivePurchase(String idempotencyKey, List<PurchaseReceiptLineInput> lines) { this(idempotencyKey, lines, null); } }
    public record OrderTransition(@NotBlank @Size(max = 160) String idempotencyKey) {}
    public record WorkflowTransition(@NotBlank String status, String resolution) {}
    public record QuoteConversion(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long storeId, boolean createInvoice, String comment) {}
    public record CreditNote(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull @Positive Long amount, @NotBlank String reason, String reasonCode, String evidenceReference, @Valid DocumentPosting accountingPosting) { public CreditNote(String idempotencyKey, Long amount, String reason) { this(idempotencyKey, amount, reason, null, null, null); } }
    public record BankStatementLineInput(@NotBlank String externalLineId, @NotNull LocalDate transactionDate, @NotNull Long amount, String reference, String description) {}
    public record BankStatementImport(@NotBlank @Size(max = 160) String idempotencyKey, @NotBlank String statementReference, @NotNull Long currencyId,
            @NotNull LocalDate periodStart, @NotNull LocalDate periodEnd, @NotEmpty List<@Valid BankStatementLineInput> lines) {}
    public record InvoicePayment(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull @Positive Long amount,
                                 @NotNull Long contactId, @NotNull Long currencyId,
                                 @NotNull String direction, String reference, String description) {}
    public record AutoDocumentPosting(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long fiscalYearId, @NotNull Long fiscalPeriodId,
            Long companyId, @NotNull LocalDate entryDate, @NotBlank String description) {}
    public record DocumentPosting(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long fiscalYearId, @NotNull Long fiscalPeriodId,
                                  Long companyId, @NotNull LocalDate entryDate, @NotBlank String description,
                                  @NotNull Long debitAccountId, @NotNull Long creditAccountId, Long taxAccountId, Long withholdingAccountId, Long reverseChargeInputAccountId, Long reverseChargeOutputAccountId) { public DocumentPosting(String idempotencyKey, Long fiscalYearId, Long fiscalPeriodId, Long companyId, LocalDate entryDate, String description, Long debitAccountId, Long creditAccountId, Long taxAccountId) { this(idempotencyKey, fiscalYearId, fiscalPeriodId, companyId, entryDate, description, debitAccountId, creditAccountId, taxAccountId, null, null, null); } }
    public record JournalLine(@NotNull Long accountId, @NotNull BigDecimal debit, @NotNull BigDecimal credit, String description) {}
    public record JournalPost(@NotBlank @Size(max = 160) String idempotencyKey, @NotNull Long fiscalYearId, @NotNull Long fiscalPeriodId,
                              Long companyId, @NotBlank String entryId, @NotNull LocalDate entryDate,
                              @NotBlank String description, @NotNull String entryType,
                              @NotEmpty List<@Valid JournalLine> lines) {}
}
