/**
 * =============================================================================
 * CLIENT — chatbot.js
 * File: chatbot.js
 * Site chatbot form submit to API
 * =============================================================================
 */

(function () {
  const WA_PHONE = "917817085910";
  const WA_URL = `https://wa.me/${WA_PHONE}?text=`;
  const BRAND = "#TechWithAman";

  const QUICK = [
    { id: "pricing", label: "Pricing / packages" },
    { id: "services", label: "Services" },
    { id: "student", label: "Student project" },
    { id: "contact", label: "Contact & timeline" },
    { id: "whatsapp", label: "Talk on WhatsApp" },
  ];

  const REPLIES = {
    pricing:
      "We offer Starter, Professional & Premium website plans — and separate packages for apps & logo design. Open the Pricing page or tell us your budget on WhatsApp for a custom quote.",
    services:
      "We build websites, Android/web apps, logo & branding, and final-year student projects with reports & viva support. Browse Services on the site for full details.",
    student:
      "Student projects include code, documentation, PPT tips & viva guidance — delivered on your deadline. Share your topic on WhatsApp for a budget-friendly quote.",
    contact:
      "Share your name, project type & deadline — we usually reply within a few hours. Email: mayankklush2006@gmail.com | Call/WhatsApp: +91 78170 85910",
    whatsapp: null,
    default:
      "Thanks for your message! For a detailed reply, tap “Send on WhatsApp” below or ask about pricing, services, or student projects.",
  };

  const SAVED_HINT =
    "Message admin panel me save ho gaya. WhatsApp par lane ke liye neeche “Send on WhatsApp” dabao — WhatsApp me Send press karne ke baad aapke phone (+91 78170 85910) par dikhega.";

  let chatbotEl;
  let messagesEl;
  let quickEl;
  let formEl;
  let inputEl;
  let waLinkEl;
  let floatEl;
  let isOpen = false;
  let lastUserText = "";

  function esc(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function addMessage(text, who) {
    if (!messagesEl) return;
    const row = document.createElement("div");
    row.className = `twa-chatbot__msg twa-chatbot__msg--${who}`;
    row.innerHTML = `<p>${esc(text)}</p>`;
    messagesEl.appendChild(row);
    messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  function updateWaLink(text) {
    if (!waLinkEl) return;
    const t = String(text || "").trim();
    const body = t ? `Hi ${BRAND}, ${t}` : `Hi ${BRAND}, I need help with a project.`;
    waLinkEl.href = WA_URL + encodeURIComponent(body);
  }

  function saveMessage(text) {
    return fetch("/api/chatbot-message", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: text, page: window.location.href }),
    })
      .then((r) => r.json())
      .then((data) => !!data.ok)
      .catch(() => false);
  }

  function afterUserMessage(text) {
    lastUserText = text;
    updateWaLink(text);
    saveMessage(text).then((ok) => {
      if (ok) {
        addMessage(SAVED_HINT, "bot");
      } else {
        addMessage(
          "Server se connect nahi hua — site npm start se chalao (http://127.0.0.1:3000). Tabhi messages admin panel me save honge.",
          "bot",
        );
      }
    });
  }

  function renderQuick() {
    if (!quickEl) return;
    quickEl.innerHTML = QUICK.map(
      (q) =>
        `<button type="button" class="twa-chatbot__chip" data-quick="${q.id}">${esc(q.label)}</button>`,
    ).join("");
    quickEl.querySelectorAll("[data-quick]").forEach((btn) => {
      btn.addEventListener("click", () => handleQuick(btn.getAttribute("data-quick"), btn.textContent));
    });
  }

  function openWhatsApp(text) {
    const body = text ? `Hi ${BRAND}, ${text}` : `Hi ${BRAND}, I need help with a project.`;
    window.open(WA_URL + encodeURIComponent(body), "_blank", "noopener");
  }

  function handleQuick(id, label) {
    if (!id) return;
    const userLabel = label || id;
    addMessage(userLabel, "user");
    if (id === "whatsapp") {
      openWhatsApp("I need help with a project.");
      addMessage("WhatsApp khul gaya — wahan Send dabao, tab message aapke phone par dikhega.", "bot");
      afterUserMessage(userLabel);
      return;
    }
    addMessage(REPLIES[id] || REPLIES.default, "bot");
    afterUserMessage(userLabel);
  }

  function handleUserText(text) {
    const t = text.trim();
    if (!t) return;
    addMessage(t, "user");
    const lower = t.toLowerCase();
    if (/price|pricing|cost|kitna|plan|package/.test(lower)) addMessage(REPLIES.pricing, "bot");
    else if (/service|website|app|logo|develop/.test(lower)) addMessage(REPLIES.services, "bot");
    else if (/student|project|viva|final|college/.test(lower)) addMessage(REPLIES.student, "bot");
    else if (/contact|call|email|phone|time|deadline/.test(lower)) addMessage(REPLIES.contact, "bot");
    else if (/whatsapp|wa\b/.test(lower)) {
      openWhatsApp(t);
      addMessage("WhatsApp khul gaya — Send dabane ke baad message aapke WhatsApp par aayega.", "bot");
    } else addMessage(REPLIES.default, "bot");
    afterUserMessage(t);
  }

  function openChat() {
    if (!chatbotEl) return;
    chatbotEl.hidden = false;
    chatbotEl.setAttribute("aria-hidden", "false");
    if (floatEl) floatEl.setAttribute("aria-expanded", "true");
    isOpen = true;
    document.body.classList.add("twa-chatbot-open");
    if (messagesEl && !messagesEl.dataset.ready) {
      messagesEl.dataset.ready = "1";
      addMessage(`Hi! Welcome to ${BRAND}. How can we help you today?`, "bot");
      renderQuick();
      updateWaLink("");
    }
    inputEl?.focus();
  }

  function closeChat() {
    if (!chatbotEl) return;
    chatbotEl.hidden = true;
    chatbotEl.setAttribute("aria-hidden", "true");
    if (floatEl) floatEl.setAttribute("aria-expanded", "false");
    isOpen = false;
    document.body.classList.remove("twa-chatbot-open");
  }

  function toggleChat() {
    if (isOpen) closeChat();
    else openChat();
  }

  function bindFloat(el) {
    floatEl = el;
    if (el.tagName === "A") {
      el.href = "#";
      el.removeAttribute("target");
      el.removeAttribute("rel");
    }
    el.setAttribute("role", "button");
    el.setAttribute("aria-controls", "twa-chatbot");
    el.setAttribute("aria-expanded", "false");
    el.setAttribute("aria-label", "Open chat assistant");
    el.title = "Chat with us";
    el.addEventListener("click", (e) => {
      e.preventDefault();
      toggleChat();
    });
  }

  function bindPanel() {
    chatbotEl = document.getElementById("twa-chatbot");
    messagesEl = document.getElementById("twa-chatbot-messages");
    quickEl = document.getElementById("twa-chatbot-quick");
    formEl = document.getElementById("twa-chatbot-form");
    inputEl = document.getElementById("twa-chatbot-input");
    waLinkEl = document.querySelector(".twa-chatbot__wa-link");

    if (!chatbotEl) return false;

    chatbotEl.querySelector(".twa-chatbot__close")?.addEventListener("click", closeChat);

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && isOpen) closeChat();
    });

    formEl?.addEventListener("submit", (e) => {
      e.preventDefault();
      const v = inputEl?.value || "";
      if (inputEl) inputEl.value = "";
      handleUserText(v);
    });

    waLinkEl?.addEventListener("click", () => {
      if (lastUserText) saveMessage(lastUserText);
    });

    return true;
  }

  function boot() {
    floatEl = document.getElementById("whatsapp-float");
    if (!floatEl) return;
    bindFloat(floatEl);
    if (!bindPanel()) {
      console.warn("[chatbot] Panel #twa-chatbot not found on this page.");
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
