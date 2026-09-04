(function () {
  var popup = document.getElementById("promo-popup");
  if (!popup) return;
  var id = popup.getAttribute("data-promo-id") || "x";
  var key = "promo-dismissed-" + id;
  try {
    if (sessionStorage.getItem(key) === "1") return;
  } catch (e) {}

  function close() {
    popup.hidden = true;
    document.body.classList.remove("promo-popup-open");
    try {
      sessionStorage.setItem(key, "1");
    } catch (e2) {}
  }

  window.setTimeout(function () {
    popup.hidden = false;
    document.body.classList.add("promo-popup-open");
  }, 700);

  popup.querySelectorAll("[data-promo-close]").forEach(function (el) {
    el.addEventListener("click", close);
  });
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && !popup.hidden) close();
  });
})();
