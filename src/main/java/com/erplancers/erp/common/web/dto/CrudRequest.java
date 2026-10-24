package com.erplancers.erp.common.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public record CrudRequest(
        @NotNull Map<String, String> values
) {
    public CrudRequest {
        values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }
}
