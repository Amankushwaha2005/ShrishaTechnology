/**
 * =============================================================================
 * CLIENT — portfolio-page.js
 * URL: /portfolio
 * File: portfolio-page.js
 * Portfolio filter and display
 * =============================================================================
 */

(function () {
  function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : String(text);
    return div.innerHTML;
  }

  function isProjectAvailable(p) {
    return p.available === true && typeof p.portfolioUrl === "string" && p.portfolioUrl.trim().length > 0;
  }

  function projectImage(p, width) {
    const raw = typeof p.image === "string" ? p.image.trim() : "";
    if (!raw) return "";
    return width ? raw.replace("w=900", width) : raw;
  }

  function initPortfolio(projects) {
    const data = { projects: projects || [] };

    const grid = document.getElementById("portfolio-grid");
    const modal = document.getElementById("portfolio-detail-modal");
    const modalBody = document.getElementById("portfolio-detail-body");
    const modalClose = document.querySelector(".portfolio-detail-close");
    const modalBackdrop = document.querySelector(".portfolio-detail-backdrop");

    const empty = document.getElementById("portfolio-empty");
    if (!grid) return false;
    if (!data.projects.length) {
      grid.innerHTML = "";
      if (empty) empty.hidden = false;
      return true;
    }
    if (empty) empty.hidden = true;

    function mediaHtml(p, title, wide) {
      const src = projectImage(p, wide ? "w=1200" : "");
      if (!src) {
        return `<div class="portfolio-card__fallback" aria-hidden="true">${escapeHtml((title || "P").charAt(0))}</div>`;
      }
      return `<img src="${escapeHtml(src)}" alt="${escapeHtml(title)}" loading="lazy">`;
    }

    function renderUnavailableModal(p) {
      const title = escapeHtml(p.title);
      const category = escapeHtml(p.category);
      const summary = escapeHtml(p.summary);

      modalBody.innerHTML = `
      <div class="home-detail-grid portfolio-detail--unavailable">
        <div class="home-detail-media">
          ${mediaHtml(p, p.title, true)}
        </div>
        <div class="home-detail-copy">
          <span class="chip">${category}</span>
          <h2 id="portfolio-detail-title">${title}</h2>
          <p class="home-detail-summary">${summary}</p>
          <div class="portfolio-not-available" role="status">
            <strong>Delivered</strong>
            <p>This project is delivered. A live link has not been added yet.</p>
          </div>
        </div>
      </div>`;
    }

    function renderAvailableModal(p) {
      const title = escapeHtml(p.title);
      const category = escapeHtml(p.category);
      const summary = escapeHtml(p.summary);
      const url = escapeHtml(p.portfolioUrl.trim());
      const details = (p.details || []).map((d) => `<li>${escapeHtml(d)}</li>`).join("");
      const gallery = (p.gallery || [])
        .map(
          (src) =>
            `<img src="${escapeHtml(src)}" alt="" loading="lazy" class="portfolio-detail-gallery__img">`,
        )
        .join("");

      const galleryBlock = gallery
        ? `<div class="portfolio-detail-gallery">${gallery}</div>`
        : "";

      modalBody.innerHTML = `
      <div class="home-detail-grid">
        <div class="home-detail-media">
          ${mediaHtml(p, p.title, true)}
          ${galleryBlock}
        </div>
        <div class="home-detail-copy">
          <span class="chip">${category}</span>
          <h2 id="portfolio-detail-title">${title}</h2>
          <p class="home-detail-summary">${summary}</p>
          <ul class="home-detail-list">${details}</ul>
          <div class="home-detail-actions">
            <a class="btn" href="${url}" target="_blank" rel="noopener noreferrer">View live portfolio</a>
          </div>
        </div>
      </div>`;
    }

    function openProject(id) {
      const p = data.projects.find((x) => x.id === id);
      if (!p || !modal || !modalBody) return;

      if (isProjectAvailable(p)) {
        renderAvailableModal(p);
      } else {
        renderUnavailableModal(p);
      }

      modal.hidden = false;
      document.body.classList.add("home-modal-open");
    }

    function closeProject() {
      if (!modal) return;
      modal.hidden = true;
      document.body.classList.remove("home-modal-open");
    }

    grid.innerHTML = data.projects
      .map((p) => {
        const ready = isProjectAvailable(p);
        const badge = ready
          ? '<span class="portfolio-card__badge portfolio-card__badge--live">Delivered</span>'
          : '<span class="portfolio-card__badge">Delivered</span>';
        const linkText = ready ? "View portfolio →" : "View details →";

        return `
      <article class="card portfolio-card" data-portfolio-id="${escapeHtml(p.id)}" role="button" tabindex="0">
        <div class="portfolio-card__media">
          ${mediaHtml(p, p.title, false)}
          ${badge}
        </div>
        <div class="portfolio-card__body">
          <span class="portfolio-card__tag">${escapeHtml(p.category)}</span>
          <h3>${escapeHtml(p.title)}</h3>
          <p>${escapeHtml(p.summary)}</p>
          <span class="home-card__link">${linkText}</span>
        </div>
      </article>`;
      })
      .join("");

    grid.querySelectorAll(".portfolio-card").forEach((card) => {
      card.addEventListener("click", () => openProject(card.getAttribute("data-portfolio-id")));
      card.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          openProject(card.getAttribute("data-portfolio-id"));
        }
      });
    });

    if (modalClose) modalClose.addEventListener("click", closeProject);
    if (modalBackdrop) modalBackdrop.addEventListener("click", closeProject);
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeProject();
    });

    return true;
  }

  async function boot() {
    if (window.__portfolioReady) return;
    if (!document.getElementById("portfolio-grid")) return;
    window.__portfolioReady = true;
    let projects = [];
    try {
      const res = await fetch("/api/portfolio", { headers: { Accept: "application/json" } });
      if (res.ok) {
        const body = await res.json();
        projects = Array.isArray(body.projects) ? body.projects : [];
      }
    } catch (err) {
      projects = [];
    }
    initPortfolio(projects);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
