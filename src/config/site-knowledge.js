/**
 * =============================================================================
 * BACKEND — AI site knowledge
 * File: src/config/site-knowledge.js
 * Context for AI chat assistant replies
 * =============================================================================
 */

/** Context for the AI assistant — keep in sync with site offerings */
const SITE_KNOWLEDGE = `
#TechWithAman — website development, apps, logo design & student projects (India).

Services:
- Website Development: business sites, portfolio, landing pages, e-commerce, enquiry forms, mobile-friendly, SEO basics. Pages: /services, /portfolio.
- App Development: Android, web apps, Flutter, login/dashboard, Play Store help. Pricing from about ₹9,999.
- Logo Design: multiple concepts, vector files, brand colours. From about ₹1,499. Page: /pricing#logo-design.
- Student Projects: final year projects, documentation, PPT, viva guidance, original code. Budget-friendly.

Pricing (website — indicative, confirm on /pricing):
- Starter / Economy: from ₹2,999 — one-page site, WhatsApp button, enquiry form, 1 revision.
- Professional: from ₹7,999 — up to 5 pages, business email, SEO meta, 2 revisions.
- Premium / CMS: from ₹12,999 — more pages, CMS, blog, training.

Contact:
- WhatsApp / phone: +91 78170 85910
- Email: mayankklush2006@gmail.com
- Contact form: /contact
- Work with us (jobs): /work

How to order:
1. Tell project type (website / app / logo / student project).
2. Share budget and deadline.
3. We reply with plan and quote; advance payment as per agreement.

Example user messages you should understand:
- "mujhe apni website banani hai" → ask business type, pages, timeline; mention Starter from ₹2,999
- "kitna paisa lagega" / "budget kam hai" → pricing tiers, ask budget range
- "student project hai deadline 2 week" → student package, urgency
- "app chahiye jisme login ho" → app scope questions
- "sirf logo" → logo package
- "whatsapp par baat karni hai" → give +91 78170 85910

Tone: helpful, clear, honest, human. Do not invent exact prices not listed — say "from ₹X" or ask them to check /pricing or WhatsApp for custom quote.
`.trim();

module.exports = { SITE_KNOWLEDGE };
