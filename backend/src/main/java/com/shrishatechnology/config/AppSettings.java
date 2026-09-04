package com.shrishatechnology.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class AppSettings {

    public String env(String key) {
        return env(key, "");
    }

    public String env(String key, String fallback) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            v = System.getProperty(key);
        }
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return v.trim();
    }

    public boolean envBool(String key, boolean fallback) {
        String v = env(key, "");
        if (v.isEmpty()) {
            return fallback;
        }
        return Set.of("1", "true", "yes", "on").contains(v.toLowerCase(Locale.ROOT));
    }

    public boolean production() {
        String node = env("NODE_ENV", env("FLASK_ENV", "development"));
        return "production".equalsIgnoreCase(node);
    }

    public boolean onRender() {
        return env("RENDER").length() > 0;
    }

    public String secretKey() {
        String s = env("SESSION_SECRET", env("SECRET_KEY", "dev-secret-change-me"));
        return s.isBlank() ? "dev-secret-change-me" : s;
    }

    public String baseUrl() {
        String b = env("BASE_URL");
        return b.isBlank() ? null : b.replaceAll("/$", "");
    }

    public String adminEmails() {
        return env("ADMIN_EMAILS");
    }

    public String adminBootstrapSecret() {
        String s = env("ADMIN_BOOTSTRAP_SECRET");
        return s.isBlank() ? null : s;
    }

    /** Google's official reCAPTCHA v2 Checkbox test keys (always pass). */
    private static final String RECAPTCHA_V2_TEST_SITE = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";
    private static final String RECAPTCHA_V2_TEST_SECRET = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";

    public String recaptchaSiteKey() {
        if (!onRender()) {
            return RECAPTCHA_V2_TEST_SITE;
        }
        String s = env("RECAPTCHA_SITE_KEY");
        return s.isBlank() || s.startsWith("your-") ? RECAPTCHA_V2_TEST_SITE : s;
    }

    public String recaptchaSecretKey() {
        if (!onRender()) {
            return RECAPTCHA_V2_TEST_SECRET;
        }
        String s = env("RECAPTCHA_SECRET_KEY");
        return s.isBlank() || s.startsWith("your-") ? RECAPTCHA_V2_TEST_SECRET : s;
    }

    public String googleClientId() {
        return configuredSecret(env("GOOGLE_CLIENT_ID"));
    }

    public String googleClientSecret() {
        return configuredSecret(env("GOOGLE_CLIENT_SECRET"));
    }

    public String googleRedirectUri() {
        return emptyToNull(env("GOOGLE_REDIRECT_URI"));
    }

    public String githubClientId() {
        return emptyToNull(env("GITHUB_CLIENT_ID"));
    }

    public String githubClientSecret() {
        return emptyToNull(env("GITHUB_CLIENT_SECRET"));
    }

    public String githubRedirectUri() {
        return emptyToNull(env("GITHUB_REDIRECT_URI"));
    }

    public String microsoftClientId() {
        return emptyToNull(env("MICROSOFT_CLIENT_ID"));
    }

    public String microsoftClientSecret() {
        return emptyToNull(env("MICROSOFT_CLIENT_SECRET"));
    }

    public String microsoftRedirectUri() {
        return emptyToNull(env("MICROSOFT_REDIRECT_URI"));
    }

    public String razorpayKeyId() {
        return emptyToNull(env("RAZORPAY_KEY_ID"));
    }

    public String razorpayKeySecret() {
        return emptyToNull(env("RAZORPAY_KEY_SECRET"));
    }

    public int paymentAdvancePercent() {
        try {
            return Integer.parseInt(env("PAYMENT_ADVANCE_PERCENT", "50"));
        } catch (NumberFormatException e) {
            return 50;
        }
    }

    public boolean paymentEnabled() {
        return razorpayKeyId() != null && razorpayKeySecret() != null;
    }

    public boolean pgSsl() {
        return envBool("PGSSL", onRender());
    }

    public int dbPoolLimit() {
        try {
            return Integer.parseInt(env("DB_POOL_LIMIT", "10"));
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public String jdbcUrl() {
        String databaseUrl = env("DATABASE_URL");
        if (!databaseUrl.isBlank()) {
            return toJdbc(databaseUrl, pgSsl());
        }
        String host = env("PGHOST", env("DB_HOST", "127.0.0.1"));
        String port = env("PGPORT", env("DB_PORT", "5432"));
        String db = env("PGDATABASE", env("DB_NAME", "web_project"));
        String ssl = pgSsl() ? "?sslmode=require" : "";
        return "jdbc:postgresql://" + host + ":" + port + "/" + db + ssl;
    }

    public String jdbcUser() {
        String databaseUrl = env("DATABASE_URL");
        if (!databaseUrl.isBlank()) {
            String user = userFromUrl(databaseUrl);
            if (user != null) {
                return user;
            }
        }
        return env("PGUSER", env("DB_USER", "postgres"));
    }

    public String jdbcPassword() {
        String databaseUrl = env("DATABASE_URL");
        if (!databaseUrl.isBlank()) {
            String pass = passwordFromUrl(databaseUrl);
            if (pass != null) {
                return pass;
            }
        }
        return env("PGPASSWORD", env("DB_PASSWORD", ""));
    }

    public Set<String> adminEmailSet() {
        String raw = adminEmails();
        if (raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String configuredSecret(String s) {
        String v = emptyToNull(s);
        if (v == null) {
            return null;
        }
        String lower = v.toLowerCase();
        if (lower.startsWith("your-") || lower.startsWith("your_") || lower.contains("xxxxx")) {
            return null;
        }
        return v;
    }

    static String toJdbc(String url, boolean ssl) {
        String normalized = url.replace("postgres://", "postgresql://");
        if (!normalized.startsWith("postgresql://") && !normalized.startsWith("jdbc:")) {
            return url;
        }
        if (normalized.startsWith("jdbc:")) {
            return normalized;
        }
        URI u = URI.create(normalized);
        int port = u.getPort() > 0 ? u.getPort() : 5432;
        String path = u.getPath() == null || u.getPath().isBlank() ? "/web_project" : u.getPath();
        String query = u.getQuery() == null ? "" : u.getQuery();
        if (ssl && !query.contains("sslmode")) {
            query = query.isEmpty() ? "sslmode=require" : query + "&sslmode=require";
        }
        String q = query.isEmpty() ? "" : "?" + query;
        return "jdbc:postgresql://" + u.getHost() + ":" + port + path + q;
    }

    static String userFromUrl(String url) {
        try {
            URI u = URI.create(url.replace("postgres://", "postgresql://").replace("jdbc:postgresql://", "postgresql://"));
            String info = u.getUserInfo();
            if (info == null) {
                return null;
            }
            int c = info.indexOf(':');
            return c < 0 ? info : info.substring(0, c);
        } catch (Exception e) {
            return null;
        }
    }

    static String passwordFromUrl(String url) {
        try {
            URI u = URI.create(url.replace("postgres://", "postgresql://").replace("jdbc:postgresql://", "postgresql://"));
            String info = u.getUserInfo();
            if (info == null || !info.contains(":")) {
                return null;
            }
            return info.substring(info.indexOf(':') + 1);
        } catch (Exception e) {
            return null;
        }
    }
}
