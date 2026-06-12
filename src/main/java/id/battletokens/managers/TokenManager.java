package id.battletokens.managers;

import id.battletokens.BattleTokens;
import id.battletokens.database.DatabaseManager;
import id.battletokens.models.TokenPlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenManager {

    private final BattleTokens plugin;
    private final DatabaseManager db;
    // Cache sementara selama player online
    private final Map<UUID, TokenPlayer> cache = new HashMap<>();

    public TokenManager(BattleTokens plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public TokenPlayer get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(),
                uuid -> db.getOrCreate(uuid, player.getName()));
    }

    public TokenPlayer get(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        return db.getPlayer(uuid);
    }

    public long getBalance(Player player) {
        return get(player).getTokens();
    }

    public void addTokens(Player player, long amount) {
        TokenPlayer tp = get(player);
        tp.addTokens(amount);
        saveAsync(tp);

        String symbol = plugin.getConfig().getString("tokens.symbol", "🪙");
        String displayName = plugin.getConfig().getString("tokens.display-name", "Battle Token");
        player.sendMessage(colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"))
                + colorize("&aKamu mendapatkan &6+" + amount + " " + symbol + " " + displayName
                + " &a| Total: &6" + tp.getTokens() + " " + symbol));
    }

    public boolean takeTokens(Player player, long amount) {
        TokenPlayer tp = get(player);
        if (!tp.hasTokens(amount)) return false;
        tp.takeTokens(amount);
        saveAsync(tp);
        return true;
    }

    public boolean hasTokens(Player player, long amount) {
        return get(player).hasTokens(amount);
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            TokenPlayer tp = db.getOrCreate(uuid, player.getName());
            cache.put(uuid, tp);
        });
    }

    public void unloadPlayer(UUID uuid) {
        TokenPlayer tp = cache.remove(uuid);
        if (tp != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                    db.updateTokens(tp.getUuid(), tp.getName(), tp.getTokens(), tp.getTotalEarned()));
        }
    }

    public void saveAsync(TokenPlayer tp) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                db.updateTokens(tp.getUuid(), tp.getName(), tp.getTokens(), tp.getTotalEarned()));
    }

    public void saveAll() {
        cache.values().forEach(tp ->
                db.updateTokens(tp.getUuid(), tp.getName(), tp.getTokens(), tp.getTotalEarned()));
    }

    public void clearCache() { cache.clear(); }

    private String colorize(String text) {
        return text.replace("&", "\u00a7");
    }
}
