package id.battletokens.database;

import id.battletokens.BattleTokens;
import id.battletokens.models.TokenPlayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final BattleTokens plugin;
    private HikariDataSource dataSource;
    private boolean usingMySQL = false;

    public DatabaseManager(BattleTokens plugin) { this.plugin = plugin; }

    public boolean initialize() {
        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        if (dbType.equals("mysql")) {
            if (connectMySQL()) { usingMySQL = true; plugin.getLogger().info("✔ Berhasil terhubung ke MySQL!"); }
            else {
                plugin.getLogger().warning("✘ Gagal terhubung ke MySQL, beralih ke SQLite...");
                if (!connectSQLite()) { plugin.getLogger().severe("✘ Gagal membuat koneksi database!"); return false; }
                plugin.getLogger().info("✔ Menggunakan SQLite sebagai fallback.");
            }
        } else {
            if (!connectSQLite()) { plugin.getLogger().severe("✘ Gagal membuat koneksi SQLite!"); return false; }
            plugin.getLogger().info("✔ Menggunakan SQLite.");
        }
        createTables();
        return true;
    }

    private boolean connectMySQL() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("database.mysql.host", "localhost")
                    + ":" + plugin.getConfig().getInt("database.mysql.port", 3306)
                    + "/" + plugin.getConfig().getString("database.mysql.database", "battletokens")
                    + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
            config.setUsername(plugin.getConfig().getString("database.mysql.username", "root"));
            config.setPassword(plugin.getConfig().getString("database.mysql.password", ""));
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.mysql.pool-size", 10));
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            config.setPoolName("BattleTokens-MySQL");
            config.addDataSourceProperty("cachePrepStmts", "true");
            dataSource = new HikariDataSource(config);
            try (Connection conn = dataSource.getConnection()) { return conn.isValid(3); }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "MySQL connection failed: " + e.getMessage());
            if (dataSource != null && !dataSource.isClosed()) { dataSource.close(); dataSource = null; }
            return false;
        }
    }

    private boolean connectSQLite() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.sqlite.file", "battletokens.db"));
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setMaximumPoolSize(1);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("BattleTokens-SQLite");
            dataSource = new HikariDataSource(config);
            return true;
        } catch (Exception e) { plugin.getLogger().log(Level.SEVERE, "SQLite connection failed: " + e.getMessage()); return false; }
    }

    private void createTables() {
        String sql = usingMySQL
                ? "CREATE TABLE IF NOT EXISTS battle_tokens (player_uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(64) NOT NULL, tokens BIGINT NOT NULL DEFAULT 0, total_earned BIGINT NOT NULL DEFAULT 0, last_updated BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                : "CREATE TABLE IF NOT EXISTS battle_tokens (player_uuid TEXT PRIMARY KEY, player_name TEXT NOT NULL, tokens INTEGER NOT NULL DEFAULT 0, total_earned INTEGER NOT NULL DEFAULT 0, last_updated INTEGER NOT NULL);";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("✔ Tabel battle_tokens berhasil dibuat/diverifikasi.");
        } catch (SQLException e) { plugin.getLogger().log(Level.SEVERE, "Gagal membuat tabel: " + e.getMessage(), e); }
    }

    public Connection getConnection() throws SQLException { return dataSource.getConnection(); }
    public boolean isUsingMySQL() { return usingMySQL; }

    public TokenPlayer getPlayer(UUID uuid) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM battle_tokens WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { plugin.getLogger().log(Level.SEVERE, "Error getPlayer: " + e.getMessage(), e); }
        return null;
    }

    public TokenPlayer getOrCreate(UUID uuid, String name) {
        TokenPlayer player = getPlayer(uuid);
        if (player != null) return player;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO battle_tokens (player_uuid, player_name, tokens, total_earned, last_updated) VALUES (?, ?, 0, 0, ?)")) {
            ps.setString(1, uuid.toString()); ps.setString(2, name); ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.SEVERE, "Error createPlayer: " + e.getMessage(), e); }
        return new TokenPlayer(uuid, name, 0, 0, System.currentTimeMillis());
    }

    public void updateTokens(UUID uuid, String name, long tokens, long totalEarned) {
        String sql = usingMySQL
                ? "INSERT INTO battle_tokens (player_uuid, player_name, tokens, total_earned, last_updated) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = ?, tokens = ?, total_earned = ?, last_updated = ?"
                : "INSERT INTO battle_tokens (player_uuid, player_name, tokens, total_earned, last_updated) VALUES (?, ?, ?, ?, ?) ON CONFLICT(player_uuid) DO UPDATE SET player_name = ?, tokens = ?, total_earned = ?, last_updated = ?";
        long now = System.currentTimeMillis();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, name); ps.setLong(3, tokens); ps.setLong(4, totalEarned); ps.setLong(5, now);
            ps.setString(6, name); ps.setLong(7, tokens); ps.setLong(8, totalEarned); ps.setLong(9, now);
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.SEVERE, "Error updateTokens: " + e.getMessage(), e); }
    }

    public List<TokenPlayer> getTopPlayers(int limit) {
        List<TokenPlayer> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM battle_tokens ORDER BY tokens DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { plugin.getLogger().log(Level.SEVERE, "Error getTopPlayers: " + e.getMessage(), e); }
        return list;
    }

    public void resetAllTokens() {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE battle_tokens SET tokens = 0, total_earned = 0, last_updated = ?")) {
            ps.setLong(1, System.currentTimeMillis()); ps.executeUpdate();
            plugin.getLogger().info("✔ Semua token player berhasil direset.");
        } catch (SQLException e) { plugin.getLogger().log(Level.SEVERE, "Error resetAllTokens: " + e.getMessage(), e); }
    }

    private TokenPlayer mapRow(ResultSet rs) throws SQLException {
        return new TokenPlayer(UUID.fromString(rs.getString("player_uuid")), rs.getString("player_name"), rs.getLong("tokens"), rs.getLong("total_earned"), rs.getLong("last_updated"));
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) { dataSource.close(); plugin.getLogger().info("BattleTokens database connection closed."); }
    }
}
