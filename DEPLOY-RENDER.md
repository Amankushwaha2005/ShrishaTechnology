# Deploy on Render (Shrisha Technology)

Live URL example: `https://techwithaman-website-2026.onrender.com`

## Fix: `ECONNREFUSED 127.0.0.1:5432` (deploy failed)

Render par **localhost par PostgreSQL nahi hota**. Web service par **`DATABASE_URL` set karna zaroori hai**.

1. Render Dashboard → **New +** → **PostgreSQL** (free) → region **Singapore** → Create  
2. PostgreSQL service kholo → **Connections** → copy **Internal Database URL**  
3. Web service **techwithaman-website-2026** → **Environment** → Add:
   - `DATABASE_URL` = (paste Internal URL)
   - `PGSSL` = `true`
   - `BASE_URL` = `https://techwithaman-website-2026.onrender.com`
4. **Save Changes** → **Manual Deploy** → Deploy latest commit  

Logs mein `Server running (PostgreSQL)` dikhna chahiye — `127.0.0.1:5432` error nahi.

## Fix: "Suspended by you" / site nahi khul rahi

Render par ye message **matlab service band (suspend) hai** — aksar galti se **Suspend** dabne se ya purani paused service resume ke baad.

### Step A — Resume (sabse pehle)

1. https://dashboard.render.com → apni service **`techwithaman-website`** kholo  
2. Upar status **Suspended** dikhe to → **Resume** (ya **Resume service**) dabao  
3. 1–2 min wait → **Manual Deploy** → **Deploy latest commit**  
4. **Logs** tab → build **Succeeded** aur start mein `Server running` dikhe  

### Step B — Agar Resume ka button nahi hai

1. **Settings** → scroll → **Delete Web Service** (sirf Render wali copy delete)  
2. **New +** → **Web Service** (Blueprint dubara)  
3. Repo: `techwithaman-website`, branch `main`  
4. Build: `npm install`  
5. Start: `npm start`  
6. Health check: `/health`  
7. Add **PostgreSQL** (Render → New PostgreSQL) and set `DATABASE_URL` on the web service  

### Step C — Build fail ho to Logs dekho

Common fixes:

- **Build fail** → Build command `npm install` rakho, phir redeploy karo  
- **Port** → Start command `npm start` (Render `PORT` khud deta hai)  
- **Env** → `BASE_URL` = `https://YOUR-NAME.onrender.com` (apna exact URL)  

### Step D — Free plan limit

Free par **limited web services**. Purani unused services **Suspend/Delete** karo, phir nayi deploy karo.

## 1) Push code (already on GitHub)

Repo: **https://github.com/Amankushwaha2005/techwithaman-website**

## 2) Create Render service (one time)

1. Open **https://dashboard.render.com** → sign up / log in  
2. **New +** → **Blueprint**  
3. Connect **GitHub** → select repo `techwithaman-website`  
4. Render reads `render.yaml` → click **Apply**  
5. Wait for build (5–10 min first time). Status **Live** = deployed  

**Or manual Web Service:**

| Setting | Value |
|---------|--------|
| Runtime | Node |
| Build Command | `npm install` |
| Start Command | `npm start` |
| Health Check | `/health` |

## 3) Environment variables (Render → your service → Environment)

Add these (copy from local `.env` where needed):

| Key | Required | Notes |
|-----|----------|--------|
| `SESSION_SECRET` | Yes | Auto-generated if using Blueprint |
| `BASE_URL` | Yes | `https://YOUR-SERVICE.onrender.com` (no trailing slash) |
| `RAZORPAY_KEY_ID` | For payments | Live keys on production |
| `RAZORPAY_KEY_SECRET` | For payments | |
| `PAYMENT_ADVANCE_PERCENT` | Optional | Default `50` |
| `OPENAI_API_KEY` | For AI chat | Optional |
| `GOOGLE_CLIENT_ID` | For Google login | Optional |
| `GOOGLE_CLIENT_SECRET` | Optional | |
| `GOOGLE_REDIRECT_URI` | Optional | `https://YOUR-SERVICE.onrender.com/auth/google/callback` |
| `ADMIN_EMAILS` | **Yes for /admin** | Your login email, e.g. `you@gmail.com` (403 fix) |
| `DATABASE_URL` | Yes | From Render PostgreSQL (Internal URL) — auto if using `render.yaml` Blueprint |

After saving env vars → **Manual Deploy** → **Deploy latest commit**.

## 4) Razorpay / Google (production)

- **Razorpay:** Dashboard → use **Live** keys; allow your Render domain  
- **Google OAuth:** Authorized redirect URI = `https://YOUR-SERVICE.onrender.com/auth/google/callback`

## 5) PostgreSQL on Render

This app uses **PostgreSQL** (`DATABASE_URL`). Use Render **PostgreSQL** (free tier) or the `databases` block in `render.yaml` Blueprint. Do not use SQLite on production.

## 6) Custom domain (optional)

Render → service → **Settings** → **Custom Domains** → add `shrishatechnology.com` and update DNS.
