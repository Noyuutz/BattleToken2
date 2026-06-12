package id.battletokens.gui;

import id.battletokens.BattleTokens;
import id.battletokens.managers.ShopManager;
import id.battletokens.managers.TokenManager;
import id.battletokens.models.ShopCategory;
import id.battletokens.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI implements Listener {

    private final BattleTokens plugin;
    private final ShopManager shopManager;
    private final TokenManager tokenManager;

    private static final String MAIN_TAG = "§0§1BATTLESHOP_MAIN";
    private static final String CATEGORY_TAG = "§0§2BATTLESHOP_CAT:";

    public ShopGUI(BattleTokens plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getShopManager();
        this.tokenManager = plugin.getTokenManager();
    }

    // ==================== OPEN MENUS ====================

    public void openMainMenu(Player player) {
        String title = plugin.getConfig().getString("shop.main-title", "&8⚔ &6Battle Shop &8⚔");
        int size = plugin.getConfig().getInt("shop.main-size", 27);
        Inventory inv = Bukkit.createInventory(null, size, colorize(title) + MAIN_TAG);

        // Isi filler glass
        fillGlass(inv);

        // Isi kategori
        for (ShopCategory cat : shopManager.getCategories()) {
            ItemStack icon = buildCategoryIcon(cat, player);
            if (cat.getSlot() < size) inv.setItem(cat.getSlot(), icon);
        }

        // Token display di slot 4
        inv.setItem(4, buildTokenDisplay(player));

        player.openInventory(inv);
    }

    public void openCategoryMenu(Player player, ShopCategory category) {
        // Title yang keliatan player = nama kategori saja
        // Tag disembunyikan di akhir dengan karakter invisible
        String fullTitle = colorize(category.getName())
                + "\u00a7r\u00a70\u00a7r" + CATEGORY_TAG + category.getId();

        Inventory inv = Bukkit.createInventory(null, category.getSize(), fullTitle);

        fillGlass(inv);

        // Isi item shop
        for (ShopItem item : category.getItems()) {
            if (item.getSlot() < category.getSize()) {
                inv.setItem(item.getSlot(), item.buildItemStack());
            }
        }

        // Tombol kembali
        int backSlot = plugin.getConfig().getInt("shop.back-button-slot", 49);
        String backMat = plugin.getConfig().getString("shop.back-button-material", "ARROW");
        String backName = plugin.getConfig().getString("shop.back-button-name", "&cKembali");
        try {
            Material mat = Material.valueOf(backMat.toUpperCase());
            inv.setItem(backSlot, buildItem(mat, backName, List.of("&7Kembali ke menu utama")));
        } catch (Exception ignored) {}

        // Token display
        inv.setItem(4, buildTokenDisplay(player));

        player.openInventory(inv);
    }

    // ==================== CLICK HANDLER ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // ✅ Cancel HANYA jika inventory milik shop kita
        if (!title.contains(MAIN_TAG) && !title.contains(CATEGORY_TAG)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        // Main menu click
        if (title.contains(MAIN_TAG)) {
            handleMainMenuClick(player, event.getSlot());
            return;
        }

        // Category menu click
        if (title.contains(CATEGORY_TAG)) {
            String catId = title.substring(title.indexOf(CATEGORY_TAG) + CATEGORY_TAG.length());
            ShopCategory category = shopManager.getCategory(catId);
            if (category == null) return;
            handleCategoryClick(player, category, event.getSlot());
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        for (ShopCategory cat : shopManager.getCategories()) {
            if (cat.getSlot() == slot) {
                openCategoryMenu(player, cat);
                return;
            }
        }
    }

    private void handleCategoryClick(Player player, ShopCategory category, int slot) {
        // Cek tombol kembali
        int backSlot = plugin.getConfig().getInt("shop.back-button-slot", 49);
        if (slot == backSlot) { openMainMenu(player); return; }

        // Cari item di slot ini
        for (ShopItem item : category.getItems()) {
            if (item.getSlot() == slot) {
                processPurchase(player, item, category);
                return;
            }
        }
    }

    private void processPurchase(Player player, ShopItem item, ShopCategory category) {
        String prefix = colorize(plugin.getConfig().getString("prefix", "&8[&6⚔ BattleTokens&8] &r"));
        String symbol = plugin.getConfig().getString("tokens.symbol", "🪙");

        if (!tokenManager.hasTokens(player, item.getPrice())) {
            player.sendMessage(prefix + colorize("&cToken kamu tidak cukup! Kamu butuh &6"
                    + item.getPrice() + " " + symbol + " &cnamun hanya memiliki &6"
                    + tokenManager.getBalance(player) + " " + symbol));
            return;
        }

        // Potong token
        tokenManager.takeTokens(player, item.getPrice());

        // Berikan item jika ada
        ItemStack purchased = item.buildItemStack();
        if (item.getMaterial() != Material.AIR && item.getCommands().isEmpty()) {
            player.getInventory().addItem(purchased);
        }

        // Jalankan command
        for (String cmd : item.getCommands()) {
            String finalCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        player.sendMessage(prefix + colorize("&aKamu berhasil membeli &e"
                + colorize(item.getName()) + " &aseharga &6" + item.getPrice() + " " + symbol + "&a!"));

        // Refresh GUI
        openCategoryMenu(player, category);
    }

    // ==================== BUILDERS ====================

    private ItemStack buildCategoryIcon(ShopCategory cat, Player player) {
        String symbol = plugin.getConfig().getString("tokens.symbol", "🪙");
        List<String> lore = new ArrayList<>();
        if (cat.getIconLore() != null) cat.getIconLore().forEach(l -> lore.add(colorize(l)));
        lore.add("");
        lore.add(colorize("&7Tokenmu: &6" + tokenManager.getBalance(player) + " " + symbol));
        lore.add(colorize("&eKlik untuk membuka!"));
        return buildItem(cat.getIcon(), cat.getName(), lore);
    }

    private ItemStack buildTokenDisplay(Player player) {
        String symbol = plugin.getConfig().getString("tokens.symbol", "🪙");
        String displayName = plugin.getConfig().getString("tokens.display-name", "Battle Token");
        long balance = tokenManager.getBalance(player);
        return buildItem(Material.SUNFLOWER,
                "&6" + symbol + " " + displayName,
                List.of(colorize("&7Saldo: &6" + balance + " " + symbol)));
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(colorize(name));
        meta.setLore(lore.stream().map(this::colorize).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "\u00a7");
    }
}
