package id.battletokens.commands;

import id.battletokens.BattleTokens;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BattleSeasonCommand implements CommandExecutor, TabCompleter {

    private final BattleTokens plugin;

    public BattleSeasonCommand(BattleTokens plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));

        if (!sender.hasPermission("battletokens.season.reset")) {
            sender.sendMessage(prefix + colorize("&cKamu tidak memiliki izin."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            String season = plugin.getConfig().getString("season.current-season", "Season 1");
            sender.sendMessage(colorize("&8&m----&r &6⚔ Season Info &8&m----"));
            sender.sendMessage(colorize("&7Season Aktif: &e" + season));
            sender.sendMessage(colorize("&7Reset Otomatis: &e" + plugin.getConfig().getBoolean("season.auto-reset", false)));
            sender.sendMessage(colorize("&c/battleseason reset &7- Reset semua token"));
            sender.sendMessage(colorize("&8&m-----------------------------"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            // Konfirmasi reset
            if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                sender.sendMessage(prefix + colorize("&cPeringatan! Ini akan mereset &lSEMUA&c token player!"));
                sender.sendMessage(prefix + colorize("&7Ketik &c/battleseason reset confirm &7untuk melanjutkan."));
                return true;
            }

            // Jalankan reset async
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                // Save semua cache dulu
                plugin.getTokenManager().saveAll();
                // Reset database
                plugin.getDatabaseManager().resetAllTokens();
                // Clear cache
                plugin.getTokenManager().clearCache();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    String season = plugin.getConfig().getString("season.current-season", "Season 1");
                    String broadcast = colorize("&8[&6⚔ BattleTokens&8] &e" + season
                            + " &7telah berakhir! Semua Battle Token telah direset.");
                    Bukkit.broadcastMessage(broadcast);
                    sender.sendMessage(prefix + colorize("&aSeason reset berhasil!"));
                });
            });
            return true;
        }

        return true;
    }

    private String colorize(String text) { return text.replace("&", "\u00a7"); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("info", "reset").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return List.of("confirm");
        return List.of();
    }
}
