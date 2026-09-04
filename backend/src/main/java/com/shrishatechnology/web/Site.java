package com.shrishatechnology.web;

import java.util.List;
import java.util.Map;

public final class Site {
    public static final String BRAND = "Shrisha Technology";

    public static final Map<String, String> COMPANY = Map.of(
            "name", BRAND,
            "tagline", "Websites · Apps · SEO & GEO · Student Projects",
            "email", "mayankklush2006@gmail.com",
            "phone", "+91 79920 20591"
    );

    public static final List<Map<String, String>> NAV = List.of(
            Map.of("href", "/", "label", "Home", "key", "home"),
            Map.of("href", "/#track", "label", "Track", "key", "track"),
            Map.of("href", "/pricing", "label", "Pricing", "key", "pricing"),
            Map.of("href", "/services", "label", "Services", "key", "services"),
            Map.of("href", "/portfolio", "label", "Portfolio", "key", "portfolio"),
            Map.of("href", "/news", "label", "News", "key", "news"),
            Map.of("href", "/about", "label", "About", "key", "about"),
            Map.of("href", "/contact", "label", "Contact", "key", "contact")
    );

    private Site() {}
}
