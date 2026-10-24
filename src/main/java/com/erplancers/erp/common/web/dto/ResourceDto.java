package com.erplancers.erp.common.web.dto;

import java.util.Map;

public record ResourceDto(
        Long id,
        String resource,
        String module,
        Map<String, Object> values
) {}
