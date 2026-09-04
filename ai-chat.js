/**
 * =============================================================================
 * CLIENT — ai-chat.js
 * File: ai-chat.js
 * AI chat widget open/send logic
 * =============================================================================
 */

(function () {
  const BRAND = "Shrisha Technology";
  let panel;
  let messagesEl;
  let statusEl;
  let formEl;
  let inputEl;
  let floatBtn;
  let isOpen = false;
  let busy = false;
  const history = [];

  function canUseApi() {
    return location.protocol === "http:" || location.protocol === "https:";
  }

  function normalize(text) {
    return String(text || "")
      .toLowerCase()
      .replace(/[^\w\s]/g, " ")
      .replace(/\s+/g, " ")
      .trim();
  }

  function recentCtx(history) {
    return (history || [])
      .filter((h) => h && h.role === "user" && h.content)
      .slice(-3)
      .map((h) => normalize(h.content))
      .join(" ");
  }

  /** Offline backup — server mode is much smarter (uses ai-fallback.js) */
  function localFallbackReply(message, history) {
    const m = normalize(message);
    const combined = `${m} ${recentCtx(history)}`.trim();

    if (/price|pricing|cost|kitna|plan|package|budget|paisa/.test(combined)) {
      return "Website plans: Starter from ₹2,999, Professional from ₹7,999, Premium/CMS from ₹12,999. Apps & logo alag packages. Pricing page dekho — exact quote WhatsApp +91 79920 20591 par.";
    }

    if (/banani|banwana|bana do|website chahiye|site chahiye|website.*ban|ban.*website/.test(combined)) {
      return "Website banwane ke liye batayein: business site / portfolio / shop? Kitne pages? Starter plan ₹2,999 se shuru (1 page + form + WhatsApp). Professional ₹7,999 (5 pages). Contact form ya WhatsApp par details bhejo — hum quote aur timeline denge.";
    }

    if (/student|viva|final year|college|btech|project/.test(combined)) {
      return "Student project: code, report, PPT, viva help — deadline batao. WhatsApp +91 79920 20591 ya Contact page use karo.";
    }

    if (/app|android|flutter|mobile/.test(combined)) {
      return "App development Android/Flutter se — login, dashboard, Play Store help. Pricing page par app packages dekho ya WhatsApp par idea share karo.";
    }

    if (/logo|brand/.test(combined)) {
      return "Logo design ~₹1,499 se — concepts, vector files, social sizes. Brand name aur style batao.";
    }

    if (/website|web|shop|portfolio|ecommerce/.test(combined)) {
      return "Hum websites banate hain: business, portfolio, e-commerce — mobile friendly, enquiry form, WhatsApp button. Pages count aur deadline batao for quote.";
    }

    if (/contact|call|email|phone|whatsapp|number|baat/.test(combined)) {
      return "Contact: WhatsApp/call +91 79920 20591, email mayankklush2006@gmail.com. Contact page par form bhi hai.";
    }

    if (/time|delivery|deadline|kitne din|kab tak|jaldi/.test(combined)) {
      return "Simple site often 5–10 working days; bade project 2–4+ weeks. Deadline share karo, hum confirm karenge.";
    }

    if (/kaise|kese|process|steps/.test(combined)) {
      return "Step 1: requirement bhejo → 2: quote/timeline → 3: design → 4: build → 5: launch. WhatsApp +91 79920 20591 ya /contact se shuru karo.";
    }

    if (/hello|hi|hey|namaste|help|hii/.test(combined)) {
      return "Hi! Main Shrisha Technology assistant. Website, app, logo, student project — kya chahiye?";
    }

    return "Thanks! Shrisha Technology websites, apps, logos & student projects karta hai. Pricing/services poochho ya WhatsApp +91 79920 20591 par likho — team reply karegi.";
  }

  function esc(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function addBubble(text, who) {
    if (!messagesEl) return;
    const row = document.createElement("div");
    row.className = `twa-ai-chat__msg twa-ai-chat__msg--${who}`;
    row.innerHTML = `<p>${esc(text)}</p>`;
    messagesEl.appendChild(row);
    messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  function setStatus(text) {
    if (statusEl) statusEl.textContent = text;
  }

  function setBusy(on) {
    busy = on;
    if (inputEl) inputEl.disabled = on;
    if (formEl) {
      const btn = formEl.querySelector(".twa-ai-chat__send");
      if (btn) btn.disabled = on;
    }
  }

  async function checkStatus() {
    if (!canUseApi()) {
      setStatus("Offline mode — npm start se http://127.0.0.1:3000 kholo (full server)");
      return;
    }
    try {
      const r = await fetch("/api/ai/status");
      const d = await r.json();
      if (d.smart) setStatus("Smart AI · Hindi/English samajhta hai");
      else if (d.aiEnabled) setStatus("AI assistant · OpenAI");
      else setStatus("Smart local · .env me OPENAI_API_KEY = zyada human jaisa");
    } catch {
      setStatus("AI assistant · server start karo (npm start)");
    }
  }

  async function sendMessage(text) {
    const t = text.trim();
    if (!t || busy) return;

    addBubble(t, "user");
    history.push({ role: "user", content: t });
    setBusy(true);
    addBubble("…", "bot");

    const pending = messagesEl?.lastElementChild;

    function finish(reply) {
      if (pending) pending.remove();
      addBubble(reply, "bot");
      history.push({ role: "assistant", content: reply });
    }

    if (!canUseApi()) {
      finish(localFallbackReply(t, history));
      setBusy(false);
      inputEl?.focus();
      return;
    }

    try {
      const res = await fetch("/api/ai/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: t, history: history.slice(-12) }),
      });
      const data = await res.json();

      if (!data.ok) {
        finish(localFallbackReply(t, history));
        return;
      }

      finish(data.reply);
    } catch {
      finish(localFallbackReply(t, history));
    } finally {
      setBusy(false);
      inputEl?.focus();
    }
  }

  function openPanel() {
    if (!panel) return;
    panel.hidden = false;
    panel.setAttribute("aria-hidden", "false");
    floatBtn?.setAttribute("aria-expanded", "true");
    isOpen = true;
    document.body.classList.add("twa-ai-chat-open");

    if (!messagesEl.dataset.ready) {
      messagesEl.dataset.ready = "1";
      addBubble(
        `Namaste! Main ${BRAND} ka smart assistant hoon — Hindi, English, Hinglish sab samajhta hoon. Website, app, logo, student project — casually likho, main help karunga.`,
        "bot",
      );
      checkStatus();
    }
    inputEl?.focus();
  }

  function closePanel() {
    if (!panel) return;
    panel.hidden = true;
    panel.setAttribute("aria-hidden", "true");
    floatBtn?.setAttribute("aria-expanded", "false");
    isOpen = false;
    document.body.classList.remove("twa-ai-chat-open");
  }

  function togglePanel() {
    if (isOpen) closePanel();
    else openPanel();
  }

  function bind() {
    panel = document.getElementById("twa-ai-chat");
    messagesEl = document.getElementById("twa-ai-chat-messages");
    statusEl = document.getElementById("twa-ai-chat-status");
    formEl = document.getElementById("twa-ai-chat-form");
    inputEl = document.getElementById("twa-ai-chat-input");
    floatBtn = document.getElementById("ai-chat-float");

    if (!panel || !floatBtn) return false;

    floatBtn.addEventListener("click", togglePanel);
    panel.querySelector(".twa-ai-chat__close")?.addEventListener("click", closePanel);

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && isOpen) closePanel();
    });

    formEl?.addEventListener("submit", (e) => {
      e.preventDefault();
      const v = inputEl?.value || "";
      if (inputEl) inputEl.value = "";
      sendMessage(v);
    });

    return true;
  }

  function boot() {
    if (!bind()) {
      console.warn("[ai-chat] UI not found on this page.");
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
