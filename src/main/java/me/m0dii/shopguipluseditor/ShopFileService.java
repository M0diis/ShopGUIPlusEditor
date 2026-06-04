package me.m0dii.shopguipluseditor;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.gui.element.button.GuiButton;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class ShopFileService {
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final ShopGUIPlusEditor plugin;
    private final ShopItemSerializer serializer;
    private final File shopsDirectory;

    public ShopFileService(ShopGUIPlusEditor plugin) {
        this.plugin = plugin;
        this.serializer = new ShopItemSerializer();
        this.shopsDirectory = new File(plugin.getDataFolder().getParentFile(), "ShopGUIPlus" + File.separator + "shops");
    }

    public Set<String> getKnownShopIds() {
        Set<String> shopIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        File[] files = shopsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                shopIds.add(name.substring(0, name.length() - 4));
            }
        }

        return shopIds;
    }

    public ItemStack prepareItemForCreation(ItemStack source) {
        ItemStack clone = source.clone();

        int amount = plugin.getCfg().shouldCopyHeldAmount()
                ? source.getAmount()
                : plugin.getCfg().getDefaultItemQuantity();

        clone.setAmount(Math.max(1, amount));
        return clone;
    }

    public boolean isValidItemId(String itemId) {
        return ITEM_ID_PATTERN.matcher(itemId).matches();
    }

    public boolean itemIdExists(String shopId, String itemId) {
        ConfigurationSection itemsSection = getItemsSection(shopId);
        return itemsSection != null && itemsSection.contains(itemId);
    }

    public String nextItemId(String shopId) {
        ConfigurationSection itemsSection = getItemsSection(shopId);

        if (itemsSection == null) {
            return "1";
        }

        int highest = 0;

        for (String key : itemsSection.getKeys(false)) {
            try {
                highest = Math.max(highest, Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
                // Non-numeric ids are supported; they just do not participate in auto-increment.
            }
        }

        return String.valueOf(highest + 1);
    }

    public Set<String> getItemIds(String shopId) {
        ConfigurationSection itemsSection = getItemsSection(shopId);

        if (itemsSection == null) {
            return Set.of();
        }

        return new TreeSet<>(itemsSection.getKeys(false));
    }

    public ShopItem getShopItem(String shopId, String itemId) {
        Shop shop = ShopGuiPlusApi.getShop(shopId);
        return shop != null ? shop.getShopItem(itemId) : null;
    }

    public Material parseMaterial(String input) {
        return Material.matchMaterial(input, true);
    }

    public List<ShopItemMatch> findByMaterial(Material material) {
        List<ShopItemMatch> matches = new ArrayList<>();

        for (String shopId : getKnownShopIds()) {
            matches.addAll(findByMaterial(material, shopId));
        }

        matches.sort(Comparator
                .comparing(ShopItemMatch::shopId, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(ShopItemMatch::page)
                .thenComparingInt(ShopItemMatch::slot));

        return matches;
    }

    public List<ShopItemMatch> findByMaterial(Material material, String shopId) {
        Shop shop = ShopGuiPlusApi.getShop(shopId);

        if (shop == null) {
            return List.of();
        }

        List<ShopItemMatch> matches = new ArrayList<>();

        for (ShopItem item : shop.getShopItems()) {
            ItemStack stack = item.getItem();

            if (stack == null || stack.getType() != material) {
                continue;
            }

            matches.add(new ShopItemMatch(shopId, item.getId(), item.getPage(), item.getSlot(), item));
        }

        matches.sort(Comparator
                .comparingInt(ShopItemMatch::page)
                .thenComparingInt(ShopItemMatch::slot)
                .thenComparing(ShopItemMatch::itemId, String.CASE_INSENSITIVE_ORDER));

        return matches;
    }

    public boolean isSlotAvailable(Shop shop, int slot, int page) {
        return getSlotConflict(shop, slot, page) == null;
    }

    public String getSlotConflict(Shop shop, int slot, int page) {
        if (shop.getShopItem(slot, page) != null) {
            return "item";
        }

        if (isReserved(shop.getButtonGoBack(), slot)
                || isReserved(shop.getButtonPreviousPage(), slot)
                || isReserved(shop.getButtonNextPage(), slot)) {
            return "button";
        }

        return null;
    }

    public SaveResult updatePrices(String shopId, String itemId, double buyPrice, double sellPrice) {
        FileConfiguration cfg = loadShopConfig(shopId);

        if (cfg == null) {
            return SaveResult.failed();
        }

        String path = shopId + ".items." + itemId;

        if (!cfg.contains(path)) {
            return SaveResult.failed();
        }

        cfg.set(path + ".buyPrice", buyPrice);
        cfg.set(path + ".sellPrice", sellPrice);

        return saveAndReload(shopId, cfg);
    }

    public SaveResult createItem(ShopEditGUI gui) {
        FileConfiguration cfg = loadShopConfig(gui.getShopId());

        if (cfg == null) {
            return SaveResult.failed();
        }

        String basePath = gui.getShopId() + ".items." + gui.getItemId();

        if (cfg.contains(basePath)) {
            return SaveResult.failed();
        }

        cfg.set(basePath + ".type", "item");
        cfg.set(basePath + ".buyPrice", gui.getNewBuyPrice());
        cfg.set(basePath + ".sellPrice", gui.getNewSellPrice());
        cfg.set(basePath + ".slot", gui.getSlot());
        cfg.set(basePath + ".page", gui.getPage() > 1 ? gui.getPage() : null);

        ConfigurationSection itemSection = cfg.createSection(basePath + ".item");
        serializer.writeItem(itemSection, gui.getPreviewItem());

        return saveAndReload(gui.getShopId(), cfg);
    }

    public SaveResult removeItem(String shopId, String itemId) {
        FileConfiguration cfg = loadShopConfig(shopId);

        if (cfg == null) {
            return SaveResult.failed();
        }

        String path = shopId + ".items." + itemId;

        if (!cfg.contains(path)) {
            return SaveResult.failed();
        }

        cfg.set(path, null);

        return saveAndReload(shopId, cfg);
    }

    private SaveResult saveAndReload(String shopId, FileConfiguration cfg) {
        File file = getShopFile(shopId);

        try {
            cfg.save(file);
            reloadShops();
            return SaveResult.saved();
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save shop configuration file: " + file.getAbsolutePath());
            return SaveResult.failed();
        }
    }

    private void reloadShops() {
        ShopGuiPlusApi.getPlugin().getShopManager().loadShops();
    }

    private ConfigurationSection getItemsSection(String shopId) {
        FileConfiguration cfg = loadShopConfig(shopId);

        if (cfg == null) {
            return null;
        }

        return cfg.getConfigurationSection(shopId + ".items");
    }

    private FileConfiguration loadShopConfig(String shopId) {
        File file = getShopFile(shopId);

        if (!file.exists()) {
            return null;
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    private File getShopFile(String shopId) {
        return new File(shopsDirectory, shopId + ".yml");
    }

    private boolean isReserved(GuiButton button, int slot) {
        return button != null && button.getSlots() != null && button.getSlots().contains(slot);
    }

    public record SaveResult(boolean success) {
        public static SaveResult saved() {
            return new SaveResult(true);
        }

        public static SaveResult failed() {
            return new SaveResult(false);
        }
    }

    public record ShopItemMatch(String shopId, String itemId, int page, int slot, ShopItem shopItem) {
    }
}
