package com.shrishatechnology.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.shrishatechnology.web.ProjectPaths;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource(AppSettings settings, ProjectPaths paths) {
        HikariConfig cfg = new HikariConfig();
        cfg.setMaximumPoolSize(settings.dbPoolLimit());
        cfg.setInitializationFailTimeout(-1);
        cfg.setConnectionTimeout(10_000);

        if (settings.onRender() || postgresReachable(settings)) {
            cfg.setJdbcUrl(settings.jdbcUrl());
            cfg.setUsername(settings.jdbcUser());
            cfg.setPassword(settings.jdbcPassword());
            cfg.setDriverClassName("org.postgresql.Driver");
        } else {
            Path file = paths.root().resolve("data").resolve("shrisha-local");
            try {
                Files.createDirectories(file.getParent());
            } catch (Exception ignored) {
                // Hikari will surface a clear error if the folder cannot be created
            }
            String h2 = file.toAbsolutePath().toString().replace('\\', '/');
            cfg.setJdbcUrl("jdbc:h2:file:" + h2
                    + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
            cfg.setUsername("sa");
            cfg.setPassword("");
            cfg.setDriverClassName("org.h2.Driver");
            log.warn("PostgreSQL is not running — using local file database at {}", file + ".mv.db");
        }
        return new HikariDataSource(cfg);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private static boolean postgresReachable(AppSettings settings) {
        Properties props = new Properties();
        props.setProperty("user", settings.jdbcUser() == null ? "" : settings.jdbcUser());
        props.setProperty("password", settings.jdbcPassword() == null ? "" : settings.jdbcPassword());
        props.setProperty("loginTimeout", "2");
        String url = settings.jdbcUrl();
        url += url.contains("?") ? "&loginTimeout=2" : "?loginTimeout=2";
        try (Connection c = DriverManager.getConnection(url, props)) {
            return c.isValid(2);
        } catch (Exception e) {
            log.warn("PostgreSQL not reachable: {}", e.getMessage());
            return false;
        }
    }
}
