package com.erplancers.erp.common.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpWorkflowRequests {
    private ErpWorkflowRequests() {}
    public record SalesLine(@NotNull Long productId, @NotNull @Positive Long quantity, @NotNull @PositiveOrZero Long unitPrice, Long taxRateId) { public SalesLine(Long productId, Long quantity, Long unitPrice) { this(productId, quantity, unitPrice, null); } }
    public record SalesOrder(@NotBlank String idempotencyKey, @NotNull Long customerId, @NotNull Long shippingContactId,
                             @NotNull Long currencyId, @NotNull Long storeId, @NotEmpty List<@Valid SalesLine> lines,
                             String comment, boolean createInvoice) {}
    public record PurchaseLine(@NotNull Long productId, @NotNull @Positive Long quantity, @NotNull @PositiveOrZero Long unitPrice, Long taxRateId) { public PurchaseLine(Long productId, Long quantity, Long unitPrice) { this(productId, quantity, unitPrice, null); } }
    public record PurchaseOrder(@NotBlank String idempotencyKey, @NotNull Long supplierId, @NotNull Long storeId, @NotNull Long currencyId,
                                @NotEmpty List<@Valid PurchaseLine> lines, String notes) {}
    public record ReceivePurchase(@NotBlank String idempotencyKey) {}
    public record PurchaseReceiptLineInput(@NotNull Long purchaseDetailId, @NotNull @Positive Long quantity) {}
    public record PartialReceivePurchase(@NotBlank String idempotencyKey, @NotEmpty List<@Valid PurchaseReceiptLineInput> lines) {}
    public record OrderTransition(@NotBlank String idempotencyKey) {}
    public record QuoteConversion(@NotBlank String idempotencyKey, @NotNull Long storeId, boolean createInvoice, String comment) {}
    public record CreditNote(@NotBlank String idempotencyKey, @NotNull @Positive Long amount, @NotBlank String reason) {}
    public record InvoicePayment(@NotBlank String idempotencyKey, @NotNull @Positive Long amount,
                                 @NotNull Long contactId, @NotNull Long currencyId,
                                 @NotNull String direction, String reference, String description) {}
    public record DocumentPosting(@NotBlank String idempotencyKey, @NotNull Long fiscalYearId, @NotNull Long fiscalPeriodId,
                                  Long companyId, @NotNull LocalDate entryDate, @NotBlank String description,
                                  @NotNull Long debitAccountId, @NotNull Long creditAccountId) {}
    public record JournalLine(@NotNull Long accountId, @NotNull BigDecimal debit, @NotNull BigDecimal credit, String description) {}
    public record JournalPost(@NotBlank String idempotencyKey, @NotNull Long fiscalYearId, @NotNull Long fiscalPeriodId,
                              Long companyId, @NotBlank String entryId, @NotNull LocalDate entryDate,
                              @NotBlank String description, @NotNull String entryType,
                              @NotEmpty List<@Valid JournalLine> lines) {}
}
