package com.shrishatechnology.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Map legacy *.html URLs to the live Spring routes (avoids 404 on /contact.html). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HtmlAliasFilter extends OncePerRequestFilter {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("/contact.html", "/contact"),
            Map.entry("/index.html", "/"),
            Map.entry("/pricing.html", "/pricing"),
            Map.entry("/services.html", "/services"),
            Map.entry("/portfolio.html", "/portfolio"),
            Map.entry("/about.html", "/about"),
            Map.entry("/login.html", "/login"),
            Map.entry("/signup.html", "/signup"),
            Map.entry("/work.html", "/work"),
            Map.entry("/news.html", "/news")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String dest = ALIASES.get(request.getRequestURI());
        if (dest != null) {
            String query = request.getQueryString();
            String target = query == null || query.isBlank() ? dest : dest + "?" + query;
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", target);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
