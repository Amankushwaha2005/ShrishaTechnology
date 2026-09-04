/**
 * CLIENT — news-page.js
 * Live daily news from /api/news (Google News RSS via Java backend)
 */
(function () {
  const catsEl = document.getElementById("news-cats");
  const gridEl = document.getElementById("news-grid");
  const featuredEl = document.getElementById("news-featured");
  const emptyEl = document.getElementById("news-empty");
  const searchEl = document.getElementById("news-search");
  const sortEl = document.getElementById("news-sort");
  const trackEl = document.getElementById("news-ticker-track");
  const modal = document.getElementById("news-modal");
  const modalBody = document.getElementById("news-modal-body");
  const modalClose = document.getElementById("news-modal-close");

  let data = { ticker: [], articles: [] };
  let category = "All Insights";
  let categories = ["All Insights"];

  function fillTicker() {
    if (!trackEl) return;
    const headlines = data.ticker.length ? data.ticker : ["Loading today’s headlines…"];
    const items = headlines.concat(headlines);
    trackEl.innerHTML = items
      .map(
        (text) =>
          `<span class="news-ticker__item"><span class="news-ticker__fire" aria-hidden="true">🔥</span>${escapeHtml(
            String(text).toUpperCase()
          )}</span>`
      )
      .join("");
  }

  function renderCats() {
    catsEl.innerHTML = categories
      .map((c) => {
        const active = c === category ? " is-active" : "";
        return `<button type="button" class="news-cat${active}" data-cat="${escapeAttr(c)}">${escapeHtml(c)}</button>`;
      })
      .join("");
  }

  function filtered() {
    const q = (searchEl.value || "").trim().toLowerCase();
    let list = data.articles.slice();
    if (category !== "All Insights") {
      list = list.filter((a) => a.category === category);
    }
    if (q) {
      list = list.filter((a) =>
        (a.title + " " + a.excerpt + " " + a.category + " " + (a.author || "")).toLowerCase().includes(q)
      );
    }
    const sort = sortEl.value;
    list.sort((a, b) => {
      if (sort === "oldest") return String(a.date).localeCompare(String(b.date));
      if (sort === "title") return String(a.title).localeCompare(String(b.title));
      return String(b.publishedAt || b.date).localeCompare(String(a.publishedAt || a.date));
    });
    return list;
  }

  function render() {
    const list = filtered();
    emptyEl.hidden = list.length > 0;
    if (!list.length) {
      emptyEl.textContent = data.articles.length
        ? "No stories match your search."
        : "Live news is loading…";
    }

    const searching = !!(searchEl.value || "").trim();
    const feat =
      !searching && category === "All Insights"
        ? list.find((a) => a.featured) || list[0]
        : null;
    const rest = list.filter((a) => !feat || a.id !== feat.id);

    if (feat) {
      featuredEl.hidden = false;
      featuredEl.innerHTML = `
        <div class="news-featured__top">
          <span class="news-featured__badge">Today</span>
          <span class="news-featured__cat">${escapeHtml(feat.category)}</span>
        </div>
        <h2>${escapeHtml(feat.title)}</h2>
        <p>${escapeHtml(feat.excerpt)}</p>
        <div class="news-featured__foot">
          <span>${escapeHtml(feat.author || "News")} · ${escapeHtml(feat.date)} · ${feat.readMins || 1} min read</span>
          <a class="news-read" href="${escapeAttr(feat.url)}" target="_blank" rel="noopener noreferrer">Read article →</a>
        </div>`;
    } else {
      featuredEl.hidden = true;
      featuredEl.innerHTML = "";
    }

    gridEl.innerHTML = rest
      .map(
        (a) => `
      <article class="news-card">
        <div class="news-card__meta">
          <span>${escapeHtml(a.category)}</span>
          <time datetime="${escapeAttr(a.date)}">${escapeHtml(a.date)}</time>
        </div>
        <h3>${escapeHtml(a.title)}</h3>
        <p>${escapeHtml(a.excerpt)}</p>
        <div class="news-card__foot">
          <span>${escapeHtml(a.author || "")}</span>
          <a class="news-read" href="${escapeAttr(a.url)}" target="_blank" rel="noopener noreferrer">Read →</a>
        </div>
      </article>`
      )
      .join("");
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }
  function escapeAttr(s) {
    return escapeHtml(s).replace(/'/g, "&#39;");
  }

  catsEl.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-cat]");
    if (!btn) return;
    category = btn.getAttribute("data-cat");
    renderCats();
    render();
  });

  searchEl.addEventListener("input", render);
  sortEl.addEventListener("change", render);
  if (modalClose) modalClose.addEventListener("click", () => (modal.hidden = true));
  if (modal) {
    const backdrop = modal.querySelector(".news-modal-backdrop");
    if (backdrop) backdrop.addEventListener("click", () => (modal.hidden = true));
  }

  fillTicker();
  renderCats();
  emptyEl.hidden = false;
  emptyEl.textContent = "Loading today’s live headlines…";

  fetch("/api/news")
    .then((r) => r.json())
    .then((json) => {
      if (!json || !json.ok || !Array.isArray(json.articles) || !json.articles.length) {
        emptyEl.hidden = false;
        emptyEl.textContent = "Live news could not be loaded. Please refresh in a minute.";
        return;
      }
      data = json;
      categories = ["All Insights", ...Array.from(new Set(data.articles.map((a) => a.category)))];
      category = "All Insights";
      fillTicker();
      renderCats();
      render();
    })
    .catch(() => {
      emptyEl.hidden = false;
      emptyEl.textContent = "Live news could not be loaded. Check your internet and refresh.";
    });
})();
