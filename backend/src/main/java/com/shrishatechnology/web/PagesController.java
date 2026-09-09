package com.shrishatechnology.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import com.shrishatechnology.config.AppSettings;
import com.shrishatechnology.config.WebConfig;
import com.shrishatechnology.config.WebConfig.AuthSupport;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PagesController {

    private final TemplateRenderer templates;
    private final AppSettings settings;
    private final AuthSupport auth;
    private final CampaignStore campaigns;

    public PagesController(TemplateRenderer templates, AppSettings settings, AuthSupport auth, CampaignStore campaigns) {
        this.templates = templates;
        this.settings = settings;
        this.auth = auth;
        this.campaigns = campaigns;
    }

    @GetMapping("/")
    public ResponseEntity<String> home(HttpServletRequest req) {
        return page(req, "pages/index.html", "home", "Shrisha Technology");
    }

    @GetMapping("/pricing")
    public ResponseEntity<String> pricing(HttpServletRequest req) {
        return page(req, "pages/pricing.html", "pricing", "Pricing | Shrisha Technology");
    }

    @GetMapping("/services")
    public ResponseEntity<String> services(HttpServletRequest req) {
        return page(req, "pages/services.html", "services", "Services | Shrisha Technology");
    }

    @GetMapping("/portfolio")
    public ResponseEntity<String> portfolio(HttpServletRequest req) {
        return page(req, "pages/portfolio.html", "portfolio", "Portfolio | Shrisha Technology");
    }

    @GetMapping("/news")
    public ResponseEntity<String> news(HttpServletRequest req) {
        return page(req, "pages/news.html", "news", "News Hub | Shrisha Technology");
    }

    @GetMapping("/about")
    public ResponseEntity<String> about(HttpServletRequest req) {
        return page(req, "pages/about.html", "about", "About | Shrisha Technology");
    }

    @GetMapping("/contact")
    public ResponseEntity<String> contact(HttpServletRequest req) {
        return page(req, "pages/contact.html", "contact", "Contact | Shrisha Technology");
    }

    @GetMapping("/work")
    public ResponseEntity<String> work(HttpServletRequest req) {
        return page(req, "pages/work.html", "work", "Work With Us | Shrisha Technology");
    }

    @GetMapping({
            "/contact.html",
            "/index.html",
            "/pricing.html",
            "/services.html",
            "/portfolio.html",
            "/about.html",
            "/login.html",
            "/signup.html",
            "/work.html",
            "/news.html"
    })
    public RedirectView htmlAlias(HttpServletRequest req) {
        String path = req.getRequestURI();
        String dest = switch (path) {
            case "/index.html" -> "/";
            case "/pricing.html" -> "/pricing";
            case "/services.html" -> "/services";
            case "/portfolio.html" -> "/portfolio";
            case "/about.html" -> "/about";
            case "/contact.html" -> "/contact";
            case "/login.html" -> "/login";
            case "/signup.html" -> "/signup";
            case "/work.html" -> "/work";
            case "/news.html" -> "/news";
            default -> "/";
        };
        String qs = req.getQueryString();
        RedirectView view = new RedirectView(qs == null || qs.isBlank() ? dest : dest + "?" + qs);
        view.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return view;
    }

    @GetMapping("/legal")
    public RedirectView legalIndex() {
        return new RedirectView("/legal/terms");
    }

    @GetMapping("/legal/terms")
    public ResponseEntity<String> legalTerms(HttpServletRequest req) {
        return page(req, "pages/legal-terms.html", "legal-terms", "Terms & Conditions | Shrisha Technology");
    }

    @GetMapping("/legal/advance")
    public ResponseEntity<String> legalAdvance(HttpServletRequest req) {
        return page(req, "pages/legal-advance.html", "legal-advance", "Advance Payment | Shrisha Technology");
    }

    @GetMapping("/legal/refund")
    public ResponseEntity<String> legalRefund(HttpServletRequest req) {
        return page(req, "pages/legal-refund.html", "legal-refund", "Refund Policy | Shrisha Technology");
    }

    @GetMapping("/legal/timeline")
    public ResponseEntity<String> legalTimeline(HttpServletRequest req) {
        return page(req, "pages/legal-timeline.html", "legal-timeline", "Project Timeline | Shrisha Technology");
    }

    @GetMapping("/legal/privacy")
    public ResponseEntity<String> legalPrivacy(HttpServletRequest req) {
        return page(req, "pages/legal-privacy.html", "legal-privacy", "Privacy | Shrisha Technology");
    }

    @GetMapping("/login")
    public Object login(HttpServletRequest req, HttpSession session) {
        if (session.getAttribute("user_id") != null) {
            return new RedirectView("/");
        }
        String nextRaw = req.getParameter("next");
        if (nextRaw != null && nextRaw.startsWith("/") && !nextRaw.startsWith("//")) {
            session.setAttribute("after_login_redirect", nextRaw);
        } else {
            session.removeAttribute("after_login_redirect");
        }
        return page(req, "pages/login.html", "login", "Login | Shrisha Technology");
    }

    @GetMapping("/signup")
    public Object signup(HttpServletRequest req, HttpSession session) {
        if (session.getAttribute("user_id") != null) {
            return new RedirectView("/");
        }
        return page(req, "pages/signup.html", "signup", "Signup | Shrisha Technology");
    }

    private ResponseEntity<String> page(HttpServletRequest req, String template, String key, String title) {
        Map<String, Object> ctx = WebConfig.pageContext(settings, req, Map.of("key", key, "title", title), auth);
        campaigns.attachPublic(ctx);
        return templates.render(template, ctx);
    }
}
