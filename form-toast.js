/**
 * =============================================================================
 * CLIENT — form-toast.js
 * File: form-toast.js
 * Success/error popup after form submit
 * =============================================================================
 */

(function () {
  var root = document.getElementById("form-toast-root");
  if (!root) return;

  var params = new URLSearchParams(window.location.search);
  var sent = params.get("sent");
  var toast = params.get("toast");
  var error = params.get("error");

  var messages = {
    contact: {
      type: "success",
      title: "Message sent!",
      text: "Thank you — we received your message and will get back to you soon.",
    },
    work: {
      type: "success",
      title: "Application submitted!",
      text: "Your application was received. We will contact you if you are selected.",
    },
    login: {
      type: "success",
      title: "Welcome back!",
      text: "You have logged in successfully.",
    },
    signup: {
      type: "success",
      title: "Account created!",
      text: "Your account was created successfully. Welcome to Shrisha Technology!",
    },
  };

  var payload = null;
  if (sent === "1") {
    var page = document.body && document.body.getAttribute("data-page");
    if (page === "contact" || window.location.pathname.indexOf("/contact") !== -1) {
      payload = messages.contact;
    } else if (page === "work" || window.location.pathname.indexOf("/work") !== -1) {
      payload = messages.work;
    }
  } else if (toast && messages[toast]) {
    payload = messages[toast];
  } else if (error) {
    payload = {
      type: "error",
      title: "Something went wrong",
      text: decodeURIComponent(error.replace(/\+/g, " ")),
    };
  }

  if (!payload) return;

  var icon =
    payload.type === "success"
      ? '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm-1.2 13.2-3.5-3.5 1.4-1.4 2.1 2.1 4.9-4.9 1.4 1.4-6.3 6.3Z"/></svg>'
      : '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm1 5v6h-2V7h2Zm0 8v2h-2v-2h2Z"/></svg>';

  root.innerHTML =
    '<div class="form-toast form-toast--' +
    payload.type +
    '" role="dialog" aria-modal="true">' +
    '<span class="form-toast__icon">' +
    icon +
    "</span>" +
    '<div class="form-toast__body">' +
    "<strong>" +
    payload.title +
    "</strong>" +
    "<p>" +
    payload.text +
    "</p>" +
    "</div>" +
    '<button type="button" class="form-toast__close" aria-label="Dismiss">&times;</button>' +
    "</div>";
  root.hidden = false;
  document.body.classList.add("form-toast-open");

  function dismiss() {
    root.hidden = true;
    root.innerHTML = "";
    document.body.classList.remove("form-toast-open");
  }

  root.querySelector(".form-toast__close").addEventListener("click", dismiss);
  root.addEventListener("click", function (event) {
    if (event.target === root) dismiss();
  });
  window.setTimeout(dismiss, 8000);

  if (sent || toast || error) {
    params.delete("sent");
    params.delete("toast");
    params.delete("error");
    var qs = params.toString();
    var nextUrl = window.location.pathname + (qs ? "?" + qs : "") + window.location.hash;
    window.history.replaceState({}, "", nextUrl);
  }
})();
