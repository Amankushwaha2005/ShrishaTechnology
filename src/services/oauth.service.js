/**
 * Shared OAuth — token exchange, profile fetch, user upsert
 */

const crypto = require("crypto");

const { query, queryOne } = require("./db");
const { PROVIDERS, isProviderConfigured } = require("../config/oauth-providers");
const { roleForEmail } = require("./user-role.service");

function getBaseUrl(req) {
  return (process.env.BASE_URL || `${req.protocol}://${req.get("host")}`).replace(/\/$/, "");
}

function getProviderConfig(req, providerKey) {
  const p = PROVIDERS[providerKey];
  if (!p) return null;
  const baseUrl = getBaseUrl(req);
  return {
    ...p,
    clientId: process.env[p.clientIdEnv]?.trim(),
    clientSecret: process.env[p.clientSecretEnv]?.trim(),
    redirectUri:
      process.env[p.redirectUriEnv]?.trim() || `${baseUrl}${p.callbackPath}`,
  };
}

async function exchangeCode(providerKey, code, config) {
  const body = new URLSearchParams({
    client_id: config.clientId,
    client_secret: config.clientSecret,
    code,
    redirect_uri: config.redirectUri,
    grant_type: "authorization_code",
  });

  const headers = { "Content-Type": "application/x-www-form-urlencoded" };
  if (config.tokenAcceptJson) {
    headers.Accept = "application/json";
  }

  const resp = await fetch(config.tokenUrl, { method: "POST", headers, body });
  const text = await resp.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = Object.fromEntries(new URLSearchParams(text));
  }

  if (!resp.ok) {
    console.error(`[oauth:${providerKey}] token`, resp.status, text);
    throw new Error(`${config.label} token exchange failed.`);
  }

  const accessToken = data.access_token;
  if (!accessToken) throw new Error(`${config.label} access token missing.`);
  return accessToken;
}

async function fetchGoogleProfile(accessToken) {
  const resp = await fetch("https://openidconnect.googleapis.com/v1/userinfo", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!resp.ok) throw new Error("Could not fetch Google profile.");
  const profile = await resp.json();
  const email = String(profile.email || "").trim().toLowerCase();
  if (!email) throw new Error("Google account email not available.");
  return {
    providerId: String(profile.sub || ""),
    name: profile.name || profile.given_name || "User",
    email,
    picture: profile.picture || "",
  };
}

async function fetchGitHubProfile(accessToken) {
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    Accept: "application/vnd.github+json",
    "User-Agent": "Shrisha-Technology-Website",
  };

  const userResp = await fetch("https://api.github.com/user", { headers });
  if (!userResp.ok) throw new Error("Could not fetch GitHub profile.");
  const user = await userResp.json();

  let email = String(user.email || "").trim().toLowerCase();
  if (!email) {
    const emailsResp = await fetch("https://api.github.com/user/emails", { headers });
    if (emailsResp.ok) {
      const emails = await emailsResp.json();
      const primary = emails.find((e) => e.primary && e.verified) || emails.find((e) => e.verified);
      email = String(primary?.email || emails[0]?.email || "").trim().toLowerCase();
    }
  }

  if (!email) {
    throw new Error(
      "GitHub email not available. In GitHub → Settings → Emails, add a verified email or make it public.",
    );
  }

  return {
    providerId: String(user.id || ""),
    name: user.name || user.login || "GitHub User",
    email,
    picture: user.avatar_url || "",
  };
}

async function fetchMicrosoftProfile(accessToken) {
  const resp = await fetch("https://graph.microsoft.com/v1.0/me", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!resp.ok) throw new Error("Could not fetch Microsoft profile.");
  const profile = await resp.json();

  const email = String(profile.mail || profile.userPrincipalName || "")
    .trim()
    .toLowerCase();
  if (!email) throw new Error("Microsoft account email not available.");

  return {
    providerId: String(profile.id || ""),
    name: profile.displayName || profile.givenName || "Microsoft User",
    email,
    picture: "",
  };
}

async function fetchProfile(providerKey, accessToken) {
  if (providerKey === "google") return fetchGoogleProfile(accessToken);
  if (providerKey === "github") return fetchGitHubProfile(accessToken);
  if (providerKey === "microsoft") return fetchMicrosoftProfile(accessToken);
  throw new Error("Unknown provider.");
}

async function upsertOAuthUser(providerKey, profile) {
  const { providerId, name, email, picture } = profile;

  let user = await queryOne(
    `SELECT id, provider, provider_id, email, role FROM users WHERE email = $1`,
    [email],
  );

  const role = roleForEmail(email, user?.role || "user");

  if (!user) {
    const inserted = await queryOne(
      `INSERT INTO users (provider, provider_id, name, email, picture, role)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id`,
      [providerKey, providerId || null, name, email, picture || null, role],
    );
    return inserted.id;
  }

  await query(
    `UPDATE users
     SET provider = $1,
         provider_id = COALESCE($2, provider_id),
         name = $3,
         picture = COALESCE(NULLIF($4, ''), picture),
         role = $5,
         updated_at = NOW()
     WHERE id = $6`,
    [providerKey, providerId || null, name, picture || "", role, user.id],
  );
  return user.id;
}

function buildAuthUrl(providerKey, config, state) {
  const params = new URLSearchParams({
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    response_type: "code",
    scope: config.scope,
    state,
    ...config.authExtra,
  });
  return `${config.authUrl}?${params.toString()}`;
}

module.exports = {
  PROVIDERS,
  isProviderConfigured,
  getProviderConfig,
  getBaseUrl,
  buildAuthUrl,
  exchangeCode,
  fetchProfile,
  upsertOAuthUser,
};
