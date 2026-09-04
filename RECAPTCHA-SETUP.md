# reCAPTCHA setup (I'm not a robot)

## Error: `Invalid key type`

Your site uses **reCAPTCHA v2 Checkbox** (`g-recaptcha` widget).

This error means Google keys are **v3** (or another type), not **v2 Checkbox**.

## Fix (5 minutes)

1. Open https://www.google.com/recaptcha/admin/create
2. Choose:
   - **reCAPTCHA type:** **Challenge (v2)**
   - **Subtype:** **"I'm not a robot" Checkbox** (NOT v3, NOT invisible-only)
3. **Domains** — add both:
   - `techwithaman-website-2026.onrender.com`
   - `localhost` (for local testing)
4. Submit → copy:
   - **Site key** → Render `RECAPTCHA_SITE_KEY`
   - **Secret key** → Render `RECAPTCHA_SECRET_KEY`
5. Render → **Save, rebuild, and deploy**

## Render variables (names must match exactly)

| Key | Value |
|-----|--------|
| `RECAPTCHA_SITE_KEY` | Site key from Google (public) |
| `RECAPTCHA_SECRET_KEY` | Secret key from Google (private) |

Do **not** put reCAPTCHA keys in `SESSION_SECRET`.  
`SESSION_SECRET` must be a long random string (Generate in Render or use `openssl rand -hex 32`).

## Test

After deploy, open `/login` or `/work` — you should see the normal Google checkbox, not a red error box.
