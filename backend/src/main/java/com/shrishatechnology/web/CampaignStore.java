package com.shrishatechnology.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shrishatechnology.db.Db;
import com.shrishatechnology.db.SchemaMigrator;

@Service
public class CampaignStore {

    private static final Pattern YT = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{6,})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm");
    private static final Set<String> KINDS = Set.of("offer", "ad", "photo", "video");
    private static final Set<String> PLACEMENTS = Set.of("ticker", "popup", "gallery");

    private final Db db;
    private final SchemaMigrator schema;
    private final Path uploadDir;

    public CampaignStore(Db db, SchemaMigrator schema, ProjectPaths paths) {
        this.db = db;
        this.schema = schema;
        this.uploadDir = paths.root().resolve("data").resolve("uploads");
    }

    public void attachPublic(Map<String, Object> ctx) {
        List<Map<String, Object>> live = listLive();
        Map<String, Object> ticker = null;
        Map<String, Object> popup = null;
        List<Map<String, Object>> gallery = new ArrayList<>();
        for (Map<String, Object> row : live) {
            String place = str(row.get("placement"));
            if (ticker == null && "ticker".equals(place)) {
                ticker = row;
            } else if (popup == null && "popup".equals(place)) {
                popup = row;
            } else if ("gallery".equals(place)) {
                gallery.add(row);
            }
        }
        ctx.put("promoTicker", ticker);
        ctx.put("promoPopup", popup);
        ctx.put("promoGallery", gallery);
        ctx.put("hasPromoGallery", !gallery.isEmpty());
    }

    public List<Map<String, Object>> listAll() {
        if (!schema.isAvailable()) {
            return List.of();
        }
        try {
            return enrichAll(db.query("SELECT * FROM site_campaigns ORDER BY sort_order ASC, id DESC"));
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> listLive() {
        List<Map<String, Object>> live = new ArrayList<>();
        Instant now = Instant.now();
        for (Map<String, Object> row : listAll()) {
            if (!isOn(row.get("active"))) {
                continue;
            }
            Instant start = toInstant(row.get("starts_at"));
            Instant end = toInstant(row.get("ends_at"));
            if (start != null && start.isAfter(now)) {
                continue;
            }
            if (end != null && end.isBefore(now)) {
                continue;
            }
            live.add(row);
        }
        return live;
    }

    public Map<String, Object> save(
            String kind,
            String title,
            String body,
            String ctaLabel,
            String ctaUrl,
            String placement,
            boolean active,
            String startsOn,
            String endsOn,
            String mediaUrl,
            MultipartFile file
    ) throws IOException {
        kind = normalize(kind, KINDS, "offer");
        placement = normalize(placement, PLACEMENTS, "ticker");
        title = title == null ? "" : title.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Title is required.");
        }
        String storedMedia = blankToNull(mediaUrl);
        String mediaKind = detectMediaKind(storedMedia, kind);
        if (file != null && !file.isEmpty()) {
            storedMedia = saveUpload(file);
            mediaKind = videoExt(file.getOriginalFilename()) ? "video" : "image";
        }
        if (storedMedia != null && youtubeId(storedMedia) != null) {
            mediaKind = "youtube";
        }
        Timestamp start = parseDate(startsOn, false);
        Timestamp end = parseDate(endsOn, true);
        db.execute(
                """
                INSERT INTO site_campaigns
                  (kind, title, body, cta_label, cta_url, media_url, media_kind, placement, active, starts_at, ends_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                kind,
                title,
                blankToNull(body),
                blankToNull(ctaLabel),
                blankToNull(ctaUrl),
                storedMedia,
                mediaKind,
                placement,
                active ? 1 : 0,
                start,
                end
        );
        return Map.of("ok", true);
    }

    public void toggle(long id) {
        Map<String, Object> row = db.queryOne("SELECT active FROM site_campaigns WHERE id = ?", id);
        if (row == null) {
            return;
        }
        int next = isOn(row.get("active")) ? 0 : 1;
        db.execute("UPDATE site_campaigns SET active = ? WHERE id = ?", next, id);
    }

    public void delete(long id) {
        Map<String, Object> row = db.queryOne("SELECT media_url FROM site_campaigns WHERE id = ?", id);
        db.execute("DELETE FROM site_campaigns WHERE id = ?", id);
        if (row != null) {
            deleteLocalFile(str(row.get("media_url")));
        }
    }

    private String saveUpload(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = ext(original);
        if (!IMAGE_EXT.contains(ext) && !VIDEO_EXT.contains(ext)) {
            throw new IllegalArgumentException("Upload a JPG, PNG, WEBP, GIF, MP4 or WEBM file.");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new IllegalArgumentException("File must be 20 MB or smaller.");
        }
        Files.createDirectories(uploadDir);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dest = uploadDir.resolve(name);
        file.transferTo(dest.toFile());
        return "/uploads/" + name;
    }

    private void deleteLocalFile(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        String name = url.substring("/uploads/".length());
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(name));
        } catch (Exception ignored) {
            // keep going even if the file is already gone
        }
    }

    private List<Map<String, Object>> enrichAll(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> c = new LinkedHashMap<>(row);
            String kind = str(row.get("kind"));
            String media = str(row.get("media_url"));
            String mediaKind = str(row.get("media_kind"));
            String yt = youtubeId(media);
            if (yt != null) {
                mediaKind = "youtube";
            }
            c.put("kindLabel", kindLabel(kind));
            c.put("placementLabel", placementLabel(str(row.get("placement"))));
            c.put("isActive", isOn(row.get("active")));
            c.put("isYoutube", yt != null);
            c.put("embedUrl", yt == null ? "" : "https://www.youtube.com/embed/" + yt);
            c.put("isVideoFile", "video".equals(mediaKind));
            c.put("isImage", "image".equals(mediaKind) || (yt == null && !media.isBlank() && !videoExt(media)));
            c.put("hasMedia", !media.isBlank() || yt != null);
            c.put("thumbUrl", yt == null ? media : "https://img.youtube.com/vi/" + yt + "/hqdefault.jpg");
            out.add(c);
        }
        return out;
    }

    private static String kindLabel(String kind) {
        return switch (kind) {
            case "offer" -> "Festival offer";
            case "ad" -> "Ad";
            case "photo" -> "Photo";
            case "video" -> "Video";
            default -> kind;
        };
    }

    private static String placementLabel(String placement) {
        return switch (placement) {
            case "ticker" -> "Top bar";
            case "popup" -> "Popup";
            case "gallery" -> "Home gallery";
            default -> placement;
        };
    }

    private static String detectMediaKind(String url, String kind) {
        if (url == null) {
            return "photo".equals(kind) ? "image" : ("video".equals(kind) ? "video" : "image");
        }
        if (youtubeId(url) != null) {
            return "youtube";
        }
        return videoExt(url) ? "video" : "image";
    }

    private static String youtubeId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher m = YT.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private static boolean videoExt(String name) {
        return VIDEO_EXT.contains(ext(name));
    }

    private static String ext(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value, Set<String> allowed, String fallback) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return allowed.contains(v) ? v : fallback;
    }

    private static boolean isOn(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
    }

    private static Timestamp parseDate(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(raw.trim());
            LocalDateTime ldt = endOfDay ? d.atTime(23, 59, 59) : d.atStartOfDay();
            return Timestamp.valueOf(ldt);
        } catch (Exception e) {
            return null;
        }
    }

    private static Instant toInstant(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (v instanceof Instant inst) {
            return inst;
        }
        if (v instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (v instanceof LocalDateTime ldt) {
            return Timestamp.valueOf(ldt).toInstant();
        }
        return null;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
