package com.bqsummer.util;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.util.*;

/**
 * Lightweight DB assertion helper for tests.
 *
 * Features:
 * - Uses the current spring.datasource.* configuration (system properties first, then classpath application.properties).
 * - Simple, dependency-free JDBC with prepared statements.
 * - Common assertions: exists, notExists, rowCount, count(where), column value equals, fetchOne.
 *
 * Usage examples:
 *   DbAssert.assertExists("users", "email = ?", "test@example.com");
 *   DbAssert.assertRowCount("users", 5);
 *   DbAssert.assertCount("orders", "status = ?", 3, "PAID");
 *   DbAssert.assertValue("users", "username", "alice", "id = ?", 1);
 *   Map<String, Object> row = DbAssert.fetchOne("users", "id = ?", 1);
 */
public final class DbAssert {

    private static volatile ConnectionHolder HOLDER;

    private DbAssert() {}

    // ============ Public assertion APIs ============

    public static void assertExists(String table, String whereClause, Object... params) {
        long count = count(table, whereClause, params);
        Assertions.assertTrue(count > 0, () -> String.format("Expected rows to exist in %s where (%s), but found 0", table, whereClause));
    }

    public static void assertNotExists(String table, String whereClause, Object... params) {
        long count = count(table, whereClause, params);
        Assertions.assertEquals(0L, count, () -> String.format("Expected no rows in %s where (%s), but found %d", table, whereClause, count));
    }

    public static void assertRowCount(String table, long expected) {
        long cnt = count(table, null);
        Assertions.assertEquals(expected, cnt, () -> String.format("Row count mismatch for %s, expected %d, actual %d", table, expected, cnt));
    }

    public static void assertCount(String table, String whereClause, long expected, Object... params) {
        long cnt = count(table, whereClause, params);
        Assertions.assertEquals(expected, cnt, () -> String.format("Count mismatch for %s where (%s), expected %d, actual %d", table, whereClause, expected, cnt));
    }

    public static void assertValue(String table, String column, Object expected, String whereClause, Object... params) {
        String sql = buildSelectOneSql(table, List.of(column), whereClause);
        Object value = queryForSingleColumn(sql, params);
        Assertions.assertEquals(normalize(expected), normalize(value), () -> String.format("Column value mismatch: %s.%s expected=%s actual=%s where (%s)", table, column, expected, value, whereClause));
    }

    public static Map<String, Object> fetchOne(String table, String whereClause, Object... params) {
        String sql = buildSelectOneSql(table, List.of("*"), whereClause);
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Collections.emptyMap();
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String name = md.getColumnLabel(i);
                    Object val = rs.getObject(i);
                    row.put(name, val);
                }
                // ensure only one row
                if (rs.next()) {
                    Assertions.fail("fetchOne returned more than one row for: " + table + " where (" + whereClause + ")");
                }
                return row;
            }
        } catch (SQLException e) {
            throw new RuntimeException("fetchOne failed: " + e.getMessage(), e);
        }
    }

    public static void truncate(String table) {
        String sql = "TRUNCATE TABLE " + table;
        try (Statement st = getConnection().createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("truncate failed: " + e.getMessage(), e);
        }
    }

    // ============ Await (polling) helpers for eventual consistency ============

    public static void awaitExists(String table, String whereClause, long timeoutMillis, long pollIntervalMillis, Object... params) {
        await(() -> count(table, whereClause, params) > 0, timeoutMillis, pollIntervalMillis,
                () -> String.format("Timeout waiting for rows to exist in %s where (%s)", table, whereClause));
    }

    public static void awaitNotExists(String table, String whereClause, long timeoutMillis, long pollIntervalMillis, Object... params) {
        await(() -> count(table, whereClause, params) == 0, timeoutMillis, pollIntervalMillis,
                () -> String.format("Timeout waiting for rows to disappear from %s where (%s)", table, whereClause));
    }

    public static void awaitCount(String table, String whereClause, long expected, long timeoutMillis, long pollIntervalMillis, Object... params) {
        await(() -> count(table, whereClause, params) == expected, timeoutMillis, pollIntervalMillis,
                () -> String.format("Timeout waiting for count=%d in %s where (%s)", expected, table, whereClause));
    }

    // ============ Internal helpers ============

    private static Object normalize(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) {
            // normalize numeric comparison (e.g., Integer vs Long vs BigDecimal)
            return new java.math.BigDecimal(n.toString());
        }
        if (o instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (o instanceof java.sql.Time t) {
            return t.toLocalTime();
        }
        return o;
    }

    private static long count(String table, String whereClause, Object... params) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(1) FROM ").append(table);
        if (whereClause != null && !whereClause.isBlank()) {
            sb.append(" WHERE ").append(whereClause);
        }
        try (PreparedStatement ps = getConnection().prepareStatement(sb.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("count failed: " + e.getMessage(), e);
        }
    }

    private static Object queryForSingleColumn(String sql, Object... params) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Object value = rs.getObject(1);
                if (rs.next()) {
                    Assertions.fail("Expected single row for sql: " + sql);
                }
                return value;
            }
        } catch (SQLException e) {
            throw new RuntimeException("queryForSingleColumn failed: " + e.getMessage(), e);
        }
    }

    private static String buildSelectOneSql(String table, List<String> columns, String whereClause) {
        String cols = String.join(", ", columns);
        StringBuilder sb = new StringBuilder("SELECT ").append(cols).append(" FROM ").append(table);
        if (whereClause != null && !whereClause.isBlank()) {
            sb.append(" WHERE ").append(whereClause);
        }
        return sb.toString();
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static synchronized Connection getConnection() {
        if (HOLDER == null || !HOLDER.isValid()) {
            HOLDER = ConnectionHolder.create(loadSettings());
        }
        return HOLDER.connection;
    }

    private static void await(BooleanSupplier condition, long timeoutMillis, long pollIntervalMillis, MessageSupplier onTimeoutMessage) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (condition.getAsBoolean()) return;
            } catch (RuntimeException ignored) {
                // keep polling in case of transient failures
            }
            try {
                Thread.sleep(Math.max(10, pollIntervalMillis));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assertions.fail(onTimeoutMessage.get());
    }

    @FunctionalInterface
    private interface BooleanSupplier { boolean getAsBoolean(); }

    @FunctionalInterface
    private interface MessageSupplier { String get(); }

    private static Settings loadSettings() {
        // Prefer system properties, then fallback to classpath application.properties
        Properties props = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {
        }

        String url = System.getProperty("spring.datasource.url", props.getProperty("spring.datasource.url"));
        String username = System.getProperty("spring.datasource.username", props.getProperty("spring.datasource.username"));
        String password = System.getProperty("spring.datasource.password", props.getProperty("spring.datasource.password"));
        String driver = System.getProperty("spring.datasource.driver-class-name", props.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver"));

        if (url == null || username == null) {
            throw new IllegalStateException("spring.datasource.url/username not found. Ensure application.properties is on classpath or set system properties.");
        }
        return new Settings(url, username, password, driver);
    }

    // ============ Small holder types ============

    private static final class Settings {
        final String url;
        final String username;
        final String password;
        final String driverClass;

        Settings(String url, String username, String password, String driverClass) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.driverClass = driverClass;
        }
    }

    private static final class ConnectionHolder implements AutoCloseable {
        final Connection connection;
        final long createdAtMillis = System.currentTimeMillis();
        final Duration maxAge = Duration.ofMinutes(10); // recreate if too old

        private ConnectionHolder(Connection c) {
            this.connection = c;
        }

        static ConnectionHolder create(Settings s) {
            try {
                Class.forName(s.driverClass);
                Properties info = new Properties();
                info.setProperty("user", s.username);
                if (s.password != null) info.setProperty("password", s.password);
                Connection c = DriverManager.getConnection(s.url, info);
                c.setAutoCommit(true);
                return new ConnectionHolder(c);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create DB connection: " + e.getMessage(), e);
            }
        }

        boolean isValid() {
            try {
                if (connection == null || connection.isClosed()) return false;
                if (System.currentTimeMillis() - createdAtMillis > maxAge.toMillis()) return false;
                return connection.isValid(2);
            } catch (SQLException e) {
                return false;
            }
        }

        @Override
        public void close() {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
