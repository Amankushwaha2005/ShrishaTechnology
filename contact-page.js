/**
 * =============================================================================
 * CLIENT — contact-page.js
 * URL: /contact
 * File: contact-page.js
 * Prefill quote page from a service topic
 * =============================================================================
 */

document.addEventListener("DOMContentLoaded", () => {
  const params = new URLSearchParams(window.location.search);
  const slug = (params.get("topic") || "").trim();
  const data = window.SERVICE_TOPICS_DATA && window.SERVICE_TOPICS_DATA[slug];
  const card = document.getElementById("quote-topic-card");
  if (!card || !data) return;

  card.hidden = false;

  const hero = document.getElementById("quote-hero-title");
  if (hero) hero.textContent = "Quote: " + data.title;

  const cat = document.getElementById("quote-topic-cat");
  if (cat) cat.textContent = data.category || "Service";

  const title = document.getElementById("quote-topic-title");
  if (title) title.textContent = data.title;

  const intro = document.getElementById("quote-topic-intro");
  if (intro) {
    intro.textContent = [data.intro, data.more].filter(Boolean).join(" ");
  }

  const img = document.getElementById("quote-topic-image");
  if (img && data.image) {
    img.src = data.image;
    img.alt = data.title;
    img.hidden = false;
  }

  const bullets = document.getElementById("quote-topic-bullets");
  if (bullets && Array.isArray(data.bullets)) {
    bullets.innerHTML = data.bullets.map((b) => `<li>${escapeHtml(b)}</li>`).join("");
  }

  const message = document.getElementById("message");
  if (message && !String(message.value || "").trim()) {
    message.value = "I would like a quote for " + data.title + ".";
  }

  function escapeHtml(s) {
    const d = document.createElement("div");
    d.textContent = s;
    return d.innerHTML;
  }
});
