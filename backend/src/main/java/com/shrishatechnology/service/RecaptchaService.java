package com.shrishatechnology.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishatechnology.config.AppSettings;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class RecaptchaService {

    public record Result(boolean ok, String error) {}

    private final AppSettings settings;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public RecaptchaService(AppSettings settings, ObjectMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
    }

    public Result validate(HttpServletRequest request) {
        if (settings.recaptchaSiteKey() != null && settings.recaptchaSecretKey() != null) {
            String token = nv(request.getParameter("g-recaptcha-response"));
            if (token.isEmpty()) {
                return new Result(false, "Please complete the reCAPTCHA.");
            }
            try {
                String body = "secret=" + enc(settings.recaptchaSecretKey()) + "&response=" + enc(token);
                HttpRequest req = HttpRequest.newBuilder(URI.create("https://www.google.com/recaptcha/api/siteverify"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonNode data = mapper.readTree(resp.body());
                if (data.path("success").asBoolean(false)) {
                    return new Result(true, "");
                }
                return new Result(false, "reCAPTCHA failed. Please try again.");
            } catch (Exception e) {
                return new Result(false, "reCAPTCHA check failed. Please try again.");
            }
        }
        return new Result(false, "Please complete the Google reCAPTCHA.");
    }

    private static String nv(String s) {
        return s == null ? "" : s.trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static String quote(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
