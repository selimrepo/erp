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

    @PostMapping("/sales/orders")
    public Map<String,Object> createSalesOrder(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.SalesOrder request) {
        require(auth,"order","WRITE"); require(auth,"product-stock-location","WRITE");
        if(request.createInvoice()) require(auth,"invoice","WRITE");
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
    @PostMapping("/procurement/purchases")
    public Map<String,Object> createPurchase(Authentication auth,@Valid @RequestBody ErpWorkflowRequests.PurchaseOrder request) {
        require(auth,"purchase","WRITE"); require(auth,"purchase-detail","WRITE");
        return workflows.createPurchase(request);
    }
    @PostMapping("/procurement/purchases/{id}/receive")
    public Map<String,Object> receivePurchase(Authentication auth,@PathVariable Long id,@RequestParam Long storeId,
                                               @Valid @RequestBody ErpWorkflowRequests.ReceivePurchase request) {
        require(auth,"purchase","WRITE"); require(auth,"product-stock-location","WRITE"); require(auth,"stock-item","WRITE");
        return workflows.receivePurchase(id,request,storeId);
    }
    @PostMapping("/finance/invoices/{id}/payments")
    public Map<String,Object> payInvoice(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.InvoicePayment request) {
        require(auth,"invoice","WRITE"); require(auth,"payment-transaction","WRITE");
        return workflows.payInvoice(id,request);
    }
    @PostMapping("/finance/invoices/{id}/post")
    public Map<String,Object> postInvoiceToLedger(Authentication auth,@PathVariable Long id,@Valid @RequestBody ErpWorkflowRequests.DocumentPosting request) {
        require(auth,"invoice","WRITE"); require(auth,"journal-entry","WRITE"); require(auth,"journal-entry-line","WRITE"); require(auth,"account","READ");
        return workflows.postInvoiceToLedger(id,request);
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
    private void require(Authentication auth,String resource,String action) {
        if(!authorization.isAllowed(auth,resource,action)) throw new AccessDeniedException(action+" access denied for "+resource);
    }
}
