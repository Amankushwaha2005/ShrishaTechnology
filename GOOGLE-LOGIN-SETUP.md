# Google (Gmail) Login — Setup

Login & signup pages use **Google OAuth**. Without keys you will see: *"Google login is not configured"*.

## Step 1 — Google Cloud Console

1. Open https://console.cloud.google.com/
2. Create or select a project
3. **APIs & Services** → **OAuth consent screen**
   - User type: **External** (or Internal for Workspace)
   - App name: `Shrisha Technology`
   - Add your email as developer / test user if app is in **Testing**
4. **APIs & Services** → **Credentials** → **Create Credentials** → **OAuth client ID**
5. Application type: **Web application**
6. **Authorized JavaScript origins** (add both):
   - `https://techwithaman-website-2026.onrender.com`
   - `http://localhost:3000` (local dev)
7. **Authorized redirect URIs** (must match exactly, then click **Save**):
   - `https://techwithaman-website-2026.onrender.com/auth/google/callback`
   - `http://localhost:3000/auth/google/callback` (local dev)
8. Copy **Client ID** and **Client secret**

## Step 2 — Render Environment

1. https://dashboard.render.com → **techwithaman-website-2026** → **Environment**
2. Add:

| Key | Value |
|-----|--------|
| `GOOGLE_CLIENT_ID` | `xxxxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | your secret |
| `GOOGLE_REDIRECT_URI` | `https://techwithaman-website-2026.onrender.com/auth/google/callback` |
| `BASE_URL` | `https://techwithaman-website-2026.onrender.com` |

3. **Save** → **Manual Deploy**

## Step 3 — Local `.env` (optional)

```env
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-secret
GOOGLE_REDIRECT_URI=http://localhost:3000/auth/google/callback
BASE_URL=http://localhost:3000
```

## Step 4 — Test

1. Open https://techwithaman-website-2026.onrender.com/login
2. Click **Continue with Google (Gmail)**
3. Pick your Google account → you should land on home, logged in

Check: https://techwithaman-website-2026.onrender.com/api/site-info  
Should show `"googleLoginEnabled": true`

## Common errors

| Error | Fix |
|-------|-----|
| `redirect_uri_mismatch` | Redirect URI in Google Console must **exactly** match `http://localhost:3000/auth/google/callback` then **Save** |
| `deleted_client` | OAuth client was deleted — create a new Web client and update `.env` |
| `access_denied` | Add your Gmail under OAuth consent screen → **Test users** |
| Still "not configured" | Redeploy after saving env vars on Render |
