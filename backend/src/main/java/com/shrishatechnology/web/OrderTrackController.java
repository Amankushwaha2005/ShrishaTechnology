package com.shrishatechnology.web;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shrishatechnology.db.Db;
import com.shrishatechnology.db.SchemaMigrator;

@RestController
public class OrderTrackController {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final Db db;
    private final SchemaMigrator schema;

    public OrderTrackController(Db db, SchemaMigrator schema) {
        this.db = db;
        this.schema = schema;
    }

    @GetMapping("/api/orders/track")
    public ResponseEntity<Map<String, Object>> track(@RequestParam(name = "id", required = false) String id) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("ok", false);
        if (!schema.isAvailable()) {
            err.put("error", "Tracking is temporarily unavailable. Try again in a minute.");
            return ResponseEntity.status(503).body(err);
        }
        String publicId = id == null ? "" : id.trim();
        if (publicId.isEmpty()) {
            err.put("error", "Enter your Order ID from the payment confirmation.");
            return ResponseEntity.badRequest().body(err);
        }
        Map<String, Object> order = db.queryOne(
                """
                SELECT public_id, name, service, plan, status, total_inr, amount_inr,
                       advance_paid_inr, balance_paid_inr, created_at, paid_at,
                       delivered_at, completed_at
                FROM orders WHERE public_id = ?
                """,
                publicId
        );
        if (order == null) {
            err.put("error", "No order found for this ID. Check the code on your receipt.");
            return ResponseEntity.status(404).body(err);
        }

        String st = str(order.get("status"));
        boolean advancePaid = "advance_paid".equals(st) || "completed".equals(st) || "paid".equals(st);
        boolean delivered = order.get("delivered_at") != null;
        boolean completed = "completed".equals(st);
        int advance = num(order.get("advance_paid_inr"), advancePaid ? num(order.get("amount_inr"), 0) : 0);
        int total = num(order.get("total_inr"), 0);
        int balancePaid = num(order.get("balance_paid_inr"), 0);
        int balanceDue = Math.max(0, total - advance - balancePaid);

        String phase;
        if (completed) {
            phase = "complete";
        } else if (delivered) {
            phase = "delivered";
        } else if (advancePaid) {
            phase = "build";
        } else {
            phase = "pending";
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step("placed", "Order placed", true, false, formatWhen(order.get("created_at"))));
        steps.add(step("advance", "Advance paid", advancePaid, "pending".equals(phase), formatWhen(order.get("paid_at"))));
        steps.add(step("build", "Project in progress", advancePaid, "build".equals(phase),
                "build".equals(phase) ? "Live — our team is working on your project" : ""));
        steps.add(step("delivered", "Ready / delivered", delivered, "delivered".equals(phase), formatWhen(order.get("delivered_at"))));
        steps.add(step("complete", "Fully paid", completed, false, formatWhen(order.get("completed_at"))));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("publicId", order.get("public_id"));
        out.put("client", firstName(str(order.get("name"))));
        out.put("service", order.get("service"));
        out.put("plan", order.get("plan"));
        out.put("status", st);
        out.put("phase", phase);
        out.put("live", advancePaid && !completed);
        out.put("totalInr", total);
        out.put("advancePaid", advance);
        out.put("balanceDue", balanceDue);
        out.put("steps", steps);
        if (!advancePaid) {
            out.put("payUrl", "/order/pay-advance?id=" + publicId);
        } else if (delivered && balanceDue > 0 && !completed) {
            out.put("payUrl", "/order/pay-balance?id=" + publicId);
        }
        return ResponseEntity.ok(out);
    }

    private static Map<String, Object> step(String key, String label, boolean done, boolean current, String detail) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("key", key);
        s.put("label", label);
        s.put("done", done);
        s.put("current", current);
        s.put("detail", detail == null ? "" : detail);
        return s;
    }

    private static String firstName(String name) {
        if (name.isBlank()) {
            return "Client";
        }
        return name.trim().split("\\s+")[0];
    }

    private static String formatWhen(Object value) {
        if (value == null) {
            return "";
        }
        try {
            if (value instanceof java.sql.Timestamp ts) {
                return ts.toInstant().atZone(IST).format(WHEN);
            }
            if (value instanceof OffsetDateTime odt) {
                return odt.atZoneSameInstant(IST).format(WHEN);
            }
            if (value instanceof Instant inst) {
                return inst.atZone(IST).format(WHEN);
            }
            if (value instanceof LocalDateTime ldt) {
                return ldt.atZone(IST).format(WHEN);
            }
            String s = String.valueOf(value);
            if (s.length() >= 16) {
                return s.substring(0, 16).replace('T', ' ');
            }
            return s;
        } catch (Exception e) {
            return "";
        }
    }

    private static int num(Object v, int fallback) {
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
