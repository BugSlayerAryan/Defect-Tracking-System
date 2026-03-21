package com.dts.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                throw new IllegalStateException("db.properties not found in classpath");
            }
            PROPS.load(is);
            Class.forName(PROPS.getProperty("db.driver"));
        } catch (IOException | ClassNotFoundException e) {
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

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                envOrProperty("DB_URL", "db.url"),
                envOrProperty("DB_USER", "db.user"),
                envOrProperty("DB_PASSWORD", "db.password"));
    }
}
