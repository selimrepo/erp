package com.erplancers.erp.common.workflow;

import com.erplancers.erp.common.security.PolicyAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class ErpWorkflowController {
    private final ErpWorkflowService workflows;
    private final PolicyAuthorizationService authorization;

    @PostMapping("/sales/quotes/{id}/transition")
    public Map<String,Object> transitionQuote(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.WorkflowTransition request) {
        require(auth,"quote","WRITE"); return workflows.transitionQuote(id,request);
    }
    @PostMapping("/crm/opportunities/{id}/transition")
    public Map<String,Object> transitionOpportunity(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.WorkflowTransition request) {
        require(auth,"opportunity","WRITE"); return workflows.transitionOpportunity(id,request);
    }
    @PostMapping("/support/cases/{id}/transition")
    public Map<String,Object> transitionCase(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.WorkflowTransition request) {
        require(auth,"case","WRITE"); return workflows.transitionCase(id,request);
    }
    @PostMapping("/projects/{id}/transition")
    public Map<String,Object> transitionProject(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.WorkflowTransition request) {
        require(auth,"project","WRITE"); return workflows.transitionProject(id,request);
    }
    @PostMapping("/projects/tasks/{id}/transition")
    public Map<String,Object> transitionProjectTask(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.WorkflowTransition request) {
        require(auth,"project-task","WRITE"); return workflows.transitionProjectTask(id,request);
    }
    @PostMapping("/sales/quotes/{id}/convert")
    public Map<String,Object> convertQuote(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.QuoteConversion request) {
        require(auth,"quote","WRITE"); require(auth,"order","WRITE"); require(auth,"product-stock-location","WRITE"); require(auth,"stock-movement","WRITE");
        if(request.createInvoice()) require(auth,"invoice","WRITE");
        return workflows.convertQuoteToOrder(id,request);
    }
    @PostMapping("/sales/orders")
    public Map<String,Object> createSalesOrder(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.SalesOrder request) {
        require(auth,"order","WRITE"); require(auth,"product-stock-location","WRITE");
        if(request.createInvoice()) require(auth,"invoice","WRITE");
        if(request.accountingPosting()!=null) requireLedgerPosting(auth);
        return workflows.createSalesOrder(request);
    }
    @PostMapping("/sales/orders/{id}/ship")
    public Map<String,Object> shipSalesOrder(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.OrderTransition request) {
        require(auth,"order","WRITE"); require(auth,"stock-movement","WRITE");
        return workflows.transitionSalesOrder(id,request,true);
    }
    @PostMapping("/sales/orders/{id}/cancel")
    public Map<String,Object> cancelSalesOrder(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.OrderTransition request) {
        require(auth,"order","WRITE"); require(auth,"product-stock-location","WRITE"); require(auth,"stock-movement","WRITE");
        return workflows.transitionSalesOrder(id,request,false);
    }
    @PostMapping("/inventory/stock-adjustments")
    public Map<String,Object> adjustStock(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.StockAdjustmentRequest request) {
        require(auth,"stock-adjustment","WRITE"); require(auth,"product-stock-location","WRITE"); require(auth,"stock-movement","WRITE");
        return workflows.adjustStock(request);
    }
    @PostMapping("/inventory/stock-transfers")
    public Map<String,Object> transferStock(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.StockTransferRequest request) {
        require(auth,"product-stock-location","WRITE"); require(auth,"stock-movement","WRITE");
        return workflows.transferStock(request);
    }
    @PostMapping("/procurement/purchases")
    public Map<String,Object> createPurchase(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.PurchaseOrder request) {
        require(auth,"purchase","WRITE"); require(auth,"purchase-detail","WRITE");
        return workflows.createPurchase(request);
    }
    @PostMapping("/procurement/purchases/{id}/receive")
    public Map<String,Object> receivePurchase(Authentication auth,@PathVariable Long id,@RequestParam Long storeId,
                                               @Valid @RequestBody ErpWorkflowRequests.ReceivePurchase request) {
        require(auth,"purchase","WRITE"); require(auth,"product-stock-location","WRITE"); require(auth,"stock-item","WRITE");
        if(request.accountingPosting()!=null) requireLedgerPosting(auth);
        return workflows.receivePurchase(id,request,storeId);
    }
    @PostMapping("/procurement/purchases/{id}/receive-lines")
    public Map<String,Object> receivePurchasePartial(Authentication auth,@PathVariable Long id,@RequestParam Long storeId,
            @Valid @RequestBody ErpWorkflowRequests.PartialReceivePurchase request) {
        require(auth,"purchase","WRITE"); require(auth,"product-stock-location","WRITE"); require(auth,"stock-item","WRITE"); require(auth,"stock-movement","WRITE");
        if(request.accountingPosting()!=null) requireLedgerPosting(auth);
        return workflows.receivePurchasePartial(id,request,storeId);
    }
    @PostMapping("/finance/invoices/{id}/payments")
    public Map<String,Object> payInvoice(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.InvoicePayment request) {
        require(auth,"invoice","WRITE"); require(auth,"payment-transaction","WRITE");
        return workflows.payInvoice(id,request);
    }
    @PostMapping("/finance/bank-statements")
    public Map<String,Object> importBankStatement(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.BankStatementImport request) {
        require(auth,"bank-statement","WRITE"); require(auth,"payment-transaction","READ");
        return workflows.importBankStatement(request);
    }
    @PostMapping("/finance/bank-statement-lines/{id}/reconcile/{paymentId}")
    public Map<String,Object> reconcileBankStatementLine(Authentication auth,@PathVariable Long id,@PathVariable Long paymentId) {
        require(auth,"bank-statement-line","WRITE"); require(auth,"payment-transaction","WRITE");
        return workflows.reconcileBankStatementLine(id,paymentId);
    }
    @PostMapping("/finance/invoices/{id}/credit-notes")
    public Map<String,Object> createCreditNote(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.CreditNote request) {
        require(auth,"invoice","WRITE");
        return workflows.createCreditNote(id,request);
    }
    @PostMapping("/finance/invoices/{id}/post")
    public Map<String,Object> postInvoiceToLedger(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.DocumentPosting request) {
        require(auth,"invoice","WRITE"); require(auth,"journal-entry","WRITE"); require(auth,"journal-entry-line","WRITE"); require(auth,"account","READ");
        return workflows.postInvoiceToLedger(id,request);
    }
    @PostMapping("/finance/invoices/{id}/post-auto")
    public Map<String,Object> postInvoiceAutomatically(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.AutoDocumentPosting request) {
        require(auth,"invoice","WRITE"); require(auth,"journal-entry","WRITE"); require(auth,"journal-entry-line","WRITE"); require(auth,"account-determination-rule","READ");
        return workflows.postInvoiceAutomatically(id,request);
    }
    @PostMapping("/finance/payments/{id}/post-auto")
    public Map<String,Object> postPaymentAutomatically(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.AutoDocumentPosting request) {
        require(auth,"payment-transaction","WRITE"); require(auth,"journal-entry","WRITE"); require(auth,"journal-entry-line","WRITE"); require(auth,"account-determination-rule","READ");
        return workflows.postPaymentAutomatically(id,request);
    }
    @PostMapping("/finance/payments/{id}/post")
    public Map<String,Object> postPaymentToLedger(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.DocumentPosting request) {
        require(auth,"payment-transaction","WRITE"); require(auth,"journal-entry","WRITE"); require(auth,"journal-entry-line","WRITE"); require(auth,"account","READ");
        return workflows.postPaymentToLedger(id,request);
    }
    @PostMapping("/accounting/journals")
    public Map<String,Object> postJournal(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.JournalPost request) {
        require(auth,"journal-entry","WRITE"); require(auth,"journal-entry-line","WRITE"); require(auth,"account","READ");
        return workflows.postJournal(request);
    }
    private void requireLedgerPosting(Authentication auth) {
        require(auth,"journal-entry","WRITE"); require(auth,"journal-entry-line","WRITE"); require(auth,"account","READ");
    }
    private void require(Authentication auth,String resource,String action) {
        if(!authorization.isAllowed(auth,resource,action)) throw new AccessDeniedException(action+" access denied for "+resource);
    }
}
