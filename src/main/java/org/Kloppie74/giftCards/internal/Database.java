package org.Kloppie74.giftCards.internal;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Database {
    private final File dbFile;
    private Connection connection;

    public Database(File dataFolder) {
        this.dbFile = new File(dataFolder, "giftcards.db");
        try {
            if (!dataFolder.exists()) dataFolder.mkdirs();
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS giftcards (" +
                "uuid TEXT NOT NULL," +
                "code TEXT NOT NULL," +
                "PRIMARY KEY(uuid, code)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public synchronized void addGiftCard(UUID uuid, String code) {
        String sql = "INSERT OR IGNORE INTO giftcards (uuid, code) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getGiftCards(UUID uuid) {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT code FROM giftcards WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    codes.add(rs.getString("code"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return codes;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}