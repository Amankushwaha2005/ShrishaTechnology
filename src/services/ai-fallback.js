/**
 * =============================================================================
 * BACKEND — AI fallback service
 * File: src/services/ai-fallback.js
 * Rule-based replies when no API key
 * =============================================================================
 */

const { SITE_KNOWLEDGE } = require("../config/site-knowledge");

function normalize(text) {
  return String(text || "")
    .toLowerCase()
    .replace(/[₹]/g, " rs ")
    .replace(/[^\w\s\u0900-\u097F]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function recentUserContext(history) {
  return (history || [])
    .filter((h) => h && h.role === "user" && h.content)
    .slice(-3)
    .map((h) => normalize(h.content))
    .join(" ");
}

function scoreIntents(combined) {
  const intents = [];
  const add = (name, re, weight = 1) => {
    if (re.test(combined)) intents.push({ name, weight });
  };

  add("greeting", /\b(hi|hello|hey|namaste|hii|helo|good morning|good evening)\b/);
  add("thanks", /\b(thank|thanks|dhanyavad|shukriya)\b/);
  add("pricing", /\b(price|pricing|cost|rate|charges|kitna|kitne|paisa|paise|budget|afford|sasta|mehnga|package|plan)\b/);
  add("website_build", /\b(website|web site|site|banani|banwana|bana do|banwa|chahiye|portfolio|landing|dukan|shop online)\b/);
  add("ecommerce", /\b(ecommerce|e commerce|online shop|cart|payment gateway|flipkart|amazon type)\b/);
  add("app", /\b(app|android|ios|flutter|mobile app|play store|application)\b/);
  add("logo", /\b(logo|brand|branding|design logo)\b/);
  add("student", /\b(student|college|final year|viva|seminar|report|ppt|btech|be project|mca|bca)\b/);
  add("timeline", /\b(time|timeline|kitne din|kab tak|deadline|jaldi|fast|delivery|days|week)\b/);
  add("contact", /\b(contact|call|phone|number|whatsapp|wa|email|mail|baat|connect|reach)\b/);
  add("payment", /\b(payment|pay|advance|refund|emi|installment)\b/);
  add("portfolio", /\b(example|sample|portfolio|kaam|work|projects done|dikhao)\b/);
  add("compare", /\b(difference|compare|better|best|konsa|which plan|recommend|suggest)\b/);
  add("yes", /\b(haan|ha|yes|ok|okay|theek|thik|sure|bilkul)\b/);
  add("no", /\b(nahi|na|no|not now|baad me)\b/);

  intents.sort((a, b) => b.weight - a.weight);
  return intents;
}

function replyForIntent(primary, combined, message) {
  const m = normalize(message);

  switch (primary) {
    case "greeting":
      return "Namaste! Main #TechWithAman ka assistant hoon. Aap website, app, logo ya student project ke baare me Hindi/English dono me pooch sakte ho — main samajh kar jawab dunga.";
    case "thanks":
      return "Welcome! Aur koi sawal ho to poochiye — ya WhatsApp +91 78170 85910 par direct team se baat kar sakte ho.";
    case "pricing":
      if (/app/.test(combined)) {
        return "App development packages /pricing#app-development par — usually ₹9,999 se upar idea par depend. Short idea WhatsApp par bhejo.";
      }
      if (/logo/.test(combined)) {
        return "Logo design ~₹1,499 se shuru — concepts + vector files. /pricing#logo-design";
      }
      return "Website: Starter ~₹2,999 (1 page), Professional ~₹7,999 (5 pages), Premium/CMS ~₹12,999. Exact quote budget + pages batane par. /pricing ya WhatsApp +91 78170 85910.";
    case "website_build":
      return "Website ke liye 3 cheezein batayein: (1) business type — shop/coaching/portfolio? (2) kitne pages? (3) kab tak chahiye? Starter ₹2,999 se 1-page site possible hai. Details milte hi exact plan bata sakte hain.";
    case "ecommerce":
      return "Online shop / payment ke liye Professional ya Premium package better hota hai. Products count, payment method (UPI/Razorpay) aur timeline batayein — hum custom quote denge.";
    case "app":
      return "App chahiye to batayein: Android only ya Android+iOS? Login chahiye? Play Store publish? MVP idea likh kar WhatsApp +91 78170 85910 par bhejo — package suggest karenge.";
    case "logo":
      return "Logo ke liye brand name, colours pasand, aur kahan use hoga (shop/social) batayein. ~₹1,499 se concepts milte hain. 2–3 din me first draft possible (load par depend).";
    case "student":
      return "Student project: topic/subject, college guidelines, deadline, aur web/app/software type batayein. Hum code + report + PPT help karte hain. Budget friendly — WhatsApp par details bhejo.";
    case "timeline":
      if (/student/.test(combined)) {
        return "Student project timeline deadline ke hisaab se — jaldi ho to abhi date batayein. Viva date bhi likh dena.";
      }
      if (/app/.test(combined)) {
        return "Simple app MVP often 2–4 weeks; complex 1–2+ months. Features list bhejo for real timeline.";
      }
      return "Simple website: often 5–10 working days. 5+ pages / custom features: 2–4 weeks. Aapki deadline batao — hum honest timeline denge.";
    case "contact":
      return "Direct baat: WhatsApp/call **+91 78170 85910**, email **mayankklush2006@gmail.com**, ya site par **/contact** form. Usually kuch hours me reply.";
    case "payment":
      return "Generally advance to start, milestones for bigger projects. Refund policy footer/terms me — exact plan WhatsApp par discuss hota hai.";
    case "portfolio":
      return "Sample work **/portfolio** par dekho — business sites, apps, student projects. Apna similar example chahiye ho to WhatsApp par batao.";
    case "compare":
      return "Starter = 1 page, jaldi & budget. Professional = 5 pages, business email, zyada professional. Premium = CMS, blog, zyada control. Aapka goal batayein — recommend kar denge.";
    case "yes":
      return "Great! Agla step: project type + budget + deadline likh kar WhatsApp +91 78170 85910 par bhejo, ya /contact form bharo. Team follow-up karegi.";
    case "no":
      return "Koi baat nahi — jab ready ho tab message karna. /pricing aur /services browse kar sakte ho.";
    default:
      break;
  }

  if (/kaise|how|kese|process|steps|procedure/.test(m)) {
    return "Process: (1) Aap requirement bhejo (2) Hum quote + timeline (3) Advance & design (4) Build + revisions (5) Launch + support. WhatsApp ya /contact se start karo.";
  }

  if (/free|muft|without money/.test(m)) {
    return "Paid professional work hai — budget friendly plans available (website ₹2,999 se). Student projects ke liye budget discuss hota hai.";
  }

  return null;
}

function fallbackReply(message, history = []) {
  const m = normalize(message);
  const ctx = recentUserContext(history);
  const combined = `${m} ${ctx}`.trim();

  if (!m) {
    return "Namaste! Main #TechWithAman ka AI assistant hoon. Website, app, logo ya student project — jo bhi chahiye casual Hindi/English me likho.";
  }

  const intents = scoreIntents(combined);
  const primary = intents[0]?.name;

  const specific = replyForIntent(primary, combined, message);
  if (specific) return specific;

  if (intents.length >= 2) {
    const a = intents[0].name;
    const b = intents[1].name;
    if (a === "website_build" && b === "pricing") {
      return "Website + pricing: Starter ₹2,999 se, Professional ₹7,999. Pehle batayein kis type ki site — phir exact quote.";
    }
    if (a === "timeline" && (b === "website_build" || b === "app")) {
      return replyForIntent("timeline", combined, message);
    }
  }

  return `Main ${SITE_KNOWLEDGE.split("\n")[0]} ke baare me help karta hoon. Thoda clear likho — jaise "coaching website 5 page kitne me" ya "Android app cost" — ya WhatsApp +91 78170 85910 par baat karo.`;
}

module.exports = { fallbackReply, normalize, scoreIntents };
