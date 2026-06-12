package id.battletokens.commands;

import id.battletokens.BattleTokens;
import id.battletokens.managers.TokenManager;
import id.battletokens.models.TokenPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BattleTokenCommand implements CommandExecutor, TabCompleter {

    private final BattleTokens plugin;
    private final TokenManager tokenManager;

    public BattleTokenCommand(BattleTokens plugin) {
        this.plugin = plugin;
        this.tokenManager = plugin.getTokenManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { handleBalance(sender, args); return true; }
        switch (args[0].toLowerCase()) {
            case "balance", "bal" -> handleBalance(sender, args);
            case "give" -> handleGive(sender, args);
            case "take" -> handleTake(sender, args);
            case "top" -> handleTop(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));
        String symbol = plugin.getConfig().getString("tokens.symbol", "🪙");

        if (args.length >= 2 && sender.hasPermission("battletokens.balance.other")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage(prefix + colorize("&cPlayer tidak ditemukan.")); return; }
            TokenPlayer tp = tokenManager.get(target);
            sender.sendMessage(prefix + colorize("&7Saldo &e" + target.getName() + "&7: &6" + tp.getTokens() + " " + symbol));
            return;
        }

        if (!(sender instanceof Player player)) { sender.sendMessage(prefix + colorize("&cHanya player yang bisa mengecek saldo sendiri.")); return; }
        if (!player.hasPermission("battletokens.balance")) { player.sendMessage(prefix + colorize("&cKamu tidak memiliki izin.")); return; }

        TokenPlayer tp = tokenManager.get(player);
        player.sendMessage(prefix + colorize("&7Saldo Battle Token kamu: &6" + tp.getTokens() + " " + symbol));
        player.sendMessage(prefix + colorize("&7Total pernah didapat: &e" + tp.getTotalEarned() + " " + symbol));
    }

    private void handleGive(CommandSender sender, String[] args) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));
        if (!sender.hasPermission("battletokens.give")) { sender.sendMessage(prefix + colorize("&cKamu tidak memiliki izin.")); return; }
        if (args.length < 3) { sender.sendMessage(colorize("&7Penggunaan: /battletoken give <player> <jumlah>")); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(prefix + colorize("&cPlayer tidak ditemukan.")); return; }

        long amount;
        try { amount = Long.parseLong(args[2]); if (amount <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { sender.sendMessage(prefix + colorize("&cJumlah tidak valid.")); return; }

        tokenManager.addTokens(target, amount);
        sender.sendMessage(prefix + colorize("&aKamu memberikan &6" + amount + " Battle Token &akepada &e" + target.getName() + "&a."));
    }

    private void handleTake(CommandSender sender, String[] args) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));
        if (!sender.hasPermission("battletokens.take")) { sender.sendMessage(prefix + colorize("&cKamu tidak memiliki izin.")); return; }
        if (args.length < 3) { sender.sendMessage(colorize("&7Penggunaan: /battletoken take <player> <jumlah>")); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(prefix + colorize("&cPlayer tidak ditemukan.")); return; }

        long amount;
        try { amount = Long.parseLong(args[2]); if (amount <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { sender.sendMessage(prefix + colorize("&cJumlah tidak valid.")); return; }

        if (!tokenManager.takeTokens(target, amount)) {
            sender.sendMessage(prefix + colorize("&cToken &e" + target.getName() + " &ctidak cukup!")); return;
        }
        sender.sendMessage(prefix + colorize("&aKamu mengambil &6" + amount + " Battle Token &adari &e" + target.getName() + "&a."));
    }

    private void handleTop(CommandSender sender, String[] args) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));
        String symbol = plugin.getConfig().getString("tokens.symbol", "🪙");
        int limit = 10;
        if (args.length >= 2) { try { limit = Math.max(1, Math.min(20, Integer.parseInt(args[1]))); } catch (NumberFormatException ignored) {} }

        var top = plugin.getDatabaseManager().getTopPlayers(limit);
        sender.sendMessage(colorize("&8&m----&r &6⚔ Top Battle Token &8&m----"));
        if (top.isEmpty()) { sender.sendMessage(colorize("&7Belum ada data.")); return; }
        for (int i = 0; i < top.size(); i++) {
            var tp = top.get(i);
            sender.sendMessage(colorize("&7" + (i+1) + ". &e" + tp.getName() + " &8| &6" + tp.getTokens() + " " + symbol));
        }
        sender.sendMessage(colorize("&8&m-----------------------------"));
    }

    private void handleReload(CommandSender sender) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));
        if (!sender.hasPermission("battletokens.admin")) {
            sender.sendMessage(prefix + colorize("&cKamu tidak memiliki izin."));
            return;
        }
        plugin.reloadConfig();
        plugin.getShopManager().reload();
        sender.sendMessage(prefix + colorize("&aConfig dan shop berhasil direload! &7("
                + plugin.getShopManager().getCategories().size() + " kategori dimuat)"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----&r &6⚔ BattleToken Help &8&m----"));
        sender.sendMessage(colorize("&6/battletoken balance &7- Cek saldo token"));
        sender.sendMessage(colorize("&6/battletoken give <player> <jumlah> &7- Give token"));
        sender.sendMessage(colorize("&6/battletoken take <player> <jumlah> &7- Ambil token"));
        sender.sendMessage(colorize("&6/battletoken top &7- Top saldo token"));
        if (sender.hasPermission("battletokens.admin"))
            sender.sendMessage(colorize("&6/battletoken reload &7- Reload config & shop"));
        sender.sendMessage(colorize("&8&m-----------------------------"));
    }

    private String colorize(String text) { return text.replace("&", "\u00a7"); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("balance", "give", "take", "top"));
            if (sender.hasPermission("battletokens.admin")) subs.add("reload");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && List.of("give","take","balance").contains(args[0].toLowerCase()))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
