package edu.itba.fieldops.api.config;

import edu.itba.fieldops.api.error.UnderMaintenance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

final class MaintenanceInterceptor implements HandlerInterceptor {
    private final FieldOpsProperties properties;

    MaintenanceInterceptor(FieldOpsProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        FieldOpsProperties.Maintenance maintenance = properties.getMaintenance();
        if (maintenance.isEnabled()) {
            throw new UnderMaintenance(maintenance.getMessage(), maintenance.getRetryAfterSeconds());
        }
        return true;
    }
}
