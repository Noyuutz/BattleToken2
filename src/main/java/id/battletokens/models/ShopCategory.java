package id.battletokens.models;

import org.bukkit.Material;
import java.util.List;

public class ShopCategory {
    private final String id;
    private final String name;
    private final Material icon;
    private final List<String> iconLore;
    private final int slot;
    private final int size;
    private final List<ShopItem> items;

    public ShopCategory(String id, String name, Material icon, List<String> iconLore,
                        int slot, int size, List<ShopItem> items) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.iconLore = iconLore;
        this.slot = slot;
        this.size = size;
        this.items = items;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getIcon() { return icon; }
    public List<String> getIconLore() { return iconLore; }
    public int getSlot() { return slot; }
    public int getSize() { return size; }
    public List<ShopItem> getItems() { return items; }

    public String getColoredName() {
        return name.replace("&", "\u00a7");
    }
}
