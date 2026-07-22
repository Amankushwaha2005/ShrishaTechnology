/**
 * =============================================================================
 * BACKEND — AI assistant service
 * File: src/services/ai-assistant.js
 * OpenAI/Groq chat completion calls
 * =============================================================================
 */

const { SITE_KNOWLEDGE } = require("../config/site-knowledge");
const { fallbackReply } = require("./ai-fallback");

const SYSTEM_PROMPT = `You are the friendly AI assistant for #TechWithAman (India) — a web agency for websites, apps, logo design, and student projects.

${SITE_KNOWLEDGE}

BEHAVIOUR:
- Understand Hindi, English, Hinglish, slang, typos, and short messages like real humans write.
- Use the full conversation history — if user said they need a website earlier and now asks "kitna lagega", answer about website pricing/timeline.
- Reply in the same language mix the user uses (Hinglish is fine).
- Be warm, clear, and practical — like a helpful sales advisor, not a robot.
- If the request is vague, ask 1–2 short clarifying questions (business type, pages, deadline, budget).
- Never invent prices not in the knowledge base; use "from ₹X" or say to check /pricing or WhatsApp for exact quote.
- For human handoff: WhatsApp +91 78170 85910, email mayankklush2006@gmail.com, /contact form.
- Keep answers concise (2–5 short paragraphs max unless user asks for detail).
- Do not claim to be a human; you are TechWithAman's AI assistant.`;

async function openAiReply(message, history) {
  const apiKey = process.env.OPENAI_API_KEY?.trim();
  if (!apiKey) return null;

  const model = process.env.OPENAI_MODEL?.trim() || "gpt-4o-mini";
  const base = (process.env.OPENAI_API_BASE || "https://api.openai.com/v1").replace(/\/$/, "");

  const messages = [
    { role: "system", content: SYSTEM_PROMPT },
    ...history.slice(-12).map((h) => ({
      role: h.role === "user" ? "user" : "assistant",
      content: String(h.content || "").slice(0, 2000),
    })),
    { role: "user", content: String(message).slice(0, 2000) },
  ];

  const res = await fetch(`${base}/chat/completions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      messages,
      max_tokens: 700,
      temperature: 0.75,
      presence_penalty: 0.1,
      frequency_penalty: 0.2,
    }),
  });

  if (!res.ok) {
    const errText = await res.text().catch(() => "");
    console.error("[ai] API error", res.status, errText.slice(0, 300));
    return null;
  }

  const data = await res.json();
  return data?.choices?.[0]?.message?.content?.trim() || null;
}

async function getAssistantReply(message, history = []) {
  const hasKey = !!process.env.OPENAI_API_KEY?.trim();

  if (hasKey) {
    try {
      const ai = await openAiReply(message, history);
      if (ai) return { reply: ai, mode: "openai" };
    } catch (err) {
      console.error("[ai] Request failed", err.message);
    }
  }

  return { reply: fallbackReply(message, history), mode: hasKey ? "local-fallback" : "local" };
}

module.exports = { getAssistantReply, fallbackReply };
