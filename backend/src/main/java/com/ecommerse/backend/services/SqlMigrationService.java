package com.ecommerse.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Executes SQL migrations from classpath:db/migrations/*.sql exactly once per database.
 *
 * Why this exists:
 * - Heroku deploy currently uses backend subtree, so root-level SQL files are not deployed.
 * - This runner ensures new SQL migrations are bundled with backend and applied automatically.
 */
@Component
@Order(0)
public class SqlMigrationService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlMigrationService.class);
    private static final String MIGRATION_TABLE = "app_sql_migrations";
    private static final String MIGRATION_LOCATION = "classpath*:db/migrations/*.sql";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ResourcePatternResolver resourceResolver;

    public SqlMigrationService(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.resourceResolver = new PathMatchingResourcePatternResolver();
    }

    @Override
    public void run(String... args) {
        if (!isPostgreSql()) {
            log.info("Skipping SQL migrations because database is not PostgreSQL");
            return;
        }

        withMigrationLock(() -> {
            ensureMigrationTable();
            applyMigrations();
        });
    }

    private boolean isPostgreSql() {
        try {
            String productName = jdbcTemplate.execute(
                    (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
            return productName != null
                    && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (Exception ex) {
            log.warn("Unable to determine database type for SQL migration step; skipping", ex);
            return false;
        }
    }

    private void ensureMigrationTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_sql_migrations (
                    script_name VARCHAR(255) PRIMARY KEY,
                    checksum VARCHAR(64) NOT NULL,
                    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private void applyMigrations() {
        try {
            Resource[] resources = resourceResolver.getResources(MIGRATION_LOCATION);
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null) {
                    continue;
                }

                String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                String checksum = sha256(sql);
                String existingChecksum = getAppliedChecksum(fileName);

                if (existingChecksum != null) {
                    if (!existingChecksum.equals(checksum)) {
                        throw new IllegalStateException("Migration file changed after apply: " + fileName
                                + ". Create a new migration file instead of editing old ones.");
                    }
                    log.debug("Skipping already-applied migration {}", fileName);
                    continue;
                }

                log.info("Applying SQL migration {}", fileName);
                try (var connection = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(connection, resource);
                }
                jdbcTemplate.update(
                        "INSERT INTO " + MIGRATION_TABLE + " (script_name, checksum, applied_at) VALUES (?, ?, ?)",
                        fileName,
                        checksum,
                        OffsetDateTime.now());
                log.info("Applied SQL migration {}", fileName);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed applying SQL migrations from " + MIGRATION_LOCATION, ex);
        }
    }

    private void withMigrationLock(Runnable migrationWork) {
        // Prevent concurrent dynos/processes from applying the same migration at startup.
        jdbcTemplate.execute("SELECT pg_advisory_lock(hashtext('app_sql_migrations_lock'))");
        try {
            migrationWork.run();
        } finally {
            jdbcTemplate.execute("SELECT pg_advisory_unlock(hashtext('app_sql_migrations_lock'))");
        }
    }

    private String getAppliedChecksum(String fileName) {
        return jdbcTemplate.query(
                "SELECT checksum FROM " + MIGRATION_TABLE + " WHERE script_name = ?",
                ps -> ps.setString(1, fileName),
                rs -> rs.next() ? rs.getString(1) : null);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash migration content", ex);
        }
    }
}
