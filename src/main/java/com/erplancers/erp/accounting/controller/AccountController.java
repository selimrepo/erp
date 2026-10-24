package com.erplancers.erp.accounting.controller;

import com.erplancers.erp.accounting.domain.Account;
import com.erplancers.erp.accounting.report.AccountReportService;
import com.erplancers.erp.accounting.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountReportService accountReportService;

    @GetMapping("/api/accounts/{id}")
    public ResponseEntity<Account> findById(@PathVariable Long id) {
        Optional<Account> account = accountRepository.findById(id);
        return account.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/accounts")
    public ResponseEntity<List<Account>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Account> accounts = accountRepository.findAll(PageRequest.of(page, size)).toList();
        // Collection endpoints should return an empty JSON array rather than 204.
        // TanStack Query requires a defined value even when no accounts exist.
        return ResponseEntity.ok(accounts);
    }
}
