/**
 * CLIENT — track-order.js
 * Home live project tracker (#track)
 */
(function () {
  const form = document.getElementById("track-form");
  const input = document.getElementById("track-id");
  const errorEl = document.getElementById("track-error");
  const resultEl = document.getElementById("track-result");
  if (!form || !input || !errorEl || !resultEl) return;

  const STORE_KEY = "shrisha_order_id";
  let pollTimer = 0;
  let lastId = "";

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function readPrefill() {
    const params = new URLSearchParams(window.location.search);
    const fromQuery = (params.get("track") || "").trim();
    const hash = window.location.hash || "";
    let fromHash = "";
    const eq = hash.indexOf("=");
    if (hash.startsWith("#track=") && eq !== -1) {
      fromHash = decodeURIComponent(hash.slice(eq + 1)).trim();
    }
    let stored = "";
    try {
      stored = (localStorage.getItem(STORE_KEY) || "").trim();
    } catch (e) {
      stored = "";
    }
    return fromQuery || fromHash || stored;
  }

  function showError(message) {
    errorEl.hidden = !message;
    errorEl.textContent = message || "";
    if (message) resultEl.hidden = true;
  }

  function render(data) {
    const live = data.live
      ? '<p class="track-live">Live tracking on</p>'
      : "";
    const steps = (data.steps || [])
      .map((step) => {
        const cls = step.current ? "is-current" : step.done ? "is-done" : "";
        const detail = step.detail
          ? `<small>${escapeHtml(step.detail)}</small>`
          : "";
        return `<li class="track-step ${cls}">
          <span class="track-step__dot" aria-hidden="true"></span>
          <div>
            <strong>${escapeHtml(step.label)}</strong>
            ${detail}
          </div>
        </li>`;
      })
      .join("");
    const pay = data.payUrl
      ? `<p class="track-pay"><a class="btn" href="${escapeHtml(data.payUrl)}">${
          data.phase === "pending" ? "Pay advance" : "Pay remaining balance"
        }</a></p>`
      : "";
    resultEl.hidden = false;
    resultEl.innerHTML = `
      <div class="track-result__head">
        <div>
          <h3>Hi ${escapeHtml(data.client)}, here is your project</h3>
          <p class="track-result__meta">
            ${escapeHtml(data.service || "")} · ${escapeHtml(data.plan || "")}<br>
            Order ID <code>${escapeHtml(data.publicId)}</code>
            · Total ₹${escapeHtml(data.totalInr)}
            · Advance ₹${escapeHtml(data.advancePaid)}
            ${data.balanceDue > 0 ? "· Balance ₹" + escapeHtml(data.balanceDue) : ""}
          </p>
        </div>
        ${live}
      </div>
      <ol class="track-steps">${steps}</ol>
      ${pay}`;
  }

  async function fetchTrack(id, { silent } = {}) {
    const res = await fetch("/api/orders/track?id=" + encodeURIComponent(id), {
      headers: { Accept: "application/json" },
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok || !data.ok) {
      if (!silent) showError(data.error || "Could not find this order.");
      return null;
    }
    showError("");
    render(data);
    try {
      localStorage.setItem(STORE_KEY, data.publicId);
    } catch (e) {}
    return data;
  }

  function startPoll(id) {
    window.clearInterval(pollTimer);
    lastId = id;
    pollTimer = window.setInterval(async () => {
      if (document.hidden || lastId !== id) return;
      const data = await fetchTrack(id, { silent: true });
      if (data && !data.live) window.clearInterval(pollTimer);
    }, 12000);
  }

  async function track(id) {
    const orderId = (id || "").trim();
    if (!orderId) {
      showError("Enter your Order ID.");
      return;
    }
    input.value = orderId;
    const data = await fetchTrack(orderId);
    if (data && data.live) startPoll(orderId);
    else window.clearInterval(pollTimer);
  }

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    track(input.value);
  });

  const prefill = readPrefill();
  if (prefill) {
    input.value = prefill;
    if (window.location.hash.indexOf("track") !== -1 || new URLSearchParams(window.location.search).has("track")) {
      track(prefill);
    }
  }
})();
