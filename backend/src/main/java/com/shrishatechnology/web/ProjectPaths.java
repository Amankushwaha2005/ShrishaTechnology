package com.shrishatechnology.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

@Component
public class ProjectPaths {

    private final Path root;

    public ProjectPaths() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(cwd.resolve("templates"))) {
            this.root = cwd;
        } else if (cwd.getParent() != null && Files.isDirectory(cwd.getParent().resolve("templates"))) {
            this.root = cwd.getParent();
        } else {
            this.root = cwd;
        }
    }

    public Path root() {
        return root;
    }
}
