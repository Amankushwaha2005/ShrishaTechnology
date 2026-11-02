package com.shrishatechnology.web;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import com.shrishatechnology.db.Db;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PortfolioController {

    private static final Set<String> CATEGORIES = Set.of("Website", "App", "Branding", "Student", "SEO", "Other");
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Db db;
    private final AdminGuard guard;
    private final TemplateRenderer templates;
    private final Path uploadDir;

    public PortfolioController(Db db, AdminGuard guard, TemplateRenderer templates, ProjectPaths paths) {
        this.db = db;
        this.guard = guard;
        this.templates = templates;
        this.uploadDir = paths.root().resolve("data").resolve("uploads");
    }

    @GetMapping("/api/portfolio")
    public ResponseEntity<Map<String, Object>> publicList() {
        List<Map<String, Object>> projects = new ArrayList<>();
        try {
            for (Map<String, Object> row : db.query("""
                    SELECT id, title, category, summary, image_url, live_url, details
                    FROM portfolio_projects
                    WHERE published = 1
                    ORDER BY sort_order ASC, id DESC
                    """)) {
                projects.add(toPublic(row));
            }
        } catch (Exception ignored) {
            // Table may not exist yet on the first request before migration finishes
        }
        return ResponseEntity.ok(Map.of("projects", projects));
    }

    @GetMapping("/admin/portfolio")
    public Object page(
            HttpServletRequest req,
            HttpSession session,
            @RequestParam(required = false) Long edit,
            @RequestParam(required = false) Long order,
            @RequestParam(required = false) String flash,
            @RequestParam(required = false) String err
    ) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> form = emptyForm();
        if (edit != null) {
            Map<String, Object> row = find(edit);
            if (row != null) {
                form = toForm(row);
            }
        } else if (order != null) {
            Map<String, Object> existing = db.queryOne(
                    "SELECT id FROM portfolio_projects WHERE order_id = ? ORDER BY id DESC",
                    order
            );
            if (existing != null) {
                return new RedirectView("/admin/portfolio?edit=" + existing.get("id"));
            }
            Map<String, Object> ord = db.queryOne(
                    "SELECT id, service, plan, delivered_at, status FROM orders WHERE id = ?",
                    order
            );
            if (ord != null) {
                String service = str(ord.get("service"));
                String plan = str(ord.get("plan"));
                form.put("title", service.isBlank() ? "Delivered project" : service);
                form.put("summary", plan.isBlank() ? "" : plan);
                form.put("category", guessCategory(service));
                form.put("orderId", order);
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        try {
            for (Map<String, Object> row : db.query("""
                    SELECT id, title, category, summary, image_url, live_url, published, sort_order, order_id, created_at
                    FROM portfolio_projects
                    ORDER BY sort_order ASC, id DESC
                    """)) {
                Map<String, Object> item = new LinkedHashMap<>(row);
                item.put("visible", isPublished(row.get("published")));
                items.add(item);
            }
        } catch (Exception ignored) {
            items = List.of();
        }

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("title", "Portfolio | " + Site.BRAND);
        ctx.put("brand", Site.BRAND);
        ctx.put("authUser", session.getAttribute("user"));
        ctx.put("items", items);
        ctx.put("form", form);
        ctx.put("categories", List.of("Website", "App", "Branding", "Student", "SEO", "Other"));
        ctx.put("editing", edit != null && form.get("id") != null);
        ctx.put("flash", flash == null ? "" : flash);
        ctx.put("err", err == null ? "" : err);
        return templates.render("admin/portfolio.html", ctx);
    }

    @PostMapping("/admin/portfolio")
    public Object save(
            HttpServletRequest req,
            HttpSession session,
            @RequestParam(required = false) Long id,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String details,
            @RequestParam(name = "live_url", required = false) String liveUrl,
            @RequestParam(name = "image_url", required = false) String imageUrl,
            @RequestParam(name = "sort_order", required = false) String sortOrder,
            @RequestParam(name = "order_id", required = false) String orderId,
            @RequestParam(required = false) String published,
            @RequestParam(required = false) MultipartFile file
    ) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        try {
            String cleanTitle = clip(title, 140);
            if (cleanTitle.isBlank()) {
                throw new IllegalArgumentException("Title is required.");
            }
            String cleanCategory = CATEGORIES.contains(category) ? category : "Other";
            String cleanSummary = clip(summary, 500);
            String cleanDetails = clip(details, 2000);
            String cleanLive = cleanUrl(liveUrl);
            int sort = parseInt(sortOrder, 0);
            Long linkedOrder = parseLong(orderId);
            int visible = "1".equals(published) ? 1 : 0;

            String image = cleanUrl(imageUrl);
            if (file != null && !file.isEmpty()) {
                image = saveUpload(file);
            }

            if (id == null) {
                db.execute("""
                        INSERT INTO portfolio_projects
                          (title, category, summary, image_url, live_url, details, published, sort_order, order_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        cleanTitle, cleanCategory, cleanSummary, image, cleanLive, cleanDetails, visible, sort, linkedOrder);
            } else {
                Map<String, Object> existing = find(id);
                if (existing == null) {
                    throw new IllegalArgumentException("That portfolio project was not found.");
                }
                if ((file == null || file.isEmpty()) && (imageUrl == null || imageUrl.isBlank())) {
                    image = str(existing.get("image_url"));
                }
                db.execute("""
                        UPDATE portfolio_projects
                        SET title=?, category=?, summary=?, image_url=?, live_url=?, details=?,
                            published=?, sort_order=?, order_id=?
                        WHERE id=?
                        """,
                        cleanTitle, cleanCategory, cleanSummary, image, cleanLive, cleanDetails, visible, sort, linkedOrder, id);
            }
            return new RedirectView("/admin/portfolio?flash=saved");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Could not save the project." : e.getMessage();
            return new RedirectView("/admin/portfolio?err=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/admin/portfolio/{id}/toggle")
    public Object toggle(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> row = find(id);
        if (row != null) {
            int next = isPublished(row.get("published")) ? 0 : 1;
            db.execute("UPDATE portfolio_projects SET published=? WHERE id=?", next, id);
        }
        return new RedirectView("/admin/portfolio?flash=toggled");
    }

    @PostMapping("/admin/portfolio/{id}/delete")
    public Object delete(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        db.execute("DELETE FROM portfolio_projects WHERE id=?", id);
        return new RedirectView("/admin/portfolio?flash=deleted");
    }

    private Map<String, Object> find(long id) {
        try {
            return db.queryOne("SELECT * FROM portfolio_projects WHERE id=?", id);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toPublic(Map<String, Object> row) {
        String live = str(row.get("live_url"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(row.get("id")));
        item.put("title", str(row.get("title")));
        item.put("category", str(row.get("category")));
        item.put("summary", str(row.get("summary")));
        item.put("image", str(row.get("image_url")));
        item.put("available", !live.isBlank());
        item.put("portfolioUrl", live);
        item.put("delivered", true);
        List<String> details = new ArrayList<>();
        for (String line : str(row.get("details")).split("\\R")) {
            String bit = line.trim();
            if (!bit.isEmpty()) {
                details.add(bit);
            }
        }
        item.put("details", details);
        return item;
    }

    private Map<String, Object> emptyForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", null);
        form.put("title", "");
        form.put("category", "Website");
        form.put("summary", "");
        form.put("details", "");
        form.put("liveUrl", "");
        form.put("imageUrl", "");
        form.put("sortOrder", 0);
        form.put("orderId", "");
        form.put("published", true);
        return form;
    }

    private Map<String, Object> toForm(Map<String, Object> row) {
        Map<String, Object> form = emptyForm();
        form.put("id", row.get("id"));
        form.put("title", str(row.get("title")));
        form.put("category", str(row.get("category")));
        form.put("summary", str(row.get("summary")));
        form.put("details", str(row.get("details")));
        form.put("liveUrl", str(row.get("live_url")));
        form.put("imageUrl", str(row.get("image_url")));
        form.put("sortOrder", row.get("sort_order") == null ? 0 : row.get("sort_order"));
        form.put("orderId", row.get("order_id") == null ? "" : row.get("order_id"));
        form.put("published", isPublished(row.get("published")));
        return form;
    }

    private String saveUpload(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = ext(original);
        if (!IMAGE_EXT.contains(ext)) {
            throw new IllegalArgumentException("Upload a JPG, PNG, WEBP or GIF image.");
        }
        if (file.getSize() > 8L * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be 8 MB or smaller.");
        }
        Files.createDirectories(uploadDir);
        String name = "portfolio-" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        file.transferTo(uploadDir.resolve(name).toFile());
        return "/uploads/" + name;
    }

    private static String guessCategory(String service) {
        String s = service.toLowerCase(Locale.ROOT);
        if (s.contains("app")) {
            return "App";
        }
        if (s.contains("logo") || s.contains("brand")) {
            return "Branding";
        }
        if (s.contains("student") || s.contains("final year")) {
            return "Student";
        }
        if (s.contains("seo")) {
            return "SEO";
        }
        if (s.contains("web") || s.contains("site")) {
            return "Website";
        }
        return "Other";
    }

    private static boolean isPublished(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String cleanUrl(String raw) {
        String v = str(raw);
        if (v.isBlank()) {
            return "";
        }
        if (v.startsWith("/uploads/") && !v.contains("..")) {
            return v;
        }
        try {
            URI uri = URI.create(v);
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return v;
            }
        } catch (Exception ignored) {
            // fall through
        }
        throw new IllegalArgumentException("Image and live links must be http or https URLs.");
    }

    private static String clip(String raw, int max) {
        String v = str(raw);
        return v.length() <= max ? v : v.substring(0, max);
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(str(raw));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Long parseLong(String raw) {
        String v = str(raw);
        if (v.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static String ext(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
