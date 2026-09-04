/**
 * =============================================================================
 * BACKEND — pages routes
 * File: src/routes/pages.routes.js
 * GET /, /pricing, /contact, /login, etc.
 * =============================================================================
 */

const express = require("express");

const { renderPage } = require("../controllers/pages.controller");

const router = express.Router();

router.get("/", (req, res) => renderPage(req, res, "pages/index", { key: "home", title: "Shrisha Technology" }));
router.get("/pricing", (req, res) =>
  renderPage(req, res, "pages/pricing", { key: "pricing", title: "Pricing | Shrisha Technology" }),
);
router.get("/services", (req, res) =>
  renderPage(req, res, "pages/services", { key: "services", title: "Services | Shrisha Technology" }),
);
router.get("/portfolio", (req, res) =>
  renderPage(req, res, "pages/portfolio", { key: "portfolio", title: "Portfolio | Shrisha Technology" }),
);
router.get("/about", (req, res) =>
  renderPage(req, res, "pages/about", { key: "about", title: "About | Shrisha Technology" }),
);
router.get("/contact", (req, res) =>
  renderPage(req, res, "pages/contact", { key: "contact", title: "Contact | Shrisha Technology" }),
);
router.get("/login", (req, res) => {
  if (req.session?.userId) return res.redirect("/");
  const next = req.query.next;
  if (typeof next === "string" && next.startsWith("/") && !next.startsWith("//")) {
    req.session.afterLoginRedirect = next;
  } else {
    delete req.session.afterLoginRedirect;
  }
  return renderPage(req, res, "pages/login", { key: "login", title: "Login | Shrisha Technology" });
});
router.get("/signup", (req, res) => {
  if (req.session?.userId) return res.redirect("/");
  return renderPage(req, res, "pages/signup", { key: "signup", title: "Signup | Shrisha Technology" });
});
router.get("/work", (req, res) =>
  renderPage(req, res, "pages/work", { key: "work", title: "Work With Us | Shrisha Technology" }),
);

// Backwards-compat: if someone opens old .html links
router.get(
  [
    "/index.html",
    "/pricing.html",
    "/services.html",
    "/portfolio.html",
    "/about.html",
    "/contact.html",
    "/admin.html",
  ],
  (req, res) => {
    const map = {
      "/index.html": "/",
      "/pricing.html": "/pricing",
      "/services.html": "/services",
      "/portfolio.html": "/portfolio",
      "/about.html": "/about",
      "/contact.html": "/contact",
      "/admin.html": "/admin",
    };
    res.redirect(301, map[req.path] || "/");
  },
);

module.exports = router;

