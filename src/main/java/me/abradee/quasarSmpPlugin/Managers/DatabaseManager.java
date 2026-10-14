package me.abradee.quasarSmpPlugin.Managers;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private final Connection connection;

    public DatabaseManager(File dataFolder, String fileName) throws SQLException {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, fileName);
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        createTables();
    }

    private void createTables() throws SQLException {
        String generalPlayersSql = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                code TEXT NOT NULL,
                uses INTEGER NOT NULL DEFAULT 0
            );
            """;

        String mediaPlayersSql = """
            CREATE TABLE IF NOT EXISTS player_media_codes (
                uuid TEXT,
                code TEXT,
                uses INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, code)
            );
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(generalPlayersSql);
            statement.execute(mediaPlayersSql);
        }
    }

    public Map<String, Integer> loadPlayerQuests(UUID uuid) {
        Map<String, Integer> progressMap = new HashMap<>();
        String sql = "SELECT code, uses FROM player_media_codes WHERE uuid = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    progressMap.put(rs.getString("code"), rs.getInt("uses"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return progressMap;
    }

    public void saveQuestProgress(UUID uuid, String code, int uses) {
        String sql = "INSERT OR REPLACE INTO player_media_codes (uuid, code, uses) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, code);
            ps.setInt(3, uses);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}