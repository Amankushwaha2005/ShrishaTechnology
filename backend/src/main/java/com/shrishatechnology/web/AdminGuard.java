package com.shrishatechnology.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.RedirectView;

import com.shrishatechnology.config.AppSettings;
import com.shrishatechnology.config.WebConfig.AuthSupport;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class AdminGuard {

    private final AppSettings settings;
    private final AuthSupport auth;
    private final TemplateRenderer templates;

    public AdminGuard(AppSettings settings, AuthSupport auth, TemplateRenderer templates) {
        this.settings = settings;
        this.auth = auth;
        this.templates = templates;
    }

    public Object denyIfNotAdmin(HttpServletRequest req, HttpSession session) {
        if (session.getAttribute("user_id") == null) {
            String next = req.getRequestURI();
            if (next == null || !next.startsWith("/admin")) {
                next = "/admin";
            }
            return new RedirectView("/login?next=" + next);
        }
        Map<String, Object> user = auth.refresh(session);
        user = auth.ensureLocalAdmin(session, user);
        if (!auth.isAdmin(user)) {
            String loginEmail = user == null ? "" : String.valueOf(user.getOrDefault("email", ""));
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("brand", Site.BRAND);
            ctx.put("loginEmail", loginEmail);
            ctx.put("adminEmailsConfigured", !settings.adminEmails().isBlank());
            ResponseEntity<String> body = templates.render("admin/forbidden.html", ctx);
            return ResponseEntity.status(403).contentType(body.getHeaders().getContentType()).body(body.getBody());
        }
        return null;
    }
}
