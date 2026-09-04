package com.shrishatechnology;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
public class Application {

    public static void main(String[] args) {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            envFile = Path.of("..", ".env");
        }
        loadDotEnv(envFile);
        SpringApplication.run(Application.class, args);
    }

    static void loadDotEnv(Path envFile) {
        if (System.getenv("RENDER") != null) {
            return;
        }
        if (!Files.isRegularFile(envFile)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (Exception ignored) {
            // Pages still boot without .env
        }
    }
}
