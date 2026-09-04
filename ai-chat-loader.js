/**
 * =============================================================================
 * CLIENT — ai-chat-loader.js
 * File: ai-chat-loader.js
 * Lazy-load AI chat scripts
 * =============================================================================
 */

(function () {
  if (document.getElementById("ai-chat-float")) {
    if (!document.querySelector('script[src*="ai-chat.js"]')) {
      const s = document.createElement("script");
      s.src = "/ai-chat.js";
      document.body.appendChild(s);
    }
    return;
  }

  const wrap = document.createElement("div");
  const aiIconSvg =
    '<svg class="twa-ai-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">' +
    '<g fill="currentColor" transform="translate(12 12)">' +
    '<path d="M -7 0 L -2.25 -3.15 L -0.7 -0.95 L -0.7 0.95 L -2.25 3.15 Z"/>' +
    '<path d="M -7 0 L -2.25 -3.15 L -0.7 -0.95 L -0.7 0.95 L -2.25 3.15 Z" transform="rotate(120)"/>' +
    '<path d="M -7 0 L -2.25 -3.15 L -0.7 -0.95 L -0.7 0.95 L -2.25 3.15 Z" transform="rotate(240)"/>' +
    "</g></svg>";

  wrap.innerHTML = `
  <button type="button" class="ai-chat-float" id="ai-chat-float" aria-label="Ask AI — open assistant" title="Ask AI" aria-expanded="false" aria-controls="twa-ai-chat">
    <span class="ai-chat-float__icon" aria-hidden="true">${aiIconSvg}</span>
    <span class="ai-chat-float__label">Ask AI</span>
  </button>
  <div id="twa-ai-chat" class="twa-ai-chat" hidden aria-hidden="true">
    <div class="twa-ai-chat__panel" role="dialog" aria-labelledby="twa-ai-chat-title" aria-modal="true">
      <header class="twa-ai-chat__head">
        <div class="twa-ai-chat__head-info">
          <span class="twa-ai-chat__avatar" aria-hidden="true">${aiIconSvg}</span>
          <div>
            <h2 id="twa-ai-chat-title" class="twa-ai-chat__title">Shrisha Technology Assistant</h2>
            <p class="twa-ai-chat__status" id="twa-ai-chat-status">Ask about services, pricing & projects</p>
          </div>
        </div>
        <button type="button" class="twa-ai-chat__close" aria-label="Close AI chat">×</button>
      </header>
      <div id="twa-ai-chat-messages" class="twa-ai-chat__messages"></div>
      <form id="twa-ai-chat-form" class="twa-ai-chat__form" autocomplete="off">
        <input id="twa-ai-chat-input" type="text" placeholder="Ask about website, app, logo, student project…" maxlength="2000" aria-label="Your question" />
        <button type="submit" class="twa-ai-chat__send" aria-label="Send">➤</button>
      </form>
      <p class="twa-ai-chat__foot">Human help: <a href="contact.html">Contact</a></p>
    </div>
  </div>`;

  while (wrap.firstElementChild) {
    document.body.appendChild(wrap.firstElementChild);
  }

  const s = document.createElement("script");
  s.src = "/ai-chat.js";
  document.body.appendChild(s);
})();
