/**
 * =============================================================================
 * BACKEND — Express app factory
 * File: src/app.js
 * Middleware, session, routes, static files, 404
 * =============================================================================
 */

const path = require("path");
const express = require("express");
const session = require("express-session");

const { queryOne } = require("./services/db");
const { isAdminUser } = require("./config/admin");
const { syncAdminRole } = require("./services/user-role.service");

const pagesRoutes = require("./routes/pages.routes");
const authRoutes = require("./routes/auth.routes");
const formsRoutes = require("./routes/forms.routes");
const aiRoutes = require("./routes/ai.routes");
const paymentsRoutes = require("./routes/payments.routes");
const paymentsController = require("./controllers/payments.controller");
const { mountAdminRoutes } = require("./routes/admin.routes");
const { getRecaptchaSiteKey } = require("./config/recaptcha");
const {
  isPaymentEnabled,
  getAdvancePercent,
  getRazorpayKeyId,
  isLiveKeyOnLocalhost,
} = require("./config/payments");

function createApp() {
  const app = express();
  const isProduction = process.env.NODE_ENV === "production";

  if (isProduction) {
    app.set("trust proxy", 1);
  }

  const projectRoot = path.join(__dirname, "..");

  app.set("view engine", "ejs");
  app.set("views", path.join(projectRoot, "views"));

  function sendHealth(req, res) {
    res.set("Cache-Control", "no-store, no-cache, must-revalidate");
    res.type("html").send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Health</title>
  <style>
    body { font-family: system-ui, sans-serif; padding: 2rem; background: #0c1430; color: #e8ecff; }
    h1 { color: #5af; font-size: 1.75rem; }
    p { opacity: 0.9; }
    .ok { color: #6d6; font-weight: 700; }
  </style>
</head>
<body>
  <h1><span class="ok">OK</span> — server is running</h1>
  <p>Shrisha Technology · Express · <code>/health</code></p>
  <p>Time (server): ${new Date().toISOString()}</p>
  <p><a href="/" style="color:#8af">Open home</a></p>
</body>
</html>`);
  }
  app.get("/health", sendHealth);
  app.get("/health/", sendHealth);

  app.get("/api/site-info", (req, res) => {
    res.json({
      site: "shrisha-technology",
      version: "payments-v2",
      paymentEnabled: isPaymentEnabled(),
      hasRazorpayKey: !!getRazorpayKeyId(),
      advancePercent: getAdvancePercent(),
      baseUrl: process.env.BASE_URL || null,
      googleLoginEnabled: !!(
        process.env.GOOGLE_CLIENT_ID?.trim() && process.env.GOOGLE_CLIENT_SECRET?.trim()
      ),
      githubLoginEnabled: !!(
        process.env.GITHUB_CLIENT_ID?.trim() && process.env.GITHUB_CLIENT_SECRET?.trim()
      ),
      microsoftLoginEnabled: !!(
        process.env.MICROSOFT_CLIENT_ID?.trim() && process.env.MICROSOFT_CLIENT_SECRET?.trim()
      ),
    });
  });

  app.use(
    session({
      name: "shrisha-technology.sid",
      secret: process.env.SESSION_SECRET || "dev-session-secret-change-me",
      resave: false,
      saveUninitialized: false,
      cookie: {
        httpOnly: true,
        sameSite: "lax",
        secure: isProduction,
      },
    }),
  );

  // Razorpay webhook needs raw body for signature verification (before JSON parser).
  app.post(
    "/api/payments/webhook",
    express.raw({ type: "application/json" }),
    paymentsController.razorpayWebhook,
  );

  // Parse form submissions (x-www-form-urlencoded) and JSON (if you later add APIs).
  app.use(express.urlencoded({ extended: false }));
  app.use(express.json());

  app.use((req, res, next) => {
    res.locals.googleLoginEnabled = !!(
      process.env.GOOGLE_CLIENT_ID?.trim() && process.env.GOOGLE_CLIENT_SECRET?.trim()
    );
    res.locals.githubLoginEnabled = !!(
      process.env.GITHUB_CLIENT_ID?.trim() && process.env.GITHUB_CLIENT_SECRET?.trim()
    );
    res.locals.microsoftLoginEnabled = !!(
      process.env.MICROSOFT_CLIENT_ID?.trim() && process.env.MICROSOFT_CLIENT_SECRET?.trim()
    );
    res.locals.aiChatEnabled = true;
    res.locals.aiOpenAiConfigured = !!process.env.OPENAI_API_KEY?.trim();
    res.locals.paymentEnabled = isPaymentEnabled();
    res.locals.paymentAdvancePercent = getAdvancePercent();
    res.locals.razorpayKeyId = getRazorpayKeyId();
    res.locals.paymentLiveOnLocalhost = isLiveKeyOnLocalhost();
    res.locals.recaptchaSiteKey = getRecaptchaSiteKey();
    next();
  });

  app.use(async (req, res, next) => {
    if (!req.session?.userId) {
      res.locals.authUser = null;
      res.locals.isAdmin = false;
      return next();
    }

    try {
      const user = await queryOne(
        `SELECT id, provider, name, email, picture, role FROM users WHERE id = $1`,
        [req.session.userId],
      );

      const synced = user ? await syncAdminRole(user) : null;
      res.locals.authUser = synced || user || null;
      res.locals.isAdmin = isAdminUser(synced || user);
    } catch (err) {
      console.error("[auth] Failed to load user session; clearing cookie.", err.message);
      res.locals.authUser = null;
      res.locals.isAdmin = false;
      return req.session.destroy(() => next());
    }
    next();
  });

  mountAdminRoutes(app);
  app.use("/", pagesRoutes);
  app.use("/", authRoutes);
  app.use("/", formsRoutes);
  app.use("/", paymentsRoutes);
  app.use("/", aiRoutes);

  // Static files after routes so /admin and other app paths are never shadowed by disk.
  app.use(express.static(projectRoot));

  // Basic 404
  app.use((req, res) => {
    res.status(404).send("Page not found");
  });

  app.use((err, req, res, next) => {
    console.error(err);
    res.status(500).send("Server error — check the terminal where npm start is running.");
  });

  return app;
}

module.exports = { createApp };

