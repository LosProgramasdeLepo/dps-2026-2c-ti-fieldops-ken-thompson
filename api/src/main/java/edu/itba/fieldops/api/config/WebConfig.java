package edu.itba.fieldops.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FieldOpsProperties.class)
class WebConfig implements WebMvcConfigurer {
    private final FieldOpsProperties properties;

    WebConfig(FieldOpsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MaintenanceInterceptor(properties)).addPathPatterns("/v1/**");
    }
}
