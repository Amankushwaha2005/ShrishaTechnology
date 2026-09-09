/**
 * =============================================================================
 * BACKEND — site config
 * File: src/config/site.js
 * Brand name, nav items, company contact info
 * =============================================================================
 */

const brand = "Shrisha Technology";

const company = {
  name: "Shrisha Technology",
  tagline: "Websites · Apps · SEO & GEO · Student Projects",
  email: "shrishatechnology2026@gmail.com",
  phone: "+91 79920 20591",
};

const navItems = [
  { href: "/", label: "Home", key: "home" },
  { href: "/pricing", label: "Pricing", key: "pricing" },
  { href: "/services", label: "Services", key: "services" },
  { href: "/portfolio", label: "Portfolio", key: "portfolio" },
  { href: "/about", label: "About", key: "about" },
  { href: "/contact", label: "Contact", key: "contact" },
];

module.exports = { brand, navItems, company };

