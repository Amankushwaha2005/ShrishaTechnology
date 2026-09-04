package com.shrishatechnology.web;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishatechnology.config.AppSettings;
import com.shrishatechnology.config.WebConfig.AuthSupport;
import com.shrishatechnology.db.Db;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    private static final Set<String> STATUSES = Set.of("new", "read", "archived");
    private static final Set<String> ROLES = Set.of("user", "admin");

    private final AppSettings settings;
    private final Db db;
    private final TemplateRenderer templates;
    private final AuthSupport auth;
    private final ObjectMapper mapper;
    private final AdminGuard guard;

    public AdminController(
            AppSettings settings,
            Db db,
            TemplateRenderer templates,
            AuthSupport auth,
            ObjectMapper mapper,
            AdminGuard guard
    ) {
        this.settings = settings;
        this.db = db;
        this.templates = templates;
        this.auth = auth;
        this.mapper = mapper;
        this.guard = guard;
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public Object dashboard(
            HttpServletRequest req,
            HttpSession session,
            @RequestParam(required = false) String flash,
            @RequestParam(required = false) String err
    ) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        auth.promoteAdminEmails();

        Timestamp weekAgo = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", count("SELECT COUNT(*) AS c FROM users"));
        stats.put("usersWeek", count("SELECT COUNT(*) AS c FROM users WHERE created_at >= ?", weekAgo));
        stats.put("contacts", count("SELECT COUNT(*) AS c FROM contact_submissions"));
        stats.put("contactsNew", count("SELECT COUNT(*) AS c FROM contact_submissions WHERE status = 'new'"));
        stats.put("work", count("SELECT COUNT(*) AS c FROM work_submissions"));
        stats.put("workNew", count("SELECT COUNT(*) AS c FROM work_submissions WHERE status = 'new'"));
        stats.put("chats", count("SELECT COUNT(*) AS c FROM chat_messages"));
        stats.put("chatsNew", count("SELECT COUNT(*) AS c FROM chat_messages WHERE status = 'new'"));
        stats.put("orders", count("SELECT COUNT(*) AS c FROM orders"));
        stats.put("ordersAdvancePaid", count(
                "SELECT COUNT(*) AS c FROM orders WHERE status IN ('advance_paid','completed','paid')"));
        stats.put("ordersCompleted", count("SELECT COUNT(*) AS c FROM orders WHERE status='completed'"));
        stats.put("ordersPending", count("SELECT COUNT(*) AS c FROM orders WHERE status='pending'"));
        Map<String, Object> revenue = queryOneSafe(
                "SELECT COALESCE(SUM(COALESCE(advance_paid_inr,0) + COALESCE(balance_paid_inr,0)),0) AS s FROM orders"
        );
        stats.put("revenuePaid", revenue == null ? 0 : toInt(revenue.get("s")));
        stats.put("ordersBalanceDue", countBalanceDue());
        stats.put("pipeline", toInt(stats.get("contactsNew")) + toInt(stats.get("workNew"))
                + toInt(stats.get("chatsNew")) + toInt(stats.get("ordersPending")));

        List<Map<String, Object>> orders = enrichOrders(querySafe("""
                SELECT id, public_id, name, email, phone, service, plan, status,
                       total_inr, amount_inr, advance_paid_inr, balance_paid_inr,
                       created_at, delivered_at
                FROM orders
                ORDER BY created_at DESC
                LIMIT 80
                """));
        List<Map<String, Object>> contacts = querySafe(
                "SELECT * FROM contact_submissions ORDER BY created_at DESC, id DESC LIMIT 40");
        List<Map<String, Object>> work = querySafe(
                "SELECT * FROM work_submissions ORDER BY created_at DESC, id DESC LIMIT 40");
        List<Map<String, Object>> chats = querySafe(
                "SELECT * FROM chat_messages ORDER BY created_at DESC, id DESC LIMIT 50");
        List<Map<String, Object>> users = querySafe("""
                SELECT id, provider, name, email, role, created_at
                FROM users
                ORDER BY id DESC
                LIMIT 120
                """);

        LocalDate start = LocalDate.now().minusDays(13);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            labels.add(start.plusDays(i).toString());
        }
        Map<String, Integer> contactS = series("contact_submissions");
        Map<String, Integer> workS = series("work_submissions");
        Map<String, Integer> orderS = series("orders");
        List<Integer> contactPts = labels.stream().map(d -> contactS.getOrDefault(d, 0)).toList();
        List<Integer> workPts = labels.stream().map(d -> workS.getOrDefault(d, 0)).toList();
        List<Integer> orderPts = labels.stream().map(d -> orderS.getOrDefault(d, 0)).toList();

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("title", "Admin | " + Site.BRAND);
        ctx.put("brand", Site.BRAND);
        ctx.put("authUser", session.getAttribute("user"));
        ctx.put("currentAdminId", session.getAttribute("user_id"));
        ctx.put("stats", stats);
        ctx.put("orders", orders);
        ctx.put("contacts", contacts);
        ctx.put("workRows", work);
        ctx.put("chats", chats);
        ctx.put("users", users);
        ctx.put("flash", flash == null ? "" : flash);
        ctx.put("err", err == null ? "" : err);
        ctx.put("chartLabelsJson", json(labels));
        ctx.put("chartContactJson", json(contactPts));
        ctx.put("chartWorkJson", json(workPts));
        ctx.put("chartOrdersJson", json(orderPts));
        return templates.render("admin/dashboard.html", ctx);
    }

    @PostMapping("/admin/submissions/contact/{id}/status")
    public Object contactStatus(HttpServletRequest req, HttpSession session, @PathVariable long id, @RequestParam String status) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        if (!STATUSES.contains(status)) {
            return new RedirectView("/admin?err=invalid");
        }
        db.execute("UPDATE contact_submissions SET status = ? WHERE id = ?", status, id);
        return new RedirectView("/admin#inbox-contact");
    }

    @PostMapping("/admin/submissions/contact/{id}/delete")
    public Object contactDelete(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        db.execute("DELETE FROM contact_submissions WHERE id = ?", id);
        return new RedirectView("/admin#inbox-contact");
    }

    @PostMapping("/admin/submissions/work/{id}/status")
    public Object workStatus(HttpServletRequest req, HttpSession session, @PathVariable long id, @RequestParam String status) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        if (!STATUSES.contains(status)) {
            return new RedirectView("/admin?err=invalid");
        }
        db.execute("UPDATE work_submissions SET status = ? WHERE id = ?", status, id);
        return new RedirectView("/admin#inbox-work");
    }

    @PostMapping("/admin/submissions/work/{id}/delete")
    public Object workDelete(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        db.execute("DELETE FROM work_submissions WHERE id = ?", id);
        return new RedirectView("/admin#inbox-work");
    }

    @PostMapping("/admin/submissions/chat/{id}/status")
    public Object chatStatus(HttpServletRequest req, HttpSession session, @PathVariable long id, @RequestParam String status) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        if (!STATUSES.contains(status)) {
            return new RedirectView("/admin?err=invalid");
        }
        db.execute("UPDATE chat_messages SET status = ? WHERE id = ?", status, id);
        return new RedirectView("/admin#inbox-chat");
    }

    @PostMapping("/admin/submissions/chat/{id}/delete")
    public Object chatDelete(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        db.execute("DELETE FROM chat_messages WHERE id = ?", id);
        return new RedirectView("/admin#inbox-chat");
    }

    @PostMapping("/admin/orders/{id}/deliver")
    public Object deliver(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> order = db.queryOne("SELECT * FROM orders WHERE id = ?", id);
        if (order == null) {
            return new RedirectView("/admin?err=invalid");
        }
        String st = normalizeStatus(order);
        if (!"advance_paid".equals(st)) {
            return new RedirectView("/admin?err=notready");
        }
        if (balanceDue(order) <= 0) {
            return new RedirectView("/admin?err=nobalance");
        }
        if (order.get("delivered_at") != null) {
            return new RedirectView("/admin#inbox-orders");
        }
        db.execute("UPDATE orders SET delivered_at = NOW() WHERE id = ?", id);
        return new RedirectView("/admin?flash=delivered#inbox-orders");
    }

    @PostMapping("/admin/orders/{id}/mark-advance-paid")
    public Object markAdvance(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> order = db.queryOne("SELECT * FROM orders WHERE id = ?", id);
        if (order == null || !"pending".equals(str(order.get("status")))) {
            return new RedirectView("/admin?err=notready#inbox-orders");
        }
        db.execute(
                """
                UPDATE orders
                SET status = 'advance_paid', paid_at = NOW(),
                    advance_paid_inr = COALESCE(advance_paid_inr, amount_inr)
                WHERE id = ?
                """,
                id
        );
        return new RedirectView("/admin?flash=advancepaid#inbox-orders");
    }

    @PostMapping("/admin/orders/{id}/delete")
    public Object deleteOrder(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        db.execute("DELETE FROM orders WHERE id = ? AND status = 'pending'", id);
        return new RedirectView("/admin?flash=orderdeleted#inbox-orders");
    }

    @PostMapping("/admin/users/{id}/role")
    public Object setRole(HttpServletRequest req, HttpSession session, @PathVariable long id, @RequestParam String role) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        if (!ROLES.contains(role)) {
            return new RedirectView("/admin?err=invalid");
        }
        Object self = session.getAttribute("user_id");
        if (self != null && AuthSupport.toLong(self) == id && "user".equals(role)) {
            return new RedirectView("/admin?err=selfdemote");
        }
        db.execute("UPDATE users SET role = ?, updated_at = NOW() WHERE id = ?", role, id);
        return new RedirectView("/admin?flash=roleupdated#users");
    }

    @GetMapping("/admin/connect")
    public Object connect(@RequestParam(required = false) String secret, HttpSession session) {
        String expected = settings.adminBootstrapSecret();
        if (expected == null || secret == null || !expected.equals(secret)) {
            return ResponseEntity.status(404).body("Page not found");
        }
        Map<String, Object> row = db.queryOne("SELECT id FROM users WHERE role='admin' ORDER BY id LIMIT 1");
        if (row == null) {
            Map<String, Object> first = db.queryOne("SELECT id FROM users ORDER BY id LIMIT 1");
            if (first == null) {
                return new RedirectView("/signup");
            }
            db.execute("UPDATE users SET role='admin', updated_at=NOW() WHERE id=?", first.get("id"));
            row = first;
        }
        session.setAttribute("user_id", row.get("id"));
        Map<String, Object> u = db.queryOne(
                "SELECT id, provider, name, email, picture, role FROM users WHERE id=?",
                row.get("id")
        );
        session.setAttribute("user", u);
        return new RedirectView("/admin");
    }

    private List<Map<String, Object>> enrichOrders(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> o = new LinkedHashMap<>(row);
            String st = normalizeStatus(row);
            int due = balanceDue(row);
            boolean delivered = row.get("delivered_at") != null;
            o.put("statusNorm", st);
            o.put("balanceDue", due);
            o.put("delivered", delivered);
            o.put("canDeliver", "advance_paid".equals(st) && !delivered && due > 0);
            o.put("showBalanceLink", "advance_paid".equals(st) && delivered && due > 0);
            o.put("isPending", "pending".equals(st));
            o.put("isPaidLike", "advance_paid".equals(st) || "completed".equals(st));
            o.put("isCompleted", "completed".equals(st));
            o.put("pillClass", pill(st, delivered, due));
            o.put("statusLabel", label(st, delivered, due));
            o.put("whenLabel", fmt(row.get("created_at")));
            out.add(o);
        }
        return out;
    }

    private static String pill(String st, boolean delivered, int due) {
        if ("completed".equals(st)) {
            return "completed";
        }
        if ("advance_paid".equals(st) && delivered && due > 0) {
            return "balance-due";
        }
        if ("advance_paid".equals(st)) {
            return "paid";
        }
        return "pending";
    }

    private static String label(String st, boolean delivered, int due) {
        if ("completed".equals(st)) {
            return "Fully paid";
        }
        if ("advance_paid".equals(st) && delivered && due > 0) {
            return "Balance due";
        }
        if ("advance_paid".equals(st)) {
            return "Advance paid";
        }
        return "Pending";
    }

    private static String normalizeStatus(Map<String, Object> order) {
        String st = str(order.get("status"));
        if ("paid".equals(st)) {
            return "advance_paid";
        }
        return st;
    }

    private static int balanceDue(Map<String, Object> order) {
        int total = toInt(order.get("total_inr"));
        int adv = toInt(order.get("advance_paid_inr"));
        if (adv <= 0) {
            adv = toInt(order.get("amount_inr"));
        }
        int bal = toInt(order.get("balance_paid_inr"));
        return Math.max(0, total - adv - bal);
    }

    private Map<String, Integer> series(String table) {
        Map<String, Integer> out = new HashMap<>();
        Timestamp since = Timestamp.from(Instant.now().minus(14, ChronoUnit.DAYS));
        try {
            List<Map<String, Object>> rows = db.query(
                    "SELECT created_at FROM " + table + " WHERE created_at >= ?",
                    since
            );
            ZoneId zone = ZoneId.systemDefault();
            for (Map<String, Object> r : rows) {
                Object v = r.get("created_at");
                if (v == null) {
                    continue;
                }
                LocalDate d;
                if (v instanceof Timestamp ts) {
                    d = ts.toInstant().atZone(zone).toLocalDate();
                } else if (v instanceof java.time.LocalDateTime ldt) {
                    d = ldt.toLocalDate();
                } else if (v instanceof java.time.OffsetDateTime odt) {
                    d = odt.toLocalDate();
                } else {
                    String s = String.valueOf(v);
                    d = LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
                }
                String key = d.toString();
                out.put(key, out.getOrDefault(key, 0) + 1);
            }
        } catch (Exception ignored) {
            // empty series if table is missing
        }
        return out;
    }

    private int countBalanceDue() {
        try {
            List<Map<String, Object>> rows = db.query(
                    "SELECT total_inr, amount_inr, advance_paid_inr, balance_paid_inr, status FROM orders"
            );
            int n = 0;
            for (Map<String, Object> r : rows) {
                String st = normalizeStatus(r);
                if (("advance_paid".equals(st) || "completed".equals(st)) && balanceDue(r) > 0) {
                    n++;
                }
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    private int count(String sql, Object... args) {
        Map<String, Object> row = queryOneSafe(sql, args);
        if (row == null) {
            return 0;
        }
        Object c = row.get("c");
        if (c == null) {
            c = row.get("C");
        }
        return toInt(c);
    }

    private Map<String, Object> queryOneSafe(String sql, Object... args) {
        try {
            return db.queryOne(sql, args);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> querySafe(String sql, Object... args) {
        try {
            return db.query(sql, args);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String fmt(Object value) {
        if (value == null) {
            return "—";
        }
        return String.valueOf(value).replace('T', ' ');
    }

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
