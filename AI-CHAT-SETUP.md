# Smart AI Chat — Shrisha Technology

## Features

- **Hindi + English + Hinglish** — casual messages samajhta hai
- **Conversation memory** — pehle ki baat yaad rakhta hai (e.g. pehle website bola, baad me "kitna lagega")
- **Clarifying questions** — vague message par details maangta hai
- **Without API key** — server par smart local rules (`src/services/ai-fallback.js`)
- **With API key** — OpenAI GPT (best, human-like)

## Setup (best quality)

1. https://platform.openai.com/api-keys — API key banao
2. `.env` file me:

```
OPENAI_API_KEY=sk-your-key
OPENAI_MODEL=gpt-4o-mini
```

3. Optional — **Groq** (free tier, fast): https://console.groq.com

```
OPENAI_API_KEY=gsk_...
OPENAI_API_BASE=https://api.groq.com/openai/v1
OPENAI_MODEL=llama-3.3-70b-versatile
```

4. Restart:

```bash
npm start
```

5. Open **http://127.0.0.1:3000** → **AI** button

Status bar me **"Smart AI · Hindi/English samajhta hai"** dikhe = full AI on.

## Run site correctly

- Use `npm start` + **http://127.0.0.1:3000**
- Do not open `index.html` as file (offline mode is weaker)

## Edit AI knowledge

`src/config/site-knowledge.js` — prices, services, contact.

## Advanced tuning

`src/services/ai-assistant.js` — system prompt, temperature  
`src/services/ai-fallback.js` — offline/smart local intents
