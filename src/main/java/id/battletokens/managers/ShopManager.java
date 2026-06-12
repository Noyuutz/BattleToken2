package id.battletokens.managers;

import id.battletokens.BattleTokens;
import id.battletokens.models.ShopCategory;
import id.battletokens.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ShopManager {

    private final BattleTokens plugin;
    private final List<ShopCategory> categories = new ArrayList<>();

    public ShopManager(BattleTokens plugin) {
        this.plugin = plugin;
    }

    public void loadShops() {
        categories.clear();
        File shopsDir = new File(plugin.getDataFolder(), "shops");
        if (!shopsDir.exists()) {
            shopsDir.mkdirs();
            plugin.saveResource("shops/Exclusive.yml", false);
            plugin.saveResource("shops/Lifesteal.yml", false);
        }

        File[] files = shopsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("Tidak ada file shop ditemukan di folder shops/");
            return;
        }

        for (File file : files) {
            try {
                ShopCategory category = loadCategory(file);
                if (category != null) {
                    categories.add(category);
                    plugin.getLogger().info("✔ Shop dimuat: " + file.getName() + " (" + category.getItems().size() + " item)");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Gagal memuat shop: " + file.getName() + " - " + e.getMessage(), e);
            }
        }

        // Urutkan berdasarkan slot
        categories.sort(Comparator.comparingInt(ShopCategory::getSlot));
        plugin.getLogger().info("✔ Total " + categories.size() + " kategori shop dimuat.");
    }

    private ShopCategory loadCategory(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id = file.getName().replace(".yml", "");
        String name = cfg.getString("shop-name", id);
        String matStr = cfg.getString("shop-icon", "CHEST");
        Material icon = parseMaterial(matStr, Material.CHEST);
        List<String> iconLore = cfg.getStringList("shop-icon-lore");
        int slot = cfg.getInt("shop-slot", 11);
        int size = cfg.getInt("shop-size", 54);

        List<ShopItem> items = new ArrayList<>();
        if (cfg.isConfigurationSection("items")) {
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                int itemSlot = cfg.getInt(path + ".slot", 0);
                String itemName = cfg.getString(path + ".name", key);
                Material itemMat = parseMaterial(cfg.getString(path + ".material", "STONE"), Material.STONE);
                long price = cfg.getLong(path + ".price", 0);
                int amount = cfg.getInt(path + ".amount", 1);
                List<String> lore = cfg.getStringList(path + ".lore");
                List<String> commands = cfg.getStringList(path + ".commands");

                // Parse enchants
                Map<Enchantment, Integer> enchants = new HashMap<>();
                if (cfg.isConfigurationSection(path + ".enchants")) {
                    for (String enchStr : cfg.getConfigurationSection(path + ".enchants").getKeys(false)) {
                        try {
                            Enchantment ench = Enchantment.getByName(enchStr.toUpperCase());
                            if (ench != null) {
                                int level = cfg.getInt(path + ".enchants." + enchStr, 1);
                                enchants.put(ench, level);
                            }
                        } catch (Exception ignored) {}
                    }
                }

                items.add(new ShopItem(key, itemSlot, itemName, itemMat, price, amount, enchants, lore, commands));
            }
        }

        return new ShopCategory(id, name, icon, iconLore, slot, size, items);
    }

    private Material parseMaterial(String str, Material fallback) {
        try { return Material.valueOf(str.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }

    public List<ShopCategory> getCategories() { return Collections.unmodifiableList(categories); }

    public ShopCategory getCategory(String id) {
        return categories.stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public void reload() { loadShops(); }
}
