/**
 * =============================================================================
 * CLIENT — service-topic.js
 * URL: /service-topic
 * File: service-topic.js
 * Service topic detail page logic
 * =============================================================================
 */

document.addEventListener("DOMContentLoaded", () => {
  const root = document.getElementById("topic-root");
  const missing = document.getElementById("topic-missing");
  if (!root || !missing) return;

  const esc = (s) => {
    const d = document.createElement("div");
    d.textContent = s;
    return d.innerHTML;
  };

  const params = new URLSearchParams(window.location.search);
  const slug = (params.get("topic") || "").trim();
  const data = window.SERVICE_TOPICS_DATA && window.SERVICE_TOPICS_DATA[slug];

  if (!data) {
    root.style.display = "none";
    missing.style.display = "block";
    document.title = "Service not found | Shrisha Technology";
    return;
  }

  missing.style.display = "none";
  root.style.display = "block";
  document.title = `${data.title} | Shrisha Technology`;

  const bullets =
    Array.isArray(data.bullets) && data.bullets.length
      ? `<ul class="topic-detail-bullets">${data.bullets.map((b) => `<li>${esc(b)}</li>`).join("")}</ul>`
      : "";

  root.innerHTML = `
    <div class="container">
      <nav class="topic-breadcrumb" aria-label="Breadcrumb">
        <a href="/">Home</a>
        <span aria-hidden="true">/</span>
        <a href="/services">Services</a>
        <span aria-hidden="true">/</span>
        <span>${esc(data.category)}</span>
        <span aria-hidden="true">/</span>
        <span class="topic-breadcrumb-current">${esc(data.title)}</span>
      </nav>
    </div>

    <section class="topic-hero">
      <img src="${esc(data.image)}" alt="${esc(data.title)}" width="1400" height="560" loading="eager" decoding="async">
      <div class="topic-hero-overlay"></div>
      <div class="topic-hero-inner container">
        <p class="topic-hero-label">${esc(data.category)}</p>
        <h1 class="topic-hero-title">${esc(data.title)}</h1>
      </div>
    </section>

    <article class="topic-detail-body section">
      <div class="topic-detail-inner">
        <p class="topic-detail-intro">${esc(data.intro)}</p>
        <p class="topic-detail-more">${esc(data.more)}</p>
        ${bullets}
        <p class="topic-detail-cta-wrap">
          <a class="btn" href="/contact?${new URLSearchParams({
            subject: "Service quote",
            plan: data.title || "",
            topic: slug,
          }).toString()}">Get a quote</a>
          <a class="btn btn-outline" href="/services">← Back to services</a>
        </p>
      </div>
    </article>
  `;
});
