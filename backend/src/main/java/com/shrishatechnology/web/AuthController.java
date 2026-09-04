package com.shrishatechnology.web;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishatechnology.config.AppSettings;
import com.shrishatechnology.config.WebConfig.AuthSupport;
import com.shrishatechnology.db.Db;
import com.shrishatechnology.service.RecaptchaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final AppSettings settings;
    private final Db db;
    private final RecaptchaService recaptcha;
    private final AuthSupport auth;
    private final ObjectMapper mapper;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final SecureRandom random = new SecureRandom();

    public AuthController(AppSettings settings, Db db, RecaptchaService recaptcha, AuthSupport auth, ObjectMapper mapper) {
        this.settings = settings;
        this.db = db;
        this.recaptcha = recaptcha;
        this.auth = auth;
        this.mapper = mapper;
    }

    @PostMapping("/auth/login")
    public RedirectView login(HttpServletRequest req, HttpSession session) {
        var captcha = recaptcha.validate(req);
        if (!captcha.ok()) {
            return authError(captcha.error(), "login");
        }
        String email = nv(req.getParameter("email")).toLowerCase();
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        if (email.isEmpty() || password.isEmpty()) {
            return authError("Email and password are required.", "login");
        }
        Map<String, Object> user = db.queryOne(
                "SELECT id, provider, name, email, password_hash, picture, role FROM users WHERE email = ?",
                email
        );
        Object hash = user == null ? null : user.get("password_hash");
        if (user == null || hash == null || String.valueOf(hash).isBlank()
                || !bcrypt.matches(password, String.valueOf(hash))) {
            return authError("Invalid email or password.", "login");
        }
        user = auth.syncAdminRole(user);
        session.setAttribute("user_id", user.get("id"));
        session.setAttribute("user", user);
        return afterAuth(req, session, "login");
    }

    @PostMapping("/auth/signup")
    public RedirectView signup(HttpServletRequest req, HttpSession session) {
        var captcha = recaptcha.validate(req);
        if (!captcha.ok()) {
            return authError(captcha.error(), "signup");
        }
        String name = nv(req.getParameter("name"));
        String email = nv(req.getParameter("email")).toLowerCase();
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return authError("Name, email and password are required.", "signup");
        }
        if (db.queryOne("SELECT id FROM users WHERE email = ?", email) != null) {
            return authError("Email already exists. Please login.", "signup");
        }
        String role = auth.roleForEmail(email, "user");
        String passwordHash = bcrypt.encode(password);
        db.execute(
                """
                INSERT INTO users (provider, name, email, password_hash, role)
                VALUES ('local', ?, ?, ?, ?)
                """,
                name, email, passwordHash, role
        );
        Map<String, Object> inserted = db.queryOne("SELECT id FROM users WHERE email = ?", email);
        session.setAttribute("user_id", inserted.get("id"));
        session.setAttribute("user", Map.of(
                "id", inserted.get("id"),
                "provider", "local",
                "name", name,
                "email", email,
                "role", role
        ));
        return afterAuth(req, session, "signup");
    }

    @RequestMapping(value = "/auth/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView logout(HttpSession session) {
        session.invalidate();
        return new RedirectView("/");
    }

    @GetMapping("/auth/google")
    public RedirectView googleStart(HttpServletRequest req, HttpSession session) {
        return oauthStart("google", req, session);
    }

    @GetMapping("/auth/google/callback")
    public RedirectView googleCb(HttpServletRequest req, HttpSession session) {
        return oauthCallback("google", req, session);
    }

    @PostMapping("/auth/google/popup")
    public RedirectView googlePopup(HttpServletRequest req, HttpSession session) {
        String returnTo = "signup".equals(req.getParameter("from")) ? "signup" : "login";
        String code = nv(req.getParameter("code"));
        if (code.isEmpty()) {
            return authError("Google login cancelled. Try again.", returnTo);
        }
        Map<String, String> cfg = oauthConfig("google", req);
        if (cfg == null) {
            return authError("Google login is not configured yet.", returnTo);
        }
        cfg.put("redirect_uri", "postmessage");
        try {
            return finishOAuth("google", code, cfg, session, returnTo);
        } catch (Exception e) {
            String msg = e.getMessage() == null || e.getMessage().isBlank()
                    ? "Google login failed."
                    : e.getMessage();
            return authError(msg, returnTo);
        }
    }

    @PostMapping("/auth/google/id-token")
    public RedirectView googleIdToken(HttpServletRequest req, HttpSession session) {
        String returnTo = "signup".equals(req.getParameter("from")) ? "signup" : "login";
        String jwt = nv(req.getParameter("credential"));
        if (jwt.isEmpty()) {
            return authError("Google login cancelled. Try again.", returnTo);
        }
        if (settings.googleClientId() == null) {
            return authError("Google login is not configured yet.", returnTo);
        }
        try {
            JsonNode p = getJson(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + enc(jwt),
                    null,
                    Map.of()
            );
            String aud = p.path("aud").asText("");
            if (!settings.googleClientId().equals(aud)) {
                return authError("Google login could not be verified.", returnTo);
            }
            String email = p.path("email").asText("").trim().toLowerCase();
            if (email.isEmpty()) {
                return authError("Google account email not available.", returnTo);
            }
            Map<String, String> profile = Map.of(
                    "providerId", p.path("sub").asText(""),
                    "name", firstNonBlank(p.path("name").asText(), p.path("given_name").asText(), "User"),
                    "email", email,
                    "picture", p.path("picture").asText("")
            );
            return loginAsUser("google", profile, session, returnTo);
        } catch (Exception e) {
            return authError("Google login failed. Please try again.", returnTo);
        }
    }

    @GetMapping("/auth/github")
    public RedirectView githubStart(HttpServletRequest req, HttpSession session) {
        return oauthStart("github", req, session);
    }

    @GetMapping("/auth/github/callback")
    public RedirectView githubCb(HttpServletRequest req, HttpSession session) {
        return oauthCallback("github", req, session);
    }

    @GetMapping("/auth/microsoft")
    public RedirectView msStart(HttpServletRequest req, HttpSession session) {
        return oauthStart("microsoft", req, session);
    }

    @GetMapping("/auth/microsoft/callback")
    public RedirectView msCb(HttpServletRequest req, HttpSession session) {
        return oauthCallback("microsoft", req, session);
    }

    private RedirectView oauthStart(String provider, HttpServletRequest req, HttpSession session) {
        String returnTo = "signup".equals(req.getParameter("from")) ? "signup" : "login";
        session.setAttribute("oauth_return_to", returnTo);
        session.setAttribute("oauth_provider", provider);
        Map<String, String> cfg = oauthConfig(provider, req);
        if (cfg == null) {
            String hint = switch (provider) {
                case "google" -> "GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET";
                case "github" -> "GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET";
                default -> "MICROSOFT_CLIENT_ID and MICROSOFT_CLIENT_SECRET";
            };
            return authError(cap(provider) + " login is not configured. Add " + hint + " on Render.", returnTo);
        }
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        String state = HexFormat.of().formatHex(buf);
        session.setAttribute("oauth_state", state);
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(cfg.get("auth_url"))
                .queryParam("client_id", cfg.get("client_id"))
                .queryParam("redirect_uri", cfg.get("redirect_uri"))
                .queryParam("response_type", "code")
                .queryParam("scope", cfg.get("scope"))
                .queryParam("state", state);
        if ("google".equals(provider)) {
            b.queryParam("access_type", "online").queryParam("prompt", "select_account");
        }
        if ("microsoft".equals(provider)) {
            b.queryParam("response_mode", "query");
        }
        try {
            return new RedirectView(b.encode().build().toUriString());
        } catch (Exception e) {
            return authError(cap(provider) + " login could not start. Please try again.", returnTo);
        }
    }

    private RedirectView oauthCallback(String provider, HttpServletRequest req, HttpSession session) {
        String returnTo = "signup".equals(String.valueOf(session.getAttribute("oauth_return_to"))) ? "signup" : "login";
        String googleError = nv(req.getParameter("error"));
        if (!googleError.isEmpty()) {
            return authError(oauthProviderError(provider, googleError, nv(req.getParameter("error_description"))), returnTo);
        }
        String code = nv(req.getParameter("code"));
        String state = nv(req.getParameter("state"));
        String expected = String.valueOf(session.getAttribute("oauth_state"));
        if (code.isEmpty() || state.isEmpty() || !state.equals(expected)) {
            return authError("Google login session expired. Open http://localhost:3000/login and try again.", returnTo);
        }
        Object sessProvider = session.getAttribute("oauth_provider");
        if (sessProvider != null && !provider.equals(sessProvider)) {
            return authError("Login session mismatch. Please try again.", returnTo);
        }
        session.removeAttribute("oauth_state");
        session.removeAttribute("oauth_return_to");
        session.removeAttribute("oauth_provider");
        Map<String, String> cfg = oauthConfig(provider, req);
        if (cfg == null) {
            return authError(cap(provider) + " login is not configured yet.", returnTo);
        }
        try {
            return finishOAuth(provider, code, cfg, session, returnTo);
        } catch (Exception e) {
            String msg = e.getMessage() == null || e.getMessage().isBlank()
                    ? cap(provider) + " login failed."
                    : e.getMessage();
            return authError(msg, returnTo);
        }
    }

    private Map<String, String> oauthConfig(String provider, HttpServletRequest req) {
        String base = settings.baseUrl();
        if (base == null) {
            base = req.getScheme() + "://" + req.getHeader("Host");
        }
        base = base.replaceAll("/$", "");
        Map<String, String> cfg = new LinkedHashMap<>();
        switch (provider) {
            case "google" -> {
                if (settings.googleClientId() == null || settings.googleClientSecret() == null) {
                    return null;
                }
                cfg.put("label", "Google");
                cfg.put("client_id", settings.googleClientId());
                cfg.put("client_secret", settings.googleClientSecret());
                cfg.put("redirect_uri", localRedirect(req, settings.googleRedirectUri(), base, "/auth/google/callback"));
                cfg.put("scope", "openid email profile");
                cfg.put("auth_url", "https://accounts.google.com/o/oauth2/v2/auth");
                cfg.put("token_url", "https://oauth2.googleapis.com/token");
            }
            case "github" -> {
                if (settings.githubClientId() == null || settings.githubClientSecret() == null) {
                    return null;
                }
                cfg.put("label", "GitHub");
                cfg.put("client_id", settings.githubClientId());
                cfg.put("client_secret", settings.githubClientSecret());
                cfg.put("redirect_uri", settings.githubRedirectUri() != null
                        ? settings.githubRedirectUri()
                        : base + "/auth/github/callback");
                cfg.put("scope", "user:email");
                cfg.put("auth_url", "https://github.com/login/oauth/authorize");
                cfg.put("token_url", "https://github.com/login/oauth/access_token");
                cfg.put("token_accept_json", "1");
            }
            case "microsoft" -> {
                if (settings.microsoftClientId() == null || settings.microsoftClientSecret() == null) {
                    return null;
                }
                cfg.put("label", "Microsoft");
                cfg.put("client_id", settings.microsoftClientId());
                cfg.put("client_secret", settings.microsoftClientSecret());
                cfg.put("redirect_uri", settings.microsoftRedirectUri() != null
                        ? settings.microsoftRedirectUri()
                        : base + "/auth/microsoft/callback");
                cfg.put("scope", "openid profile email User.Read");
                cfg.put("auth_url", "https://login.microsoftonline.com/common/oauth2/v2.0/authorize");
                cfg.put("token_url", "https://login.microsoftonline.com/common/oauth2/v2.0/token");
            }
            default -> {
                return null;
            }
        }
        return cfg;
    }

    private String oauthExchange(String provider, String code, Map<String, String> cfg) throws Exception {
        String form = "client_id=" + enc(cfg.get("client_id"))
                + "&client_secret=" + enc(cfg.get("client_secret"))
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(cfg.get("redirect_uri"))
                + "&grant_type=authorization_code";
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(cfg.get("token_url")))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form));
        if ("1".equals(cfg.get("token_accept_json"))) {
            b.header("Accept", "application/json");
        }
        HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException(tokenErrorMessage(cfg.get("label"), resp.body()));
        }
        JsonNode payload;
        try {
            payload = mapper.readTree(resp.body());
        } catch (Exception e) {
            payload = mapper.createObjectNode();
            for (String p : resp.body().split("&")) {
                int i = p.indexOf('=');
                if (i > 0) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) payload)
                            .put(p.substring(0, i), p.substring(i + 1));
                }
            }
        }
        String token = payload.path("access_token").asText("");
        if (token.isBlank()) {
            String apiErr = payload.path("error").asText("");
            if (!apiErr.isBlank()) {
                throw new IllegalStateException(tokenErrorMessage(cfg.get("label"), resp.body()));
            }
            throw new IllegalStateException(cfg.get("label") + " access token missing.");
        }
        return token;
    }

    private Map<String, String> oauthProfile(String provider, String accessToken) throws Exception {
        if ("google".equals(provider)) {
            JsonNode p = getJson("https://openidconnect.googleapis.com/v1/userinfo", accessToken, Map.of());
            String email = p.path("email").asText("").trim().toLowerCase();
            if (email.isEmpty()) {
                throw new IllegalStateException("Google account email not available.");
            }
            return Map.of(
                    "providerId", p.path("sub").asText(""),
                    "name", firstNonBlank(p.path("name").asText(), p.path("given_name").asText(), "User"),
                    "email", email,
                    "picture", p.path("picture").asText("")
            );
        }
        if ("github".equals(provider)) {
            Map<String, String> headers = Map.of(
                    "Accept", "application/vnd.github+json",
                    "User-Agent", "Shrisha-Technology-Website"
            );
            JsonNode user = getJson("https://api.github.com/user", accessToken, headers);
            String email = user.path("email").asText("").trim().toLowerCase();
            if (email.isEmpty()) {
                JsonNode emails = getJson("https://api.github.com/user/emails", accessToken, headers);
                if (emails.isArray()) {
                    for (JsonNode e : emails) {
                        if (e.path("primary").asBoolean(false) && e.path("verified").asBoolean(false)) {
                            email = e.path("email").asText("").trim().toLowerCase();
                            break;
                        }
                    }
                    if (email.isEmpty()) {
                        for (JsonNode e : emails) {
                            if (e.path("verified").asBoolean(false)) {
                                email = e.path("email").asText("").trim().toLowerCase();
                                break;
                            }
                        }
                    }
                    if (email.isEmpty() && emails.size() > 0) {
                        email = emails.get(0).path("email").asText("").trim().toLowerCase();
                    }
                }
            }
            if (email.isEmpty()) {
                throw new IllegalStateException(
                        "GitHub email not available. In GitHub → Settings → Emails, add a verified email or make it public.");
            }
            return Map.of(
                    "providerId", user.path("id").asText(""),
                    "name", firstNonBlank(user.path("name").asText(), user.path("login").asText(), "GitHub User"),
                    "email", email,
                    "picture", user.path("avatar_url").asText("")
            );
        }
        JsonNode p = getJson("https://graph.microsoft.com/v1.0/me", accessToken, Map.of());
        String email = firstNonBlank(p.path("mail").asText(), p.path("userPrincipalName").asText(), "")
                .trim().toLowerCase();
        if (email.isEmpty()) {
            throw new IllegalStateException("Microsoft account email not available.");
        }
        return Map.of(
                "providerId", p.path("id").asText(""),
                "name", firstNonBlank(p.path("displayName").asText(), p.path("givenName").asText(), "Microsoft User"),
                "email", email,
                "picture", ""
        );
    }

    private JsonNode getJson(String url, String token, Map<String, String> extra) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET();
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }
        extra.forEach(b::header);
        HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("Could not fetch profile.");
        }
        return mapper.readTree(resp.body());
    }

    private long upsertUser(String provider, Map<String, String> profile) {
        String email = profile.get("email");
        String providerId = emptyToNull(profile.get("providerId"));
        String name = profile.getOrDefault("name", "User");
        String picture = emptyToNull(profile.get("picture"));
        Map<String, Object> user = db.queryOne(
                "SELECT id, provider, provider_id, email, role FROM users WHERE email = ?",
                email
        );
        String role = auth.roleForEmail(email, user == null ? "user" : String.valueOf(user.getOrDefault("role", "user")));
        if (user == null) {
            db.execute(
                    """
                    INSERT INTO users (provider, provider_id, name, email, picture, role)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    provider, providerId, name, email, picture, role
            );
            Map<String, Object> inserted = db.queryOne("SELECT id FROM users WHERE email = ?", email);
            return AuthSupport.toLong(inserted.get("id"));
        }
        db.execute(
                """
                UPDATE users
                SET provider = ?,
                    provider_id = COALESCE(?, provider_id),
                    name = ?,
                    picture = COALESCE(NULLIF(?, ''), picture),
                    role = ?,
                    updated_at = NOW()
                WHERE id = ?
                """,
                provider, providerId, name, picture == null ? "" : picture, role, AuthSupport.toLong(user.get("id"))
        );
        return AuthSupport.toLong(user.get("id"));
    }

    private RedirectView afterAuth(HttpServletRequest req, HttpSession session, String toast) {
        String next = req.getParameter("next");
        if (next != null && next.startsWith("/") && !next.startsWith("//")) {
            String sep = next.contains("?") ? "&" : "?";
            return new RedirectView(next + sep + "toast=" + toast);
        }
        Object stored = session.getAttribute("after_login_redirect");
        session.removeAttribute("after_login_redirect");
        if (stored instanceof String n && n.startsWith("/") && !n.startsWith("//")) {
            String sep = n.contains("?") ? "&" : "?";
            return new RedirectView(n + sep + "toast=" + toast);
        }
        return new RedirectView("/?toast=" + toast);
    }

    private RedirectView finishOAuth(
            String provider,
            String code,
            Map<String, String> cfg,
            HttpSession session,
            String returnTo
    ) throws Exception {
        String token = oauthExchange(provider, code, cfg);
        Map<String, String> profile = oauthProfile(provider, token);
        return loginAsUser(provider, profile, session, returnTo);
    }

    private RedirectView loginAsUser(String provider, Map<String, String> profile, HttpSession session, String returnTo) {
        long userId = upsertUser(provider, profile);
        session.setAttribute("user_id", userId);
        Map<String, Object> u = db.queryOne(
                "SELECT id, provider, name, email, picture, role FROM users WHERE id = ?",
                userId
        );
        u = auth.syncAdminRole(u);
        session.setAttribute("user", u);
        Object next = session.getAttribute("after_login_redirect");
        session.removeAttribute("after_login_redirect");
        if (next instanceof String n && n.startsWith("/") && !n.startsWith("//")) {
            String sep = n.contains("?") ? "&" : "?";
            return new RedirectView(n + sep + "toast=" + ("signup".equals(returnTo) ? "signup" : "login"));
        }
        return new RedirectView("/?toast=" + ("signup".equals(returnTo) ? "signup" : "login"));
    }

    private String localRedirect(HttpServletRequest req, String configured, String base, String path) {
        if (!settings.onRender()) {
            String host = req.getHeader("Host");
            if (host != null && !host.isBlank()) {
                String h = host.replaceAll("/$", "");
                if (h.toLowerCase().startsWith("127.0.0.1")) {
                    h = "localhost" + (h.contains(":") ? h.substring(h.indexOf(':')) : "");
                }
                return req.getScheme() + "://" + h + path;
            }
        }
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return base + path;
    }

    private static String oauthProviderError(String provider, String error, String description) {
        return switch (error) {
            case "redirect_uri_mismatch" ->
                    "Google redirect URI match nahi hui. Console mein add karo: http://localhost:3000/auth/google/callback";
            case "access_denied" ->
                    "Google ne allow nahi kiya. OAuth consent screen → Testing ho to apna Gmail Test users mein add karo.";
            case "deleted_client", "unauthorized_client", "invalid_client" ->
                    "Google OAuth client invalid hai. Console se naya Web client banao aur .env update karo.";
            default -> cap(provider) + " login failed: " + (description.isEmpty() ? error : description);
        };
    }

    private String tokenErrorMessage(String label, String body) {
        try {
            JsonNode n = mapper.readTree(body == null ? "{}" : body);
            String err = n.path("error").asText("");
            if ("invalid_client".equals(err) || "unauthorized_client".equals(err)) {
                return "Google Client ID / Secret galat hain. .env check karo aur server restart karo.";
            }
            if ("redirect_uri_mismatch".equals(err)) {
                return "Google redirect URI match nahi hui. Console mein http://localhost:3000/auth/google/callback add karo.";
            }
        } catch (Exception ignored) {
            // fall through
        }
        return label + " token exchange failed. Google Console redirect URI check karo.";
    }

    private RedirectView authError(String message, String returnTo) {
        String page = "signup".equals(returnTo) ? "/signup" : "/login";
        return new RedirectView(page + "?error=" + enc(message));
    }

    private static String nv(String s) {
        return s == null ? "" : s.trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String cap(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
