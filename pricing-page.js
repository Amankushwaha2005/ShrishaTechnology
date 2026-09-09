/**
 * =============================================================================
 * CLIENT — pricing-page.js
 * URL: /pricing
 * File: pricing-page.js
 * Pricing carousel, order links
 * =============================================================================
 */

(function () {
  const data = window.PRICING_DATA;
  if (!data || !data.categories) return;

  const gridEl = document.getElementById("pricing-category-grid");
  const pickerSection = document.getElementById("pricing-picker");
  const detailSection = document.getElementById("pricing-detail");
  const detailHeader = document.getElementById("pricing-detail-header");
  const packagesEl = document.getElementById("pricing-packages");
  const extrasEl = document.getElementById("pricing-extra");
  const backBtn = document.querySelector(".pricing-back-btn");

  if (!gridEl || !detailSection) return;

  function formatInr(n) {
    if (n === 0) return "Free*";
    return "₹" + n.toLocaleString("en-IN");
  }

  const ADVANCE_PERCENT =
    typeof window.PAYMENT_ADVANCE_PERCENT === "number"
      ? window.PAYMENT_ADVANCE_PERCENT
      : parseInt(document.body?.dataset?.paymentAdvancePercent || "50", 10) || 50;

  function enquireUrl(category, pkg) {
    const q = new URLSearchParams({
      subject: "Pricing enquiry",
      plan: (category.title || "") + " — " + (pkg.title || ""),
      budget: pkg.priceLabel || formatInr(pkg.price),
    });
    return "/contact?" + q.toString();
  }

  function orderUrl(category, pkg) {
    const price = Number(pkg.price) || 0;
    if (price <= 0) return "";
    const advance = Math.max(1, Math.round((price * ADVANCE_PERCENT) / 100));
    const q = new URLSearchParams({
      service: category.title || "",
      plan: pkg.title || "",
      total: String(price),
      amount: String(advance),
    });
    return "/order?" + q.toString();
  }

  function renderCategoryGrid() {
    gridEl.innerHTML = data.categories
      .map(
        (cat) => `
      <button type="button" class="pricing-category-card" data-category-id="${cat.id}" aria-label="View ${cat.title} pricing">
        <div class="pricing-category-card__media">
          <img src="${cat.image}" alt="${cat.title}" loading="lazy" decoding="async" width="400" height="260">
        </div>
        <div class="pricing-category-card__body">
          <h3>${cat.title}</h3>
          <p>${cat.subtitle || ""}</p>
          <span class="pricing-category-card__cta">View packages →</span>
        </div>
      </button>`,
      )
      .join("");

    gridEl.querySelectorAll(".pricing-category-card").forEach((btn) => {
      btn.addEventListener("click", () => openCategory(btn.getAttribute("data-category-id")));
    });
  }

  function renderPackageCard(cat, pkg) {
    const optionsHtml = (pkg.options || [])
      .map(
        (opt) => `
        <div class="pricing-option-group">
          <span class="pricing-option-label">${opt.label}</span>
          <div class="pricing-option-choices">
            ${opt.choices
              .map(
                (c, i) => `
              <label class="pricing-option-choice">
                <input type="radio" name="opt-${cat.id}-${pkg.title.replace(/\s/g, "")}-${opt.label}" ${i === 0 ? "checked" : ""}>
                <span>${c}</span>
              </label>`,
              )
              .join("")}
          </div>
        </div>`,
      )
      .join("");

    return `
      <article class="pricing-package-card">
        <span class="pricing-ribbon ${pkg.ribbonClass || ""}">${pkg.ribbon || ""}</span>
        <h3>${pkg.title}</h3>
        <p class="pricing-package-desc">${pkg.description}</p>
        <div class="pricing-package-price-row">
          <strong class="pricing-package-price">${pkg.priceLabel || formatInr(pkg.price)}</strong>
        </div>
        <div class="pricing-package-actions">
          ${
            orderUrl(cat, pkg)
              ? `<a class="btn btn-small btn-pay" href="${orderUrl(cat, pkg)}">Book &amp; Pay Advance</a>`
              : ""
          }
          <a class="btn btn-small btn-outline" href="${enquireUrl(cat, pkg)}">Enquire</a>
        </div>
        <ul class="pricing-feature-list">
          ${(pkg.features || []).map((f) => `<li>${f}</li>`).join("")}
        </ul>
        ${optionsHtml ? `<div class="pricing-options">${optionsHtml}</div>` : ""}
      </article>`;
  }

  function renderExtras(cat) {
    const ex = cat.extras;
    if (!ex) {
      extrasEl.innerHTML = "";
      extrasEl.hidden = true;
      return;
    }
    extrasEl.hidden = false;
    extrasEl.innerHTML = `
      <div class="pricing-info-grid">
        <article class="pricing-info-card">
          <h4>Website delivery</h4>
          <p>${ex.delivery}</p>
        </article>
        <article class="pricing-info-card">
          <h4>Source files</h4>
          <p>${ex.source}</p>
        </article>
      </div>
      <article class="pricing-maintenance">
        <h3>Website with one-year free maintenance</h3>
        <p>${ex.maintenance}</p>
      </article>
      ${
        ex.steps
          ? `<div class="pricing-steps">
        <h3>What we cover for your startup</h3>
        <ol class="pricing-steps-list">
          ${ex.steps
            .map(
              (s, i) => `
            <li>
              <span class="pricing-step-num">${i + 1}</span>
              <div>
                <strong>${s.title}</strong>
                <p>${s.text}</p>
              </div>
            </li>`,
            )
            .join("")}
        </ol>
      </div>`
          : ""
      }`;
  }

  function openCategory(id) {
    const cat = data.categories.find((c) => c.id === id);
    if (!cat) return;

    pickerSection.hidden = true;
    detailSection.hidden = false;

    detailHeader.innerHTML = `
      <span class="chip">${cat.title}</span>
      <h2>${cat.heroTitle || cat.title + " — packages"}</h2>
      <p>${cat.heroText || cat.subtitle || "Select a package and enquire for a custom quote."}</p>`;

    packagesEl.innerHTML = (cat.packages || []).map((pkg) => renderPackageCard(cat, pkg)).join("");
    renderExtras(cat);

    detailSection.scrollIntoView({ behavior: "smooth", block: "start" });
    history.replaceState(null, "", "#" + id);
  }

  function closeDetail() {
    detailSection.hidden = true;
    pickerSection.hidden = false;
    history.replaceState(null, "", window.location.pathname);
    pickerSection.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  if (backBtn) backBtn.addEventListener("click", closeDetail);

  renderCategoryGrid();

  const hash = (window.location.hash || "").replace("#", "");
  if (hash) {
    const found = data.categories.some((c) => c.id === hash);
    if (found) openCategory(hash);
  }
})();
