package com.shrishatechnology.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shrishatechnology.service.NewsFeedService;

@RestController
public class NewsApiController {

    private final NewsFeedService news;

    public NewsApiController(NewsFeedService news) {
        this.news = news;
    }

    @GetMapping("/api/news")
    public Map<String, Object> news() {
        try {
            return news.latest();
        } catch (Exception e) {
            return Map.of("ok", false, "error", "Could not load live news right now.", "ticker", java.util.List.of(), "articles", java.util.List.of());
        }
    }
}
