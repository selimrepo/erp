package com.erplancers.erp.common.security.web;

import com.erplancers.erp.common.security.ErpUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final ErpUserService users;

    @Value("${erp.registration.admin-token:}")
    private String adminRegistrationToken;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request) {
        users.create(request.username().trim(), request.password(), "USER", "USER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(request.username().trim(), "Registration successful"));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<RegistrationResponse> registerAdmin(
            @Valid @RequestBody AdminRegistrationRequest request) {
        if (adminRegistrationToken.isBlank() || !adminRegistrationToken.equals(request.adminToken())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String username = request.username().trim();
        users.create(username, request.password(), "ADMIN", "ADMIN");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(username, "Admin registration successful"));
    }

    public record RegistrationRequest(
            @NotBlank @Size(min = 3, max = 120) String username,
            @NotBlank @Size(min = 8, max = 128) String password) {}

    public record AdminRegistrationRequest(
            @NotBlank @Size(min = 3, max = 120) String username,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(min = 16, max = 256) String adminToken) {}

    public record RegistrationResponse(String username, String message) {}
}
