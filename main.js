/**
 * =============================================================================
 * CLIENT — main.js
 * File: main.js
 * Theme toggle, mobile nav, scroll helpers (all pages)
 * =============================================================================
 */

document.addEventListener("DOMContentLoaded", () => {
  const navbars = document.querySelectorAll(".navbar");

  navbars.forEach((navbar) => {
    const toggle = navbar.querySelector(".menu-toggle");
    const links = navbar.querySelectorAll(".nav-links a");

    if (!toggle) return;

    toggle.addEventListener("click", () => {
      navbar.classList.toggle("menu-open");
    });

    links.forEach((link) => {
      link.addEventListener("click", () => {
        navbar.classList.remove("menu-open");
      });
    });

    document.addEventListener("click", (event) => {
      if (!navbar.contains(event.target)) {
        navbar.classList.remove("menu-open");
      }
    });
  });

  document.querySelectorAll(".account-menu").forEach((menu) => {
    const btn = menu.querySelector(".account-menu__btn");
    const panel = menu.querySelector(".account-menu__dropdown");
    if (!btn || !panel) return;

    const close = () => {
      panel.hidden = true;
      btn.setAttribute("aria-expanded", "false");
    };

    btn.addEventListener("click", (event) => {
      event.stopPropagation();
      const open = panel.hidden;
      document.querySelectorAll(".account-menu__dropdown").forEach((el) => {
        el.hidden = true;
      });
      document.querySelectorAll(".account-menu__btn").forEach((el) => {
        el.setAttribute("aria-expanded", "false");
      });
      if (open) {
        panel.hidden = false;
        btn.setAttribute("aria-expanded", "true");
      }
    });

    document.addEventListener("click", (event) => {
      if (!menu.contains(event.target)) close();
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") close();
    });
  });

  const typewriterEl = document.getElementById("typewriter");
  if (typewriterEl) {
    const wordsRaw = typewriterEl.getAttribute("data-words") || "[]";
    let words = [];
    try {
      words = JSON.parse(wordsRaw);
    } catch (_) {
      words = [];
    }

    const typeSpeed = Number(typewriterEl.getAttribute("data-type-speed")) || 38;
    const deleteSpeed = Number(typewriterEl.getAttribute("data-delete-speed")) || 18;
    const pauseMs = Number(typewriterEl.getAttribute("data-pause")) || 2000;

    let wordIndex = 0;
    let charIndex = 0;
    let isDeleting = false;
    let timerId = null;

    const tick = () => {
      const currentWord = words[wordIndex % words.length] || "";

      if (!isDeleting) {
        charIndex = Math.min(charIndex + 1, currentWord.length);
      } else {
        charIndex = Math.max(charIndex - 1, 0);
      }

      typewriterEl.textContent = currentWord.slice(0, charIndex);

      if (!isDeleting && charIndex === currentWord.length) {
        timerId = setTimeout(() => {
          isDeleting = true;
          tick();
        }, pauseMs);
        return;
      }

      if (isDeleting && charIndex === 0) {
        isDeleting = false;
        wordIndex += 1;
        timerId = setTimeout(tick, 80);
        return;
      }

      timerId = setTimeout(tick, isDeleting ? deleteSpeed : typeSpeed);
    };

    if (Array.isArray(words) && words.length > 0) {
      tick();
    }

    window.addEventListener("beforeunload", () => {
      if (timerId) clearTimeout(timerId);
    });
  }

  const root = document.documentElement;
  const themeBtn = document.querySelector(".theme-toggle");

  const setTheme = (theme) => {
    if (theme === "light") {
      root.setAttribute("data-theme", "light");
    } else {
      root.removeAttribute("data-theme");
    }

    if (themeBtn) {
      const isLight = theme === "light";
      themeBtn.setAttribute("aria-label", isLight ? "Switch to dark theme" : "Switch to light theme");
      themeBtn.setAttribute("title", isLight ? "Switch to dark theme" : "Switch to light theme");
    }
  };

  const savedTheme = localStorage.getItem("theme");
  setTheme(savedTheme === "light" ? "light" : "dark");

  if (themeBtn) {
    themeBtn.addEventListener("click", () => {
      const isLight = root.getAttribute("data-theme") === "light";
      const next = isLight ? "dark" : "light";
      localStorage.setItem("theme", next);
      setTheme(next);
    });
  }

  const authMessageEl = document.getElementById("authMessage");
  if (authMessageEl) {
    const params = new URLSearchParams(window.location.search);
    const msg = params.get("msg");
    const error = params.get("error");
    const messages = {
      "signup-success": "Signup successful. Please login.",
      missing: "Please fill all required fields.",
      invalid: "Invalid email or password.",
      weak: "Password must be at least 6 characters.",
      exists: "Email already registered. Please login.",
    };
    const text = messages[msg] || messages[error];
    if (text) {
      authMessageEl.textContent = text;
      authMessageEl.style.display = "block";
    }
  }
});
