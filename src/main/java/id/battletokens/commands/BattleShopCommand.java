package id.battletokens.commands;

import id.battletokens.BattleTokens;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BattleShopCommand implements CommandExecutor, TabCompleter {

    private final BattleTokens plugin;

    public BattleShopCommand(BattleTokens plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + colorize("&cHanya player yang bisa membuka shop!"));
            return true;
        }

        if (!player.hasPermission("battletokens.shop")) {
            player.sendMessage(prefix + colorize("&cKamu tidak memiliki izin."));
            return true;
        }

        plugin.getShopGUI().openMainMenu(player);
        return true;
    }

    private String colorize(String text) { return text.replace("&", "\u00a7"); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
