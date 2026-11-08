package com.erplancers.erp.common.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ErpWorkflowRequests {
    private ErpWorkflowRequests() {}
    public record SalesLine(@NotNull Long productId, @NotNull @Positive Long quantity, @NotNull @PositiveOrZero Long unitPrice) {}
    public record SalesOrder(@NotBlank String idempotencyKey, @NotNull Long customerId, @NotNull Long shippingContactId,
                             @NotNull Long currencyId, @NotNull Long storeId, @NotEmpty List<@Valid SalesLine> lines,
                             String comment, boolean createInvoice) {}
    public record PurchaseLine(@NotNull Long productId, @NotNull @Positive Long quantity, @NotNull @PositiveOrZero Long unitPrice) {}
    public record PurchaseOrder(@NotBlank String idempotencyKey, @NotNull Long supplierId, @NotNull Long storeId,
                                @NotEmpty List<@Valid PurchaseLine> lines, String notes) {}
    public record ReceivePurchase(@NotBlank String idempotencyKey) {}
    public record InvoicePayment(@NotBlank String idempotencyKey, @NotNull @Positive Long amount,
                                 @NotNull Long contactId, @NotNull Long currencyId,
                                 @NotNull String direction, String reference, String description) {}
    public record JournalLine(@NotNull Long accountId, @NotNull BigDecimal debit, @NotNull BigDecimal credit, String description) {}
    public record JournalPost(@NotBlank String idempotencyKey, @NotNull Long fiscalYearId, @NotNull Long fiscalPeriodId,
                              Long companyId, @NotBlank String entryId, @NotNull LocalDate entryDate,
                              @NotBlank String description, @NotNull String entryType,
                              @NotEmpty List<@Valid JournalLine> lines) {}
}
