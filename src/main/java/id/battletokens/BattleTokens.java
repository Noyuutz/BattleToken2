package id.battletokens;

import id.battletokens.commands.*;
import id.battletokens.database.DatabaseManager;
import id.battletokens.gui.ShopGUI;
import id.battletokens.listeners.PlayerListener;
import id.battletokens.managers.ShopManager;
import id.battletokens.managers.TokenManager;
import id.battletokens.utils.BattleTokensPlaceholder;
import org.bukkit.plugin.java.JavaPlugin;

public class BattleTokens extends JavaPlugin {

    private static BattleTokens instance;
    private DatabaseManager databaseManager;
    private TokenManager tokenManager;
    private ShopManager shopManager;
    private ShopGUI shopGUI;

    @Override
    public void onEnable() {
        instance = this;
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        saveDefaultConfig();

        // Database
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("✘ Gagal menginisialisasi database! Plugin dinonaktifkan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Managers
        tokenManager = new TokenManager(this, databaseManager);
        shopManager = new ShopManager(this);
        shopManager.loadShops();

        // GUI
        shopGUI = new ShopGUI(this);

        // Commands
        getCommand("battletoken").setExecutor(new BattleTokenCommand(this));
        getCommand("battletoken").setTabCompleter(new BattleTokenCommand(this));
        getCommand("battleshop").setExecutor(new BattleShopCommand(this));
        getCommand("battleshop").setTabCompleter(new BattleShopCommand(this));
        getCommand("battleseason").setExecutor(new BattleSeasonCommand(this));
        getCommand("battleseason").setTabCompleter(new BattleSeasonCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BattleTokensPlaceholder(this).register();
            getLogger().info("✔ PlaceholderAPI berhasil didaftarkan!");
        }

        // Auto-save setiap 5 menit
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> tokenManager.saveAll(), 20L * 60 * 5, 20L * 60 * 5);

        getLogger().info("======================================");
        getLogger().info("  BattleTokens v" + getDescription().getVersion() + " aktif!");
        getLogger().info("  Database: " + (databaseManager.isUsingMySQL() ? "MySQL" : "SQLite"));
        getLogger().info("  Shop Categories: " + shopManager.getCategories().size());
        getLogger().info("======================================");
    }

    @Override
    public void onDisable() {
        if (tokenManager != null) tokenManager.saveAll();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("BattleTokens telah dinonaktifkan.");
    }

    public static BattleTokens getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public TokenManager getTokenManager() { return tokenManager; }
    public ShopManager getShopManager() { return shopManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
}
