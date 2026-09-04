package com.shrishatechnology.config;

import java.nio.file.Path;
import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.shrishatechnology.db.Db;
import com.shrishatechnology.db.SchemaMigrator;
import com.shrishatechnology.web.ProjectPaths;
import com.shrishatechnology.web.Site;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ProjectPaths paths;

    public WebConfig(ProjectPaths paths) {
        this.paths = paths;
    }

    @Bean
    public AuthSupport authSupport(AppSettings settings, Db db, SchemaMigrator schema) {
        return new AuthSupport(settings, db, schema);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = dirLocation(paths.root());
        registry.addResourceHandler("/*.css", "/*.js", "/health.html", "/service-topic.html")
                .addResourceLocations(root)
                .setCachePeriod(0);
        registry.addResourceHandler("/images/**")
                .addResourceLocations(dirLocation(paths.root().resolve("images")))
                .setCachePeriod(0);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(dirLocation(paths.root().resolve("data").resolve("uploads")))
                .setCachePeriod(0);
    }

    private static String dirLocation(Path dir) {
        String loc = dir.toAbsolutePath().normalize().toUri().toString();
        if (!loc.endsWith("/")) {
            loc += "/";
        }
        return loc;
    }

    public static Map<String, Object> pageContext(
            AppSettings settings,
            HttpServletRequest request,
            Map<String, Object> page,
            AuthSupport auth
    ) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("page", page);
        ctx.put("navItems", Site.NAV);
        ctx.put("brand", Site.BRAND);
        ctx.put("year", Year.now().getValue());
        ctx.putAll(auth.flags(request));
        String error = request.getParameter("error");
        ctx.put("authError", error == null ? "" : error);
        String nextRaw = request.getParameter("next");
        String loginNext = "";
        if (nextRaw != null && nextRaw.startsWith("/") && !nextRaw.startsWith("//")) {
            loginNext = nextRaw;
        }
        ctx.put("loginNext", loginNext);
        ctx.put("googleLoginEnabled", settings.googleClientId() != null && settings.googleClientSecret() != null);
        ctx.put("googleClientId", settings.googleClientId() == null ? "" : settings.googleClientId());
        ctx.put("githubLoginEnabled", settings.githubClientId() != null && settings.githubClientSecret() != null);
        ctx.put("microsoftLoginEnabled", settings.microsoftClientId() != null && settings.microsoftClientSecret() != null);
        ctx.put("recaptchaSiteKey", settings.recaptchaSiteKey());
        ctx.put("paymentAdvancePercent", settings.paymentAdvancePercent());
        return ctx;
    }

    public static class AuthSupport {
        private final AppSettings settings;
        private final Db db;
        private final SchemaMigrator schema;

        public AuthSupport(AppSettings settings, Db db, SchemaMigrator schema) {
            this.settings = settings;
            this.db = db;
            this.schema = schema;
        }

        public Map<String, Object> flags(HttpServletRequest request) {
            Map<String, Object> user = refresh(request.getSession(false));
            Map<String, Object> out = new HashMap<>();
            out.put("authUser", user);
            out.put("isAdmin", isAdmin(user));
            out.put("accountInitial", accountInitial(user));
            out.put("accountPicture", accountPicture(user));
            return out;
        }

        public Map<String, Object> refresh(HttpSession session) {
            if (session == null) {
                return null;
            }
            Object id = session.getAttribute("user_id");
            if (id == null) {
                session.removeAttribute("user");
                return null;
            }
            if (!schema.isAvailable()) {
                return null;
            }
            try {
                Map<String, Object> user = db.queryOne(
                        "SELECT id, provider, name, email, picture, role FROM users WHERE id = ?",
                        toLong(id)
                );
                user = syncAdminRole(user);
                if (user != null) {
                    session.setAttribute("user", user);
                    return user;
                }
            } catch (Exception ignored) {
                return null;
            }
            session.removeAttribute("user_id");
            session.removeAttribute("user");
            return null;
        }

        public boolean isAdmin(Map<String, Object> user) {
            if (user == null) {
                return false;
            }
            if ("admin".equals(String.valueOf(user.get("role")))) {
                return true;
            }
            String em = String.valueOf(user.getOrDefault("email", "")).trim().toLowerCase();
            return !em.isEmpty() && settings.adminEmailSet().contains(em);
        }

        public String roleForEmail(String email, String currentRole) {
            if ("admin".equals(currentRole)) {
                return "admin";
            }
            String em = email == null ? "" : email.trim().toLowerCase();
            if (!em.isEmpty() && settings.adminEmailSet().contains(em)) {
                return "admin";
            }
            return currentRole == null || currentRole.isBlank() ? "user" : currentRole;
        }

        public Map<String, Object> syncAdminRole(Map<String, Object> user) {
            if (user == null || user.get("id") == null) {
                return user;
            }
            String next = roleForEmail(String.valueOf(user.getOrDefault("email", "")),
                    String.valueOf(user.getOrDefault("role", "user")));
            if (next.equals(String.valueOf(user.get("role")))) {
                return user;
            }
            db.execute(
                    """
                    UPDATE users SET role = ?, updated_at = NOW() WHERE id = ?
                    """,
                    next,
                    toLong(user.get("id"))
            );
            Map<String, Object> updated = db.queryOne(
                    "SELECT id, provider, name, email, picture, role FROM users WHERE id = ?",
                    toLong(user.get("id"))
            );
            return updated != null ? updated : user;
        }

        public void promoteAdminEmails() {
            var emails = settings.adminEmailSet();
            if (emails.isEmpty()) {
                return;
            }
            for (String email : emails) {
                db.execute(
                        "UPDATE users SET role = 'admin', updated_at = NOW() WHERE LOWER(email) = ? AND role <> 'admin'",
                        email
                );
            }
        }

        /** First logged-in user on localhost becomes admin so /admin works without extra env. */
        public Map<String, Object> ensureLocalAdmin(HttpSession session, Map<String, Object> user) {
            if (settings.onRender() || user == null || user.get("id") == null) {
                return user;
            }
            if (isAdmin(user)) {
                return user;
            }
            Map<String, Object> existing = db.queryOne("SELECT id FROM users WHERE role = 'admin' LIMIT 1");
            if (existing != null) {
                return user;
            }
            db.execute("UPDATE users SET role = 'admin', updated_at = NOW() WHERE id = ?", user.get("id"));
            Map<String, Object> updated = db.queryOne(
                    "SELECT id, provider, name, email, picture, role FROM users WHERE id = ?",
                    toLong(user.get("id"))
            );
            if (updated != null) {
                session.setAttribute("user", updated);
                return updated;
            }
            return user;
        }

        public static long toLong(Object id) {
            if (id instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(String.valueOf(id));
        }

        private static String accountInitial(Map<String, Object> user) {
            if (user == null) {
                return "";
            }
            String name = String.valueOf(user.getOrDefault("name", "")).trim();
            if (!name.isEmpty()) {
                return name.substring(0, 1).toUpperCase();
            }
            String email = String.valueOf(user.getOrDefault("email", "")).trim();
            if (!email.isEmpty()) {
                return email.substring(0, 1).toUpperCase();
            }
            return "U";
        }

        private static String accountPicture(Map<String, Object> user) {
            if (user == null) {
                return "";
            }
            Object raw = user.get("picture");
            if (raw == null) {
                return "";
            }
            String url = String.valueOf(raw).trim();
            if (url.isEmpty() || "null".equalsIgnoreCase(url)) {
                return "";
            }
            return url;
        }
    }
}
