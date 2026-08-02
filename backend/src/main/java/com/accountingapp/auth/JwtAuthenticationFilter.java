package com.accountingapp.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests bearing {@code Authorization: Bearer <access token>}. Registered directly
 * into the Spring Security filter chain (see {@code SecurityConfig}) rather than as a bare
 * {@code @Component}, so it runs before authorization is evaluated.
 *
 * A stale-but-signature-valid token for a since-deactivated user is accepted here — access tokens
 * are short-lived (see {@code app.jwt.access-token-ttl-minutes}), and re-checking {@code User.active}
 * on every request would cost a DB round trip per call. Login and refresh both re-check it, so a
 * deactivated user can't obtain new tokens and loses access within one token lifetime.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            UUID userId = jwtService.parseUserId(header.substring(PREFIX.length()));
            if (userId != null) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
