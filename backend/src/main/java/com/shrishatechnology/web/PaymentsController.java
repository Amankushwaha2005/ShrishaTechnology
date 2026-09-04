package com.shrishatechnology.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishatechnology.config.AppSettings;
import com.shrishatechnology.config.WebConfig;
import com.shrishatechnology.config.WebConfig.AuthSupport;
import com.shrishatechnology.db.Db;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentsController {

    private final AppSettings settings;
    private final Db db;
    private final TemplateRenderer templates;
    private final AuthSupport auth;
    private final ObjectMapper mapper;
    private final CampaignStore campaigns;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
    private final SecureRandom random = new SecureRandom();

    public PaymentsController(
            AppSettings settings,
            Db db,
            TemplateRenderer templates,
            AuthSupport auth,
            ObjectMapper mapper,
            CampaignStore campaigns
    ) {
        this.settings = settings;
        this.db = db;
        this.templates = templates;
        this.auth = auth;
        this.mapper = mapper;
        this.campaigns = campaigns;
    }

    @GetMapping("/order")
    public ResponseEntity<String> showOrder(
            HttpServletRequest req,
            @RequestParam(defaultValue = "0") int total,
            @RequestParam(defaultValue = "0") int amount,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String plan
    ) {
        int amountInr = amount > 0 ? amount : 0;
        if (total > 0 && amountInr == 0) {
            amountInr = computeAdvance(total);
        }
        Map<String, Object> ctx = paymentPage(req, Map.of(
                "key", "order",
                "title", "Place Order & Pay | Shrisha Technology"
        ));
        ctx.put("orderPrefill", Map.of(
                "service", service == null ? "" : service,
                "plan", plan == null ? "" : plan,
                "total_inr", total,
                "amount_inr", amountInr
        ));
        return templates.render("pages/order.html", ctx);
    }

    @GetMapping("/order/success")
    public ResponseEntity<String> success(HttpServletRequest req, @RequestParam(name = "id", required = false) String publicId) {
        Map<String, Object> order = lookup(publicId);
        String status = order == null ? "" : str(order.get("status"));
        boolean advanceOk = order != null && ("paid".equals(status) || "advance_paid".equals(status) || "completed".equals(status));
        int balanceDue = 0;
        if (order != null) {
            int paid = num(order.get("advance_paid_inr"), num(order.get("amount_inr"), 0));
            balanceDue = Math.max(0, num(order.get("total_inr"), 0) - paid);
        }
        Map<String, Object> receipt = null;
        if (order != null && ("advance_paid".equals(status) || "completed".equals(status))) {
            receipt = Map.of(
                    "paymentDate", formatDate(order.get("paid_at") != null ? order.get("paid_at") : order.get("created_at")),
                    "gateway", "Razorpay",
                    "paymentId", order.get("razorpay_payment_id") == null ? "—" : order.get("razorpay_payment_id")
            );
        }
        Map<String, Object> ctx = paymentPage(req, Map.of(
                "key", "order-success",
                "title", "Order Confirmed | Shrisha Technology"
        ));
        ctx.put("order", order);
        ctx.put("receipt", receipt);
        ctx.put("balanceDue", balanceDue);
        ctx.put("advanceOk", advanceOk);
        return templates.render("pages/order-success.html", ctx);
    }

    @GetMapping("/order/receipt")
    public ResponseEntity<String> receipt(
            @RequestParam(name = "id", required = false) String publicId,
            @RequestParam(defaultValue = "advance") String phase
    ) {
        Map<String, Object> order = lookup(publicId);
        if (order == null) {
            return ResponseEntity.status(404).body("Receipt not found. Complete payment first or check your order ID.");
        }
        String status = str(order.get("status"));
        boolean isBalance = "balance".equals(phase);
        if (isBalance && !"completed".equals(status)) {
            return ResponseEntity.status(404).body("Balance receipt not available yet.");
        }
        if (!isBalance && "pending".equals(status)) {
            return ResponseEntity.status(404).body("Receipt not found. Complete payment first or check your order ID.");
        }
        int amount = isBalance
                ? num(order.get("balance_paid_inr"), 0)
                : num(order.get("advance_paid_inr"), num(order.get("amount_inr"), 0));
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("phase", isBalance ? "balance" : "advance");
        receipt.put("receiptTitle", isBalance ? "Balance Payment Receipt" : "Advance Payment Receipt");
        receipt.put("payeeName", order.get("name") == null ? "—" : order.get("name"));
        receipt.put("status", "PAID");
        receipt.put("paymentDate", formatDate(first(order.get("paid_at"), order.get("balance_paid_at"), order.get("created_at"))));
        receipt.put("transactionNo", first(order.get("razorpay_payment_id"), order.get("razorpay_balance_payment_id"), "—"));
        receipt.put("gateway", "Razorpay");
        receipt.put("paymentId", isBalance ? order.get("razorpay_balance_payment_id") : order.get("razorpay_payment_id"));
        receipt.put("amountFormatted", "₹" + amount);
        receipt.put("orderId", order.get("public_id"));
        receipt.put("service", order.get("service") == null ? "—" : order.get("service"));
        receipt.put("plan", order.get("plan") == null ? "—" : order.get("plan"));
        receipt.put("totalFormatted", "₹" + num(order.get("total_inr"), 0));
        receipt.put("email", order.get("email") == null ? "—" : order.get("email"));
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("page", Map.of("title", "Receipt " + order.get("public_id") + " | " + Site.BRAND));
        ctx.put("receipt", receipt);
        ctx.put("company", Site.COMPANY);
        ctx.put("order", order);
        ctx.put("brand", Site.BRAND);
        ctx.put("year", java.time.Year.now().getValue());
        return templates.render("pages/order-receipt.html", ctx);
    }

    @PostMapping("/api/payments/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody(required = false) Map<String, Object> payload) {
        try {
            if (payload == null) {
                payload = Map.of();
            }
            String name = str(payload.get("name")).trim();
            String email = str(payload.get("email")).trim().toLowerCase();
            String phone = emptyToNull(str(payload.get("phone")).trim());
            String service = str(payload.get("service")).trim();
            String plan = str(payload.get("plan")).trim();
            String notes = emptyToNull(str(payload.get("notes")).trim());
            int totalInr = (int) Double.parseDouble(str(payload.getOrDefault("total_inr", "0")).isBlank() ? "0" : str(payload.get("total_inr")));
            if (name.isEmpty() || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Name and email are required."));
            }
            if (service.isEmpty() || plan.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Service and plan are required."));
            }
            if (totalInr < 1) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Total amount is required."));
            }
            int advancePercent = settings.paymentAdvancePercent();
            int amountInr = computeAdvance(totalInr);
            String publicId = publicId();
            db.execute(
                    """
                    INSERT INTO orders (public_id, name, email, phone, service, plan, notes, total_inr, amount_inr, advance_percent, status)
                    VALUES (?,?,?,?,?,?,?,?,?,?,'pending')
                    """,
                    publicId, name, email, phone, service, plan, notes, totalInr, amountInr, advancePercent
            );
            Map<String, Object> order = db.queryOne("SELECT * FROM orders WHERE public_id=?", publicId);
            if (!settings.paymentEnabled()) {
                return ResponseEntity.status(503).body(Map.of(
                        "ok", false,
                        "error", "Online payment is not configured yet.",
                        "publicId", publicId,
                        "amountInr", amountInr
                ));
            }
            String rzpId = rzpCreateOrder(amountInr * 100, publicId, Map.of("public_id", publicId));
            db.execute("UPDATE orders SET razorpay_order_id=? WHERE id=?", rzpId, order.get("id"));
            Map<String, Object> out = new HashMap<>();
            out.put("ok", true);
            out.put("publicId", publicId);
            out.put("razorpayOrderId", rzpId);
            out.put("amountPaise", amountInr * 100);
            out.put("amountInr", amountInr);
            out.put("keyId", settings.razorpayKeyId());
            out.put("customer", Map.of("name", name, "email", email, "contact", phone == null ? "" : phone));
            out.put("description", service + " — " + plan + " (advance)");
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "ok", false,
                    "error", publicDbError(e, "Could not create order.")
            ));
        }
    }

    @PostMapping("/api/payments/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        String orderId = str(body.get("razorpay_order_id"));
        String paymentId = str(body.get("razorpay_payment_id"));
        String signature = str(body.get("razorpay_signature"));
        if (orderId.isBlank() || paymentId.isBlank() || signature.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing verification fields."));
        }
        if (!rzpVerify(orderId, paymentId, signature)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid signature."));
        }
        Map<String, Object> order = db.queryOne("SELECT * FROM orders WHERE razorpay_order_id=?", orderId);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Order not found."));
        }
        db.execute(
                """
                UPDATE orders
                SET status='advance_paid', razorpay_payment_id=?, paid_at=NOW(), advance_paid_inr=amount_inr
                WHERE id=?
                """,
                paymentId, order.get("id")
        );
        return ResponseEntity.ok(Map.of("ok", true, "redirectUrl", "/order/success?id=" + order.get("public_id")));
    }

    @RequestMapping(value = "/api/payments/status", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> status(
            @RequestParam(required = false) String publicId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        if ((publicId == null || publicId.isBlank()) && body != null) {
            publicId = str(body.get("publicId")).trim();
        }
        if (publicId == null || publicId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "publicId is required."));
        }
        Map<String, Object> order = db.queryOne("SELECT status FROM orders WHERE public_id=?", publicId);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Order not found."));
        }
        String st = str(order.get("status"));
        if ("advance_paid".equals(st) || "completed".equals(st) || "paid".equals(st)) {
            return ResponseEntity.ok(Map.of("ok", true, "status", "paid", "redirectUrl", "/order/success?id=" + publicId));
        }
        return ResponseEntity.ok(Map.of("ok", true, "status", st));
    }

    @PostMapping("/api/payments/resume-advance")
    public ResponseEntity<Map<String, Object>> resume(@RequestBody(required = false) Map<String, Object> body) {
        try {
            String publicId = body == null ? "" : str(body.get("publicId")).trim();
            if (publicId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Order ID is required."));
            }
            Map<String, Object> order = lookup(publicId);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Order not found."));
            }
            if (!settings.paymentEnabled()) {
                return ResponseEntity.status(503).body(Map.of("ok", false, "error", "Online payment is not available."));
            }
            String st = str(order.get("status"));
            if ("advance_paid".equals(st) || "completed".equals(st)) {
                return ResponseEntity.ok(Map.of("ok", true, "redirectUrl", "/order/success?id=" + publicId));
            }
            String rzpId = str(order.get("razorpay_order_id"));
            if (rzpId.isBlank()) {
                rzpId = rzpCreateOrder(num(order.get("amount_inr"), 0) * 100, publicId, Map.of("public_id", publicId));
                db.execute("UPDATE orders SET razorpay_order_id=? WHERE id=?", rzpId, order.get("id"));
            }
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "publicId", publicId,
                    "razorpayOrderId", rzpId,
                    "amountPaise", num(order.get("amount_inr"), 0) * 100,
                    "amountInr", num(order.get("amount_inr"), 0),
                    "keyId", settings.razorpayKeyId(),
                    "description", order.get("service") + " — " + order.get("plan") + " (advance)"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", publicDbError(e, "Could not resume payment.")));
        }
    }

    @GetMapping("/order/pay-advance")
    public ResponseEntity<String> payAdvance(HttpServletRequest req, @RequestParam(name = "id", required = false) String publicId) {
        Map<String, Object> order = lookup(publicId);
        String st = order == null ? "" : str(order.get("status"));
        Map<String, Object> ctx = paymentPage(req, Map.of("key", "order-pay-advance", "title", "Pay Advance | Shrisha Technology"));
        ctx.put("order", order);
        ctx.put("canPay", "pending".equals(st));
        ctx.put("alreadyPaid", "advance_paid".equals(st) || "completed".equals(st) || "paid".equals(st));
        ctx.put("advanceDue", order == null ? 0 : num(order.get("amount_inr"), 0));
        return templates.render("pages/order-pay-advance.html", ctx);
    }

    @GetMapping("/order/pay-balance")
    public ResponseEntity<String> payBalance(HttpServletRequest req, @RequestParam(name = "id", required = false) String publicId) {
        Map<String, Object> order = lookup(publicId);
        String st = order == null ? "" : str(order.get("status"));
        boolean alreadyComplete = "completed".equals(st);
        int due = order == null ? 0 : balanceDue(order);
        boolean canPay = order != null
                && ("advance_paid".equals(st) || "completed".equals(st))
                && order.get("delivered_at") != null
                && due > 0
                && !alreadyComplete;
        Map<String, Object> ctx = paymentPage(req, Map.of("key", "order-pay-balance", "title", "Pay Balance | Shrisha Technology"));
        ctx.put("order", order);
        ctx.put("alreadyComplete", alreadyComplete);
        ctx.put("canPay", canPay);
        ctx.put("balanceDue", due);
        ctx.put("advancePaid", order == null ? 0 : num(order.get("advance_paid_inr"), num(order.get("amount_inr"), 0)));
        ctx.put("orderStatusLabel", "advance_paid".equals(st) || "completed".equals(st) ? "Advance paid" : (st.isBlank() ? "Pending" : st));
        return templates.render("pages/order-pay-balance.html", ctx);
    }

    @PostMapping("/api/payments/create-balance-order")
    public ResponseEntity<Map<String, Object>> createBalance(@RequestBody(required = false) Map<String, Object> body) {
        try {
            String publicId = body == null ? "" : str(body.get("publicId")).trim();
            if (publicId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Order ID is required."));
            }
            Map<String, Object> order = lookup(publicId);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Order not found."));
            }
            if (!settings.paymentEnabled()) {
                return ResponseEntity.status(503).body(Map.of("ok", false, "error", "Online payment is not available."));
            }
            int due = balanceDue(order);
            if (due <= 0) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "No remaining balance due."));
            }
            String rzpId = str(order.get("razorpay_balance_order_id"));
            if (rzpId.isBlank()) {
                rzpId = rzpCreateOrder(due * 100, publicId + "-bal", Map.of("public_id", publicId, "phase", "balance"));
                db.execute("UPDATE orders SET razorpay_balance_order_id=? WHERE id=?", rzpId, order.get("id"));
            }
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "publicId", publicId,
                    "razorpayOrderId", rzpId,
                    "amountPaise", due * 100,
                    "amountInr", due,
                    "keyId", settings.razorpayKeyId(),
                    "description", order.get("service") + " — " + order.get("plan") + " (balance)"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", publicDbError(e, "Could not start balance payment.")));
        }
    }

    @PostMapping("/api/payments/verify-balance")
    public ResponseEntity<Map<String, Object>> verifyBalance(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        String orderId = str(body.get("razorpay_order_id"));
        String paymentId = str(body.get("razorpay_payment_id"));
        String signature = str(body.get("razorpay_signature"));
        if (orderId.isBlank() || paymentId.isBlank() || signature.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing verification fields."));
        }
        if (!rzpVerify(orderId, paymentId, signature)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid signature."));
        }
        Map<String, Object> order = db.queryOne("SELECT * FROM orders WHERE razorpay_balance_order_id=?", orderId);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Order not found."));
        }
        int due = balanceDue(order);
        db.execute(
                """
                UPDATE orders
                SET status='completed', razorpay_balance_payment_id=?, balance_paid_inr=?,
                    balance_paid_at=NOW(), completed_at=NOW()
                WHERE id=?
                """,
                paymentId, due, order.get("id")
        );
        return ResponseEntity.ok(Map.of("ok", true, "redirectUrl", "/order/balance-success?id=" + order.get("public_id")));
    }

    @RequestMapping(value = "/api/payments/balance-status", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> balanceStatus(
            @RequestParam(required = false) String publicId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        if ((publicId == null || publicId.isBlank()) && body != null) {
            publicId = str(body.get("publicId")).trim();
        }
        if (publicId == null || publicId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "publicId is required."));
        }
        Map<String, Object> order = db.queryOne("SELECT status FROM orders WHERE public_id=?", publicId);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Order not found."));
        }
        String st = str(order.get("status"));
        if ("completed".equals(st)) {
            return ResponseEntity.ok(Map.of("ok", true, "status", "paid", "redirectUrl", "/order/balance-success?id=" + publicId));
        }
        return ResponseEntity.ok(Map.of("ok", true, "status", st));
    }

    @GetMapping("/order/balance-success")
    public ResponseEntity<String> balanceSuccess(HttpServletRequest req, @RequestParam(name = "id", required = false) String publicId) {
        Map<String, Object> order = lookup(publicId);
        Map<String, Object> receipt = null;
        if (order != null && "completed".equals(str(order.get("status")))) {
            receipt = Map.of(
                    "paymentDate", formatDate(order.get("balance_paid_at")),
                    "gateway", "Razorpay",
                    "paymentId", order.get("razorpay_balance_payment_id") == null ? "—" : order.get("razorpay_balance_payment_id")
            );
        }
        Map<String, Object> ctx = paymentPage(req, Map.of("key", "order-balance-success", "title", "Payment Complete | Shrisha Technology"));
        ctx.put("order", order);
        ctx.put("receipt", receipt);
        return templates.render("pages/order-balance-success.html", ctx);
    }

    private Map<String, Object> paymentPage(HttpServletRequest req, Map<String, Object> page) {
        Map<String, Object> ctx = WebConfig.pageContext(settings, req, page, auth);
        campaigns.attachPublic(ctx);
        ctx.put("paymentEnabled", settings.paymentEnabled());
        ctx.put("razorpayKeyId", settings.razorpayKeyId());
        ctx.put("paymentLiveOnLocalhost", false);
        return ctx;
    }

    private Map<String, Object> lookup(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return null;
        }
        return db.queryOne("SELECT * FROM orders WHERE public_id = ?", publicId.trim());
    }

    private int computeAdvance(int total) {
        int t = Math.max(0, total);
        if (t <= 0) {
            return 0;
        }
        return Math.max(1, Math.round((t * settings.paymentAdvancePercent()) / 100f));
    }

    private int balanceDue(Map<String, Object> order) {
        int total = num(order.get("total_inr"), 0);
        int adv = num(order.get("advance_paid_inr"), num(order.get("amount_inr"), 0));
        int bal = num(order.get("balance_paid_inr"), 0);
        return Math.max(0, total - adv - bal);
    }

    private String publicId() {
        byte[] a = new byte[4];
        byte[] b = new byte[4];
        random.nextBytes(a);
        random.nextBytes(b);
        return HexFormat.of().formatHex(a) + "-" + HexFormat.of().formatHex(b);
    }

    private String rzpCreateOrder(int amountPaise, String receipt, Map<String, String> notes) throws Exception {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (settings.razorpayKeyId() + ":" + settings.razorpayKeySecret()).getBytes(StandardCharsets.UTF_8)
        );
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amountPaise);
        payload.put("currency", "INR");
        payload.put("receipt", receipt);
        payload.put("payment_capture", 1);
        payload.put("notes", notes);
        String json = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("Could not create Razorpay order.");
        }
        JsonNode data = mapper.readTree(resp.body());
        String id = data.path("id").asText("");
        if (id.isBlank()) {
            throw new IllegalStateException("Razorpay order id missing.");
        }
        return id;
    }

    private boolean rzpVerify(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(settings.razorpayKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(raw);
            return expected.equalsIgnoreCase(signature == null ? "" : signature);
        } catch (Exception e) {
            return false;
        }
    }

    private static String formatDate(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (value instanceof LocalDate ld) {
            return ld.toString();
        }
        String s = String.valueOf(value);
        return s.length() > 19 ? s.substring(0, 19) : s;
    }

    private static Object first(Object... vals) {
        for (Object v : vals) {
            if (v != null && !String.valueOf(v).isBlank()) {
                return v;
            }
        }
        return "—";
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

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String publicDbError(Exception e, String fallback) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("JDBC") || m.contains("Connection is not available") || m.contains("HikariPool")) {
            return "Database is not available. Start PostgreSQL or try again in a minute.";
        }
        return m.isBlank() ? fallback : m;
    }
}
