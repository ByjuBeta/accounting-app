package com.accountingapp.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(10)
public class OrganizationContextFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Organization-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER_NAME);
        try {
            if (header != null && !header.isBlank()) {
                OrganizationContext.set(UUID.fromString(header));
            }
            filterChain.doFilter(request, response);
        } finally {
            OrganizationContext.clear();
        }
    }
}
