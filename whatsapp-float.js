/**
 * =============================================================================
 * CLIENT — whatsapp-float.js
 * File: whatsapp-float.js
 * WhatsApp float button behavior
 * =============================================================================
 */

(function () {
  const PHONE = "917992020591";
  const TEXT = "Hi Shrisha Technology, I would like to discuss a project.";
  const VERSION = "2";

  const ICON_SVG =
    '<svg class="whatsapp-float__icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">' +
    '<path fill="#FFFFFF" d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.435 9.884-9.881 9.884z"/>' +
    "</svg>";

  function isMobile() {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(
      navigator.userAgent,
    );
  }

  function whatsappUrl() {
    const encoded = encodeURIComponent(TEXT);
    return isMobile()
      ? `https://wa.me/${PHONE}?text=${encoded}`
      : `https://web.whatsapp.com/send?phone=${PHONE}&text=${encoded}`;
  }

  function openWhatsApp(e) {
    e.preventDefault();
    window.open(whatsappUrl(), "_blank", "noopener,noreferrer");
  }

  function bind(el) {
    el.href = whatsappUrl();
    el.setAttribute("target", "_blank");
    el.setAttribute("rel", "noopener noreferrer");
    el.addEventListener("click", openWhatsApp);
  }

  function renderButton(el) {
    el.className = "whatsapp-float";
    el.setAttribute("data-wa-version", VERSION);
    el.setAttribute("aria-label", "Chat on WhatsApp");
    el.title = "WhatsApp — Shrisha Technology";
    el.innerHTML = '<span class="whatsapp-float__btn">' + ICON_SVG + "</span>";
  }

  let el = document.getElementById("whatsapp-float");
  if (!el) {
    el = document.createElement("a");
    el.id = "whatsapp-float";
    document.body.appendChild(el);
  }

  if (el.getAttribute("data-wa-version") !== VERSION) {
    renderButton(el);
  }

  bind(el);
})();
