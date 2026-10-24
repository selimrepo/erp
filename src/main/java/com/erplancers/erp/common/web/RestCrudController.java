package com.erplancers.erp.common.web;

import com.erplancers.erp.common.domain.EntityBase;
import com.erplancers.erp.common.security.PolicyAuthorizationService;
import com.erplancers.erp.common.service.DynamicCrudService;
import com.erplancers.erp.common.web.dto.CrudRequest;
import com.erplancers.erp.common.web.dto.ResourceDto;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO-based REST API for every repository registered by CrudRegistry.
 *
 * Authorization is checked per resource first, then by module.
 */
@RestController
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
@RequestMapping("/api/resources")
public class RestCrudController {

    private final CrudRegistry registry;
    private final DynamicCrudService service;
    private final PolicyAuthorizationService authorization;

    @GetMapping
    public Collection<Map<String, Object>> resources(Authentication authentication) {
        return registry.all().values().stream()
                .filter(d -> authorization.isAllowed(authentication, d.slug(), "READ"))
                .map(d -> {
                    Map<String, Object> resource = new LinkedHashMap<>();
                    resource.put("slug", d.slug());
                    resource.put("module", d.module());
                    resource.put("entity", d.entityType().getSimpleName());
                    resource.put("path", "/api/resources/" + d.slug());
                    return resource;
                })
                .toList();
    }

    @GetMapping("/{slug}")
    public Page<ResourceDto> list(
            Authentication authentication,
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        requireRead(authentication, slug);
        return service.list(slug, page, size)
                .map(row -> toDto(slug, row));
    }

    @GetMapping("/{slug}/metadata")
    public Map<String, Object> metadata(
            Authentication authentication,
            @PathVariable String slug) {
        requireRead(authentication, slug);
        return service.formMetadata(slug, null);
    }

    @GetMapping("/{slug}/{id}")
    public ResourceDto get(
            Authentication authentication,
            @PathVariable String slug,
            @PathVariable Long id) {
        requireRead(authentication, slug);
        return toDto(slug, service.get(slug, id));
    }

    @GetMapping("/{slug}/{id}/metadata")
    public Map<String, Object> metadata(
            Authentication authentication,
            @PathVariable String slug,
            @PathVariable Long id) {
        requireRead(authentication, slug);
        return service.formMetadata(slug, id);
    }

    @PostMapping("/{slug}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto create(
            Authentication authentication,
            @PathVariable String slug,
            @Valid @RequestBody CrudRequest request) {
        requireWrite(authentication, slug);
        EntityBase saved = service.save(slug, null, request.values());
        return toDto(slug, service.get(slug, saved.getId()));
    }

    @PutMapping("/{slug}/{id}")
    public ResourceDto update(
            Authentication authentication,
            @PathVariable String slug,
            @PathVariable Long id,
            @Valid @RequestBody CrudRequest request) {
        requireWrite(authentication, slug);
        service.save(slug, id, request.values());
        return toDto(slug, service.get(slug, id));
    }

    @PatchMapping("/{slug}/{id}")
    public ResourceDto patch(
            Authentication authentication,
            @PathVariable String slug,
            @PathVariable Long id,
            @Valid @RequestBody CrudRequest request) {
        requireWrite(authentication, slug);
        service.save(slug, id, request.values());
        return toDto(slug, service.get(slug, id));
    }

    @DeleteMapping("/{slug}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable String slug,
            @PathVariable Long id) {
        requireDelete(authentication, slug);
        service.delete(slug, id);
    }

    private ResourceDto toDto(String slug, Map<String, Object> row) {
        CrudRegistry.CrudDescriptor descriptor = registry.get(slug);
        Object id = row.get("id");
        return new ResourceDto(
                id instanceof Number number ? number.longValue() : null,
                slug,
                descriptor.module(),
                row
        );
    }

    private void requireRead(Authentication authentication, String slug) {
        if (!authorization.isAllowed(authentication, slug, "READ")) {
            throw new AccessDeniedException("Read access denied for ERP resource: " + slug);
        }
    }

    private void requireWrite(Authentication authentication, String slug) {
        if (!authorization.isAllowed(authentication, slug, "WRITE")) {
            throw new AccessDeniedException("Write access denied for ERP resource: " + slug);
        }
    }

    private void requireDelete(Authentication authentication, String slug) {
        if (!authorization.isAllowed(authentication, slug, "DELETE")) {
            throw new AccessDeniedException("Delete access denied for ERP resource: " + slug);
        }
    }
}
