package com.ecommerse.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Repairs legacy product column types in PostgreSQL databases.
 * Some deployments have products.description/brand stored as BYTEA, which
 * breaks LOWER(...) filters used by product listing queries.
 */
@Component
@Order(1)
public class ProductSchemaRepairService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductSchemaRepairService.class);

    private final JdbcTemplate jdbcTemplate;

    public ProductSchemaRepairService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!isPostgreSql()) {
            return;
        }

        repairByteaColumnToText("description");
        repairByteaColumnToText("brand");
    }

    private boolean isPostgreSql() {
        try {
            String productName = jdbcTemplate.execute(
                    (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
            return productName != null
                    && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (Exception ex) {
            log.warn("Unable to determine database type for schema repair; skipping product schema repair", ex);
            return false;
        }
    }

    private void repairByteaColumnToText(String columnName) {
        String dataType = getColumnDataType(columnName);
        if (dataType == null) {
            return;
        }

        if (!"bytea".equalsIgnoreCase(dataType)) {
            return;
        }

        String sql = "ALTER TABLE products ALTER COLUMN " + columnName + " TYPE TEXT USING encode(" + columnName
                + ", 'escape')";
        jdbcTemplate.execute(sql);
        log.warn("Repaired products.{} from BYTEA to TEXT for query compatibility", columnName);
    }

    private String getColumnDataType(String columnName) {
        List<String> types = jdbcTemplate.queryForList(
                """
                        SELECT data_type
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'products'
                          AND column_name = ?
                        """,
                String.class,
                columnName);

        return types.isEmpty() ? null : types.get(0);
    }
}
