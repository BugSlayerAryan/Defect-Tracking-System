package com.dts.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {
    private static final Properties PROPS = new Properties();
    private static final ResolvedConfig CONFIG;

    static {
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                throw new IllegalStateException("db.properties not found in classpath");
            }
            PROPS.load(is);
            CONFIG = resolveConfig();
            Class.forName(CONFIG.driver);
            initializeSchemaIfNeeded(CONFIG);
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {
    }

    private static String envOrProperty(String envKey, String propKey) {
        String value = System.getenv(envKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return PROPS.getProperty(propKey);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String normalizeJdbcUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }
        if (rawUrl.startsWith("postgres://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
        }
        if (rawUrl.startsWith("postgresql://")) {
            return "jdbc:" + rawUrl;
        }
        return rawUrl;
    }

    private static String inferDriver(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        }
        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        return null;
    }

    private static ResolvedConfig resolveConfig() {
        String rawUrl = firstNonBlank(
                System.getenv("DB_URL"),
                System.getenv("DATABASE_URL"),
                PROPS.getProperty("db.url"));

        String jdbcUrl = normalizeJdbcUrl(rawUrl);

        String user = firstNonBlank(
                System.getenv("DB_USER"),
                System.getenv("DATABASE_USER"),
                PROPS.getProperty("db.user"));

        String password = firstNonBlank(
                System.getenv("DB_PASSWORD"),
                System.getenv("DATABASE_PASSWORD"),
                PROPS.getProperty("db.password"));

        if ((user == null || password == null) && rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            try {
                URI uri = new URI(rawUrl.startsWith("postgres://") ? "postgresql://" + rawUrl.substring("postgres://".length()) : rawUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    if (user == null || user.isBlank()) {
                        user = parts[0];
                    }
                    if (password == null || password.isBlank()) {
                        password = parts[1];
                    }
                }
            } catch (URISyntaxException ignored) {
            }
        }

        String driver = firstNonBlank(
                System.getenv("DB_DRIVER"),
                inferDriver(jdbcUrl),
                PROPS.getProperty("db.driver"));

        if (jdbcUrl == null || driver == null) {
            throw new IllegalStateException("Database configuration is incomplete. Set DB_URL and DB_DRIVER.");
        }

        return new ResolvedConfig(jdbcUrl, user, password, driver);
    }

    private static void initializeSchemaIfNeeded(ResolvedConfig config) throws SQLException, IOException {
        String resource = null;
        if (config.jdbcUrl.startsWith("jdbc:h2:")) {
            resource = "init-h2.sql";
        }
        if (config.jdbcUrl.startsWith("jdbc:postgresql:")) {
            resource = "init-postgres.sql";
        }
        if (resource == null) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(
                config.jdbcUrl,
                config.user,
                config.password)) {
            if (tableExists(conn.getMetaData(), "USERS") && tableExists(conn.getMetaData(), "DEFECTS")) {
                return;
            }

            try (InputStream initSql = DBConnection.class.getClassLoader().getResourceAsStream(resource)) {
                if (initSql == null) {
                    throw new IllegalStateException(resource + " not found in classpath");
                }
                String script = new String(initSql.readAllBytes(), StandardCharsets.UTF_8);
                for (String sql : script.split(";")) {
                    String statement = sql.trim();
                    if (statement.isEmpty()) {
                        continue;
                    }
                    try (Statement st = conn.createStatement()) {
                        st.execute(statement);
                    }
                }
            }
        }
    }

    private static boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.jdbcUrl,
                CONFIG.user,
                CONFIG.password);
    }

    private static final class ResolvedConfig {
        private final String jdbcUrl;
        private final String user;
        private final String password;
        private final String driver;

        private ResolvedConfig(String jdbcUrl, String user, String password, String driver) {
            this.jdbcUrl = jdbcUrl;
            this.user = user;
            this.password = password;
            this.driver = driver;
        }
    }
}
