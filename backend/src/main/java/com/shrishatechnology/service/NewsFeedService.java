package com.shrishatechnology.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class NewsFeedService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final long CACHE_MS = 10 * 60 * 1000;

    private static final List<Feed> FEEDS = List.of(
            new Feed("Top", "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en"),
            new Feed("India", "https://news.google.com/rss/headlines/section/topic/NATION?hl=en-IN&gl=IN&ceid=IN:en"),
            new Feed("World", "https://news.google.com/rss/headlines/section/topic/WORLD?hl=en-IN&gl=IN&ceid=IN:en"),
            new Feed("Business", "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en-IN&gl=IN&ceid=IN:en"),
            new Feed("Technology", "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-IN&gl=IN&ceid=IN:en"),
            new Feed("Sports", "https://news.google.com/rss/headlines/section/topic/SPORTS?hl=en-IN&gl=IN&ceid=IN:en"),
            new Feed("Entertainment", "https://news.google.com/rss/headlines/section/topic/ENTERTAINMENT?hl=en-IN&gl=IN&ceid=IN:en")
    );

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile Map<String, Object> cache;
    private volatile long cacheAt;

    public Map<String, Object> latest() {
        long now = System.currentTimeMillis();
        Map<String, Object> hit = cache;
        if (hit != null && now - cacheAt < CACHE_MS) {
            return hit;
        }
        synchronized (this) {
            if (cache != null && System.currentTimeMillis() - cacheAt < CACHE_MS) {
                return cache;
            }
            Map<String, Object> fresh = fetchAll();
            cache = fresh;
            cacheAt = System.currentTimeMillis();
            return fresh;
        }
    }

    private Map<String, Object> fetchAll() {
        List<Map<String, Object>> articles = new ArrayList<>();
        LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();

        for (Feed feed : FEEDS) {
            try {
                for (Map<String, Object> item : parseFeed(feed)) {
                    String key = String.valueOf(item.get("title")).toLowerCase(Locale.ROOT);
                    if (key.isBlank() || seen.containsKey(key)) {
                        continue;
                    }
                    seen.put(key, true);
                    articles.add(item);
                }
            } catch (Exception ignored) {
                // other feeds still used
            }
        }

        Instant cutoff = Instant.now().minus(Duration.ofHours(36));
        List<Map<String, Object>> recent = articles.stream()
                .filter(a -> {
                    Instant p = (Instant) a.get("_published");
                    return p == null || !p.isBefore(cutoff);
                })
                .toList();
        if (recent.size() >= 8) {
            articles = new ArrayList<>(recent);
        }

        articles.sort(Comparator.comparing((Map<String, Object> a) -> (Instant) a.getOrDefault("_published", Instant.EPOCH)).reversed());
        if (articles.size() > 48) {
            articles = new ArrayList<>(articles.subList(0, 48));
        }
        if (!articles.isEmpty()) {
            articles.get(0).put("featured", true);
        }

        List<String> ticker = articles.stream()
                .limit(12)
                .map(a -> String.valueOf(a.get("title")))
                .toList();

        List<Map<String, Object>> publicArticles = new ArrayList<>();
        for (Map<String, Object> a : articles) {
            Map<String, Object> copy = new LinkedHashMap<>(a);
            copy.remove("_published");
            publicArticles.add(copy);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", !publicArticles.isEmpty());
        out.put("updatedAt", ZonedDateTime.now(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        out.put("ticker", ticker);
        out.put("articles", publicArticles);
        return out;
    }

    private List<Map<String, Object>> parseFeed(Feed feed) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(feed.url()))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .GET()
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400 || resp.body() == null || resp.body().length == 0) {
            return List.of();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ignored) {
        }
        Document doc = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(resp.body()));
        NodeList items = doc.getElementsByTagName("item");
        List<Map<String, Object>> list = new ArrayList<>();
        int max = Math.min(items.getLength(), 12);
        for (int i = 0; i < max; i++) {
            Element el = (Element) items.item(i);
            String title = text(el, "title");
            String link = text(el, "link");
            String pub = text(el, "pubDate");
            String desc = stripHtml(text(el, "description"));
            String source = text(el, "source");
            if (title.isBlank() || link.isBlank()) {
                continue;
            }
            Instant published = parseDate(pub);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", Integer.toHexString((title + link).hashCode()));
            row.put("category", feed.category());
            row.put("title", cleanTitle(title));
            row.put("excerpt", excerpt(desc, title));
            row.put("url", link);
            row.put("date", published.atZone(IST).toLocalDate().toString());
            row.put("publishedAt", published.toString());
            row.put("author", source.isBlank() ? "Google News" : source);
            row.put("readMins", 1);
            row.put("featured", false);
            row.put("_published", published);
            list.add(row);
        }
        return list;
    }

    private static Instant parseDate(String pub) {
        if (pub == null || pub.isBlank()) {
            return Instant.now();
        }
        try {
            return ZonedDateTime.parse(pub, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static String text(Element parent, String tag) {
        NodeList n = parent.getElementsByTagName(tag);
        if (n.getLength() == 0 || n.item(0) == null) {
            return "";
        }
        return n.item(0).getTextContent() == null ? "" : n.item(0).getTextContent().trim();
    }

    private static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String excerpt(String desc, String title) {
        if (desc.isBlank() || desc.equalsIgnoreCase(title)) {
            return "Tap Read to open the full story from the publisher.";
        }
        return desc.length() > 220 ? desc.substring(0, 217) + "…" : desc;
    }

    private static String cleanTitle(String title) {
        int dash = title.lastIndexOf(" - ");
        if (dash > 20) {
            return title.substring(0, dash).trim();
        }
        return title;
    }

    private record Feed(String category, String url) {}
}
