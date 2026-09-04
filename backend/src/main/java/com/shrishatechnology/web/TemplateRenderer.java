package com.shrishatechnology.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.loader.FileLocator;

@Service
public class TemplateRenderer {

    private final Path templatesDir;
    private final Jinjava jinjava;

    public TemplateRenderer(ProjectPaths paths) {
        this.templatesDir = paths.root().resolve("templates");
        JinjavaConfig cfg = JinjavaConfig.newBuilder()
                .withLstripBlocks(true)
                .withTrimBlocks(true)
                .build();
        this.jinjava = new Jinjava(cfg);
        try {
            this.jinjava.setResourceLocator(new FileLocator(templatesDir.toFile()));
        } catch (java.io.FileNotFoundException e) {
            throw new IllegalStateException("templates folder not found: " + templatesDir, e);
        }
    }

    public ResponseEntity<String> render(String template, Map<String, Object> context) {
        try {
            Path file = templatesDir.resolve(template);
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Context ctx = new Context();
            if (context != null) {
                context.forEach(ctx::put);
            }
            String html = jinjava.render(source, ctx);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Template error: " + e.getMessage());
        }
    }
}
