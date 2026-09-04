package com.shrishatechnology.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class CampaignsController {

    private final AdminGuard guard;
    private final CampaignStore store;
    private final TemplateRenderer templates;

    public CampaignsController(AdminGuard guard, CampaignStore store, TemplateRenderer templates) {
        this.guard = guard;
        this.store = store;
        this.templates = templates;
    }

    @GetMapping("/admin/campaigns")
    public Object page(
            HttpServletRequest req,
            HttpSession session,
            @RequestParam(required = false) String flash,
            @RequestParam(required = false) String err
    ) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("title", "Campaigns | " + Site.BRAND);
        ctx.put("brand", Site.BRAND);
        ctx.put("authUser", session.getAttribute("user"));
        ctx.put("adminNav", "campaigns");
        ctx.put("campaigns", store.listAll());
        ctx.put("flash", flash == null ? "" : flash);
        ctx.put("err", err == null ? "" : err);
        return templates.render("admin/campaigns.html", ctx);
    }

    @PostMapping("/admin/campaigns")
    public Object create(
            HttpServletRequest req,
            HttpSession session,
            @RequestParam String kind,
            @RequestParam String title,
            @RequestParam(required = false) String body,
            @RequestParam(name = "cta_label", required = false) String ctaLabel,
            @RequestParam(name = "cta_url", required = false) String ctaUrl,
            @RequestParam String placement,
            @RequestParam(required = false) String active,
            @RequestParam(name = "starts_on", required = false) String startsOn,
            @RequestParam(name = "ends_on", required = false) String endsOn,
            @RequestParam(name = "media_url", required = false) String mediaUrl,
            @RequestParam(required = false) MultipartFile file
    ) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        try {
            store.save(kind, title, body, ctaLabel, ctaUrl, placement, active != null, startsOn, endsOn, mediaUrl, file);
            return new RedirectView("/admin/campaigns?flash=saved");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Could not save campaign." : e.getMessage();
            return new RedirectView("/admin/campaigns?err=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/admin/campaigns/{id}/toggle")
    public Object toggle(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        store.toggle(id);
        return new RedirectView("/admin/campaigns?flash=toggled");
    }

    @PostMapping("/admin/campaigns/{id}/delete")
    public Object delete(HttpServletRequest req, HttpSession session, @PathVariable long id) {
        Object denied = guard.denyIfNotAdmin(req, session);
        if (denied != null) {
            return denied;
        }
        store.delete(id);
        return new RedirectView("/admin/campaigns?flash=deleted");
    }
}
