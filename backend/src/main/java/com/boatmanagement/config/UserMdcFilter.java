package com.boatmanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserMdcFilter extends OncePerRequestFilter {

    private static final String MDC_USER_KEY = "user";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String username = jwtAuth.getToken().getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwtAuth.getToken().getSubject();
                }
                MDC.put(MDC_USER_KEY, username);
            } else {
                MDC.put(MDC_USER_KEY, "anonymous");
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USER_KEY);
        }
    }
}
