package com.shrishatechnology.config;

import java.io.IOException;
import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Keep local cookies + Google redirect URI on localhost (Google Console usually lists this). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalHostCanonicalFilter extends OncePerRequestFilter {

    private final AppSettings settings;

    public LocalHostCanonicalFilter(AppSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!settings.onRender()) {
            String host = request.getHeader("Host");
            if (host != null && host.toLowerCase(Locale.ROOT).startsWith("127.0.0.1")) {
                String port = "";
                int colon = host.indexOf(':');
                if (colon >= 0) {
                    port = host.substring(colon);
                }
                String target = "http://localhost" + port + request.getRequestURI();
                String query = request.getQueryString();
                if (query != null && !query.isBlank()) {
                    target += "?" + query;
                }
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", target);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
