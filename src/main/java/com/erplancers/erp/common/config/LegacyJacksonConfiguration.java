package com.erplancers.erp.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the Jackson 2 mapper used by legacy ERP services.
 *
 * Spring Boot 4's default JSON integration uses Jackson 3; components that
 * still use com.fasterxml.jackson.databind.ObjectMapper need an explicit
 * Jackson 2 bean.
 */
@Configuration(proxyBeanMethods = false)
public class LegacyJacksonConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper legacyJacksonObjectMapper() {
        return new ObjectMapper();
    }
}
