/**
 * =============================================================================
 * BACKEND — wrap-service-topic-links
 * File: scripts/wrap-service-topic-links.js
 * Wrap service links in HTML for topics
 * =============================================================================
 */

const fs = require("fs");
const path = require("path");
const root = path.join(__dirname, "..");
const file = path.join(root, "services.html");

const slugs = [
  "web-consulting",
  "app-consulting",
  "tech-consulting",
  "cyber-security-consulting",
  "android-app-development",
  "native-app-development",
  "ios-app-development",
  "flutter-app-development",
  "hybrid-app-development",
  "app-maintenance",
  "social-app-development",
  "car-rental-app-development",
  "grocery-app-development",
  "website-design",
  "website-redesign",
  "website-maintenance",
  "ecommerce-website",
  "ngo-website",
  "car-rental-website",
  "enterprise-software-development",
  "school-management-software",
  "school-erp-development",
  "nidhi-software",
  "real-estate-software",
  "nbfc-software",
  "nbfc-software-development",
  "cold-storage-software",
  "billing-software",
  "crm-software",
  "invoicing-software",
  "erp-software",
  "mlm-software",
  "ecommerce-web-development",
  "ecommerce-app-development",
  "search-engine-optimization",
  "geo-generative-engine-optimization",
  "local-geographic-seo",
  "technical-seo",
  "content-keyword-strategy",
  "ai-search-visibility",
  "performance-testing",
  "security-testing",
  "accessibility-testing",
  "usability-testing",
  "automation-testing",
  "mobile-app-testing",
  "functional-testing",
];

let html = fs.readFileSync(file, "utf8");
let i = 0;
html = html.replace(/<div class="service-topic">([\s\S]*?)<\/div>/g, (_m, inner) => {
  const slug = slugs[i++];
  if (!slug) throw new Error("Too many topic blocks for slug list");
  return `<a class="service-topic service-topic-link" href="service-topic.html?topic=${slug}">${inner.trim()}</a>`;
});
if (i !== slugs.length) {
  throw new Error(`Slug count mismatch: used ${i}, expected ${slugs.length}`);
}
fs.writeFileSync(file, html, "utf8");
console.log("Wrapped", i, "topic blocks in services.html");
