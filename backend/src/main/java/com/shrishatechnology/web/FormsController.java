package com.shrishatechnology.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

import com.shrishatechnology.service.RecaptchaService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class FormsController {

    private final RecaptchaService recaptcha;
    private final FormInbox inbox;

    public FormsController(RecaptchaService recaptcha, FormInbox inbox) {
        this.recaptcha = recaptcha;
        this.inbox = inbox;
    }

    @PostMapping("/contact")
    public RedirectView contact(HttpServletRequest req) {
        var captcha = recaptcha.validate(req);
        if (!captcha.ok()) {
            return new RedirectView("/contact?error=" + RecaptchaService.quote(captcha.error()));
        }
        String name = nv(req.getParameter("name"));
        String email = nv(req.getParameter("email")).toLowerCase();
        String phone = emptyToNull(nv(req.getParameter("phone")));
        String message = nv(req.getParameter("message"));
        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            return new RedirectView("/contact?error=" + RecaptchaService.quote("Please fill name, email, and message."));
        }
        try {
            inbox.saveContact(name, email, phone, message);
        } catch (Exception e) {
            return new RedirectView("/contact?error=" + RecaptchaService.quote("Could not send right now. Please try again."));
        }
        return new RedirectView("/contact?sent=1");
    }

    @PostMapping("/work")
    public RedirectView work(HttpServletRequest req) {
        var captcha = recaptcha.validate(req);
        if (!captcha.ok()) {
            return new RedirectView("/work?error=" + RecaptchaService.quote(captcha.error()));
        }
        String fullName = nv(req.getParameter("fullName"));
        String email = nv(req.getParameter("email")).toLowerCase();
        String phone = emptyToNull(nv(req.getParameter("phone")));
        String resume = emptyToNull(nv(req.getParameter("resume")));
        String category = nv(req.getParameter("category"));
        String skill = nv(req.getParameter("skill"));
        if (fullName.isEmpty() || email.isEmpty()) {
            return new RedirectView("/work?error=" + RecaptchaService.quote("Name and email are required."));
        }
        if (category.isEmpty() || skill.isEmpty()) {
            return new RedirectView("/work?error=" + RecaptchaService.quote("Please select category and role."));
        }
        try {
            inbox.saveWork(fullName, email, phone, resume, category + " — " + skill);
        } catch (Exception e) {
            return new RedirectView("/work?error=" + RecaptchaService.quote("Could not submit right now. Please try again."));
        }
        return new RedirectView("/work?sent=1");
    }

    private static String nv(String s) {
        return s == null ? "" : s.trim();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
