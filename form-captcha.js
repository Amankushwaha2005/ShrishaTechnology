/**
 * CLIENT — form-captcha.js
 * Require Google reCAPTCHA before submit.
 */
(function () {
  document.querySelectorAll("form").forEach((form) => {
    if (!form.querySelector(".g-recaptcha")) return;
    form.addEventListener("submit", (event) => {
      const ready = window.grecaptcha && typeof window.grecaptcha.getResponse === "function";
      const token = ready ? window.grecaptcha.getResponse() : "";
      if (token) return;
      event.preventDefault();
      alert("Please complete the Google I'm not a robot check.");
    });
  });
})();
