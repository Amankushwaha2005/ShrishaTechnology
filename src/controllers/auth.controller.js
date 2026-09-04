/**
 * =============================================================================
 * BACKEND — auth controller
 * File: src/controllers/auth.controller.js
 * Login, signup, Google / GitHub / Microsoft OAuth, logout
 * =============================================================================
 */

const crypto = require("crypto");
const bcrypt = require("bcryptjs");

const { query, queryOne } = require("../services/db");
const { validateFormRecaptcha } = require("../services/recaptcha.service");
const { roleForEmail, syncAdminRole } = require("../services/user-role.service");
const { isProviderConfigured } = require("../config/oauth-providers");
const {
  getProviderConfig,
  buildAuthUrl,
  exchangeCode,
  fetchProfile,
  upsertOAuthUser,
} = require("../services/oauth.service");

function isGoogleOAuthReady() {
  return isProviderConfigured("google");
}

function isGitHubOAuthReady() {
  return isProviderConfigured("github");
}

function isMicrosoftOAuthReady() {
  return isProviderConfigured("microsoft");
}

function authError(res, message, returnTo = "login") {
  const base = returnTo === "signup" ? "/signup" : "/login";
  return res.redirect(`${base}?error=${encodeURIComponent(message)}`);
}

function loginError(res, message) {
  return authError(res, message, "login");
}

function signupError(res, message) {
  return authError(res, message, "signup");
}

function getReturnTo(req) {
  return req.query.from === "signup" ? "signup" : "login";
}

function finishOAuthRedirect(req, res, returnTo) {
  const next = req.session.afterLoginRedirect;
  delete req.session.afterLoginRedirect;
  const toast = returnTo === "signup" ? "signup" : "login";
  if (typeof next === "string" && next.startsWith("/") && !next.startsWith("//")) {
    const sep = next.includes("?") ? "&" : "?";
    return res.redirect(`${next}${sep}toast=${toast}`);
  }
  return res.redirect(`/?toast=${toast}`);
}

async function login(req, res) {
  const captcha = await validateFormRecaptcha(req);
  if (!captcha.ok) return loginError(res, captcha.error);

  const { email, password } = req.body || {};

  if (!email || !password) return loginError(res, "Email and password are required.");

  const user = await queryOne(
    `SELECT id, provider, name, email, password_hash, picture, role
     FROM users WHERE email = $1`,
    [String(email).trim().toLowerCase()],
  );

  if (!user || !user.password_hash) return loginError(res, "Invalid email or password.");
  const ok = bcrypt.compareSync(String(password), user.password_hash);
  if (!ok) return loginError(res, "Invalid email or password.");

  const synced = await syncAdminRole(user);
  req.session.userId = synced.id;
  const next = req.body?.next;
  if (typeof next === "string" && next.startsWith("/") && !next.startsWith("//")) {
    const sep = next.includes("?") ? "&" : "?";
    return res.redirect(`${next}${sep}toast=login`);
  }
  return res.redirect("/?toast=login");
}

async function signup(req, res) {
  const captcha = await validateFormRecaptcha(req);
  if (!captcha.ok) return signupError(res, captcha.error);

  const { name, email, password } = req.body || {};

  if (!name || !email || !password) return signupError(res, "Name, email and password are required.");

  const normalizedEmail = String(email).trim().toLowerCase();
  const exists = await queryOne("SELECT id FROM users WHERE email = $1", [normalizedEmail]);
  if (exists) return signupError(res, "Email already exists. Please login.");

  const passwordHash = bcrypt.hashSync(String(password), 10);
  const role = roleForEmail(normalizedEmail, "user");
  const inserted = await queryOne(
    `INSERT INTO users (provider, name, email, password_hash, role)
     VALUES ('local', $1, $2, $3, $4)
     RETURNING id`,
    [String(name).trim(), normalizedEmail, passwordHash, role],
  );

  req.session.userId = inserted.id;
  const next = req.body?.next;
  if (typeof next === "string" && next.startsWith("/") && !next.startsWith("//")) {
    const sep = next.includes("?") ? "&" : "?";
    return res.redirect(`${next}${sep}toast=signup`);
  }
  return res.redirect("/?toast=signup");
}

function startOAuth(providerKey, req, res) {
  const returnTo = getReturnTo(req);
  req.session.oauthReturnTo = returnTo;
  req.session.oauthProvider = providerKey;

  const config = getProviderConfig(req, providerKey);
  if (!config?.clientId || !config?.clientSecret) {
    const envHint =
      providerKey === "google"
        ? "GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET"
        : providerKey === "github"
          ? "GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET"
          : "MICROSOFT_CLIENT_ID and MICROSOFT_CLIENT_SECRET";
    return authError(
      res,
      `${config?.label || providerKey} login is not configured. Add ${envHint} on Render.`,
      returnTo,
    );
  }

  const state = crypto.randomBytes(24).toString("hex");
  req.session.oauthState = state;

  return res.redirect(buildAuthUrl(providerKey, config, state));
}

async function oauthCallback(providerKey, req, res) {
  const returnTo = req.session.oauthReturnTo === "signup" ? "signup" : "login";
  const { code, state } = req.query || {};

  if (!code || !state || state !== req.session.oauthState) {
    return authError(res, `${providerKey} login failed. Please try again.`, returnTo);
  }

  if (req.session.oauthProvider && req.session.oauthProvider !== providerKey) {
    return authError(res, "Login session mismatch. Please try again.", returnTo);
  }

  delete req.session.oauthState;
  delete req.session.oauthReturnTo;
  delete req.session.oauthProvider;

  const config = getProviderConfig(req, providerKey);
  if (!config?.clientId || !config?.clientSecret) {
    return authError(res, `${config.label} login is not configured yet.`, returnTo);
  }

  try {
    const accessToken = await exchangeCode(providerKey, code, config);
    const profile = await fetchProfile(providerKey, accessToken);
    const userId = await upsertOAuthUser(providerKey, profile);
    req.session.userId = userId;
    return finishOAuthRedirect(req, res, returnTo);
  } catch (error) {
    console.error(`[oauth:${providerKey}]`, error.message);
    return authError(res, error.message || `${config.label} login failed.`, returnTo);
  }
}

function googleAuth(req, res) {
  return startOAuth("google", req, res);
}

function googleCallback(req, res) {
  return oauthCallback("google", req, res);
}

function githubAuth(req, res) {
  return startOAuth("github", req, res);
}

function githubCallback(req, res) {
  return oauthCallback("github", req, res);
}

function microsoftAuth(req, res) {
  return startOAuth("microsoft", req, res);
}

function microsoftCallback(req, res) {
  return oauthCallback("microsoft", req, res);
}

function logout(req, res) {
  req.session.destroy(() => {
    res.clearCookie("shrisha-technology.sid");
    res.redirect("/");
  });
}

module.exports = {
  login,
  signup,
  googleAuth,
  googleCallback,
  githubAuth,
  githubCallback,
  microsoftAuth,
  microsoftCallback,
  logout,
  isGoogleOAuthReady,
  isGitHubOAuthReady,
  isMicrosoftOAuthReady,
};
