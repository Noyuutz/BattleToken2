package id.battletokens.listeners;

import id.battletokens.BattleTokens;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final BattleTokens plugin;

    public PlayerListener(BattleTokens plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getTokenManager().loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTokenManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
