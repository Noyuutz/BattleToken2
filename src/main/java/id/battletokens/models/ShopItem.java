package id.battletokens.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class ShopItem {
    private final String id;
    private final int slot;
    private final String name;
    private final Material material;
    private final long price;
    private final int amount;
    private final Map<Enchantment, Integer> enchants;
    private final List<String> lore;
    private final List<String> commands;

    public ShopItem(String id, int slot, String name, Material material, long price,
                    int amount, Map<Enchantment, Integer> enchants, List<String> lore, List<String> commands) {
        this.id = id;
        this.slot = slot;
        this.name = name;
        this.material = material;
        this.price = price;
        this.amount = amount;
        this.enchants = enchants;
        this.lore = lore;
        this.commands = commands;
    }

    public String getId() { return id; }
    public int getSlot() { return slot; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public long getPrice() { return price; }
    public int getAmount() { return amount; }
    public Map<Enchantment, Integer> getEnchants() { return enchants; }
    public List<String> getLore() { return lore; }
    public List<String> getCommands() { return commands; }

    public ItemStack buildItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(colorize(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(this::colorize).toList());
        }
        item.setItemMeta(meta);

        if (enchants != null) {
            enchants.forEach((ench, level) -> item.addUnsafeEnchantment(ench, level));
        }
        return item;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "\u00a7");
    }
}
