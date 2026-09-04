package com.shrishatechnology.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishatechnology.db.Db;
import com.shrishatechnology.db.SchemaMigrator;

@Component
public class FormInbox {

    private static final Logger log = LoggerFactory.getLogger(FormInbox.class);

    private final Db db;
    private final SchemaMigrator schema;
    private final ObjectMapper mapper;
    private final Path fallbackFile;

    public FormInbox(Db db, SchemaMigrator schema, ObjectMapper mapper, ProjectPaths paths) {
        this.db = db;
        this.schema = schema;
        this.mapper = mapper;
        this.fallbackFile = paths.root().resolve("data").resolve("form-submissions.jsonl");
    }

    public void saveContact(String name, String email, String phone, String message) {
        if (schema.isAvailable()) {
            db.execute(
                    "INSERT INTO contact_submissions (name, email, phone, message) VALUES (?, ?, ?, ?)",
                    name, email, phone, message
            );
            return;
        }
        writeFallback("contact", Map.of(
                "name", name,
                "email", email,
                "phone", phone == null ? "" : phone,
                "message", message
        ));
    }

    public void saveWork(String fullName, String email, String phone, String resume, String skill) {
        if (schema.isAvailable()) {
            db.execute(
                    "INSERT INTO work_submissions (full_name, email, phone, resume, skill) VALUES (?, ?, ?, ?, ?)",
                    fullName, email, phone, resume, skill
            );
            return;
        }
        writeFallback("work", Map.of(
                "fullName", fullName,
                "email", email,
                "phone", phone == null ? "" : phone,
                "resume", resume == null ? "" : resume,
                "skill", skill
        ));
    }

    private void writeFallback(String type, Map<String, String> fields) {
        try {
            Files.createDirectories(fallbackFile.getParent());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", type);
            row.put("at", Instant.now().toString());
            row.putAll(fields);
            String line = mapper.writeValueAsString(row) + System.lineSeparator();
            Files.writeString(fallbackFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.warn("PostgreSQL unavailable — saved {} form to {}", type, fallbackFile);
        } catch (Exception e) {
            throw new IllegalStateException("Could not save the form. Please try again.", e);
        }
    }
}
