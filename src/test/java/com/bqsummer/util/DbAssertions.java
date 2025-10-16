package com.bqsummer.util;

/**
 * Convenience static facade for {@link DbAssert} to enable static imports like:
 *   import static com.bqsummer.util.DbAssertions.*;
 */
public final class DbAssertions {
    private DbAssertions() {}

    public static void assertExists(String table, String whereClause, Object... params) {
        DbAssert.assertExists(table, whereClause, params);
    }

    public static void assertNotExists(String table, String whereClause, Object... params) {
        DbAssert.assertNotExists(table, whereClause, params);
    }

    public static void assertRowCount(String table, long expected) {
        DbAssert.assertRowCount(table, expected);
    }

    public static void assertCount(String table, String whereClause, long expected, Object... params) {
        DbAssert.assertCount(table, whereClause, expected, params);
    }

    public static void assertValue(String table, String column, Object expected, String whereClause, Object... params) {
        DbAssert.assertValue(table, column, expected, whereClause, params);
    }
}
