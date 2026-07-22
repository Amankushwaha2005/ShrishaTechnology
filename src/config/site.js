/**
 * =============================================================================
 * BACKEND — site config
 * File: src/config/site.js
 * Brand name, nav items, company contact info
 * =============================================================================
 */

const brand = "#TechWithAman";

const company = {
  name: "#TechWithAman",
  tagline: "Websites · Apps · Logo Design · Student Projects",
  email: "mayankklush2006@gmail.com",
  phone: "+91 78170 85910",
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

