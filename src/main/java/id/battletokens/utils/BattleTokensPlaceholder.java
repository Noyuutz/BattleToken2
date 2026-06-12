package id.battletokens.utils;

import id.battletokens.BattleTokens;
import id.battletokens.models.TokenPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BattleTokensPlaceholder extends PlaceholderExpansion {

    private final BattleTokens plugin;

    public BattleTokensPlaceholder(BattleTokens plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "battletokens"; }
    @Override public @NotNull String getAuthor() { return "ClaudeAI"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "0";
        TokenPlayer tp = plugin.getTokenManager().get(player);
        if (tp == null) return "0";

        return switch (params.toLowerCase()) {
            // %battletokens_balance% → 150
            case "balance" -> String.valueOf(tp.getTokens());
            // %battletokens_total_earned% → 500
            case "total_earned" -> String.valueOf(tp.getTotalEarned());
            // %battletokens_symbol% → 🪙
            case "symbol" -> plugin.getConfig().getString("tokens.symbol", "🪙");
            // %battletokens_balance_formatted% → 150 🪙
            case "balance_formatted" -> tp.getTokens() + " " + plugin.getConfig().getString("tokens.symbol", "🪙");
            default -> null;
        };
    }
}
