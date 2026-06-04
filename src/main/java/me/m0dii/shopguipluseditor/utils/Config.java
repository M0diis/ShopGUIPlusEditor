package me.m0dii.shopguipluseditor.utils;

import me.m0dii.shopguipluseditor.ShopGUIPlusEditor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Config {
    private final ShopGUIPlusEditor plugin;

    private FileConfiguration cfg;

    private Map<Messages, String> messages = new EnumMap<>(Messages.class);
    private List<String> commandHelp = new ArrayList<>();

    private String priceEditTitle;
    private int editMenuSize;
    private int previewSlot;

    private ItemStack fillItem;
    private ButtonTemplate buyPriceButton;
    private ButtonTemplate sellPriceButton;
    private ButtonTemplate saveButton;
    private ButtonTemplate removeButton;
    private ButtonTemplate backButton;

    private List<AdjustButtonTemplate> buyAdjustButtons = new ArrayList<>();
    private List<AdjustButtonTemplate> sellAdjustButtons = new ArrayList<>();

    private double defaultBuyPrice;
    private double defaultSellPrice;
    private int defaultPage;
    private int defaultItemQuantity;
    private boolean copyHeldAmount;

    private double buyPriceMin;
    private double buyPriceMax;
    private double sellPriceMin;
    private double sellPriceMax;

    public Config(ShopGUIPlusEditor plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        load(plugin);
    }

    public void load(ShopGUIPlusEditor plugin) {
        this.cfg = plugin.getConfig();
        this.messages = new EnumMap<>(Messages.class);

        this.priceEditTitle = getStr("edit-menu.title", "&8Editing %item_id%");
        this.editMenuSize = normalizeMenuSize(cfg.getInt("edit-menu.size", 54));
        this.previewSlot = cfg.getInt("edit-menu.preview-slot", 22);

        this.fillItem = loadItem("edit-menu.fill-item", Material.GRAY_STAINED_GLASS_PANE, "&r");
        this.buyPriceButton = loadButton("edit-menu.buy-price-button", 20, Material.PAPER, "&aBuy price");
        this.sellPriceButton = loadButton("edit-menu.sell-price-button", 24, Material.PAPER, "&cSell price");
        this.saveButton = loadButton("edit-menu.save-button", 31, Material.EMERALD, "&aSave item");
        this.removeButton = loadButton("edit-menu.remove-button", 32, Material.LAVA_BUCKET, "&cRemove item");
        this.backButton = loadButton("edit-menu.back-button", 49, Material.BARRIER, "&cBack");

        this.buyAdjustButtons = loadAdjustButtons("edit-menu.buy-price-adjust-buttons", new int[]{37, 38, 39});
        this.sellAdjustButtons = loadAdjustButtons("edit-menu.sell-price-adjust-buttons", new int[]{43, 42, 41});

        this.defaultBuyPrice = Utils.roundPrice(cfg.getDouble("new-item.default-buy-price", 0D));
        this.defaultSellPrice = Utils.roundPrice(cfg.getDouble("new-item.default-sell-price", -1D));
        this.defaultPage = Math.max(1, cfg.getInt("new-item.default-page", 1));
        this.defaultItemQuantity = Math.max(1, cfg.getInt("new-item.default-quantity", 1));
        this.copyHeldAmount = cfg.getBoolean("new-item.copy-held-amount", true);

        this.buyPriceMin = cfg.getDouble("validation.buy-price.min", 0D);
        this.buyPriceMax = readMaxValue("validation.buy-price.max");
        this.sellPriceMin = cfg.getDouble("validation.sell-price.min", -1D);
        this.sellPriceMax = readMaxValue("validation.sell-price.max");

        this.commandHelp = Utils.format(cfg.getStringList("messages.command-help"));

        messages.put(Messages.NO_PERMISSION, getStr("messages.no-permission", "&cYou do not have permission to do that."));
        messages.put(Messages.PLAYER_ONLY, getStr("messages.player-only", "&cOnly players can use this command."));
        messages.put(Messages.SET_PRICES, getStr("messages.successfully-set", "&aSuccessfully updated the item prices."));
        messages.put(Messages.ITEM_ADDED, getStr("messages.item-added", "&aSuccessfully added the item to the shop."));
        messages.put(Messages.ITEM_REMOVED, getStr("messages.item-removed", "&aSuccessfully removed the item from the shop."));
        messages.put(Messages.HOLD_ITEM, getStr("messages.hold-item", "&cHold the item you want to add in your main hand."));
        messages.put(Messages.SHOP_NOT_FOUND, getStr("messages.shop-not-found", "&cCould not find a ShopGUIPlus shop named &f%shop_id%&c."));
        messages.put(Messages.ITEM_NOT_FOUND, getStr("messages.item-not-found", "&cCould not find the item ID &f%item_id%&c in &f%shop_id%&c."));
        messages.put(Messages.MATERIAL_NOT_FOUND, getStr("messages.material-not-found", "&cCould not find any ShopGUIPlus item using material &f%material%&c."));
        messages.put(Messages.MATERIAL_AMBIGUOUS, getStr("messages.material-ambiguous", "&cFound &f%material%&c in multiple shops: &f%matches%&c. Use &f/shopguipluseditor edit <material> <shop>&c."));
        messages.put(Messages.MATERIAL_AMBIGUOUS_IN_SHOP, getStr("messages.material-ambiguous-in-shop", "&cFound multiple &f%material%&c entries in &f%shop_id%&c: &f%matches%&c. Use &f/shopguipluseditor edit %shop_id%/<item-id>&c."));
        messages.put(Messages.INVALID_NUMBER, getStr("messages.invalid-number", "&cInvalid number: &f%input%&c."));
        messages.put(Messages.INVALID_SLOT, getStr("messages.invalid-slot", "&cSlot must be between &f0&c and &f%max_slot%&c for &f%shop_id%&c."));
        messages.put(Messages.INVALID_PAGE, getStr("messages.invalid-page", "&cPage must be at least &f1&c."));
        messages.put(Messages.INVALID_ITEM_ID, getStr("messages.invalid-item-id", "&cItem IDs may only contain letters, numbers, hyphens and underscores."));
        messages.put(Messages.ITEM_ALREADY_EXISTS, getStr("messages.item-already-exists", "&cThe item ID &f%item_id%&c already exists in &f%shop_id%&c."));
        messages.put(Messages.SLOT_OCCUPIED, getStr("messages.slot-occupied", "&cSlot &f%item_slot%&c on page &f%item_page%&c is already occupied in &f%shop_id%&c."));
        messages.put(Messages.SAVE_FAILED, getStr("messages.save-failed", "&cFailed to save the ShopGUIPlus shop file."));
        messages.put(Messages.RELOADED, getStr("messages.reloaded", "&aSuccessfully reloaded the config."));
    }

    public String getPriceEditTitle() {
        return this.priceEditTitle;
    }

    public int getEditMenuSize() {
        return editMenuSize;
    }

    public int getPreviewSlot() {
        return previewSlot;
    }

    public ItemStack getFillItem() {
        return fillItem.clone();
    }

    public ButtonTemplate getBuyPriceButton() {
        return buyPriceButton;
    }

    public ButtonTemplate getSellPriceButton() {
        return sellPriceButton;
    }

    public ButtonTemplate getSaveButton() {
        return saveButton;
    }

    public ButtonTemplate getRemoveButton() {
        return removeButton;
    }

    public ButtonTemplate getBackButton() {
        return backButton;
    }

    public List<AdjustButtonTemplate> getBuyAdjustButtons() {
        return buyAdjustButtons;
    }

    public List<AdjustButtonTemplate> getSellAdjustButtons() {
        return sellAdjustButtons;
    }

    public Map<Messages, String> getMessages() {
        return this.messages;
    }

    public List<String> getCommandHelp() {
        return commandHelp;
    }

    public double getDefaultBuyPrice() {
        return defaultBuyPrice;
    }

    public double getDefaultSellPrice() {
        return defaultSellPrice;
    }

    public int getDefaultPage() {
        return defaultPage;
    }

    public int getDefaultItemQuantity() {
        return defaultItemQuantity;
    }

    public boolean shouldCopyHeldAmount() {
        return copyHeldAmount;
    }

    public double clampBuyPrice(double value) {
        return clamp(value, buyPriceMin, buyPriceMax);
    }

    public double clampSellPrice(double value) {
        return clamp(value, sellPriceMin, sellPriceMax);
    }

    private List<AdjustButtonTemplate> loadAdjustButtons(String path, int[] fallbackSlots) {
        ConfigurationSection section = cfg.getConfigurationSection(path);

        if (section == null) {
            return Collections.emptyList();
        }

        List<AdjustButtonTemplate> buttons = new ArrayList<>();
        int fallbackIndex = 0;

        for (String key : section.getKeys(false)) {
            ConfigurationSection buttonSection = section.getConfigurationSection(key);

            if (buttonSection == null) {
                continue;
            }

            int slot = buttonSection.getInt("slot",
                    fallbackSlots[Math.min(fallbackIndex, fallbackSlots.length - 1)]);
            fallbackIndex++;

            double amount = buttonSection.getDouble("amount", 1D);
            int shiftMultiplier = buttonSection.getInt("shift-multiplier",
                    buttonSection.getInt("multiplier", 2));
            int itemAmount = Math.max(1, buttonSection.getInt("size",
                    buttonSection.getInt("amount-item-count", 1)));

            Material material = parseMaterial(buttonSection.getString("material"), Material.WHITE_CONCRETE);
            String name = buttonSection.getString("name", "&a$%amount%");
            List<String> lore = buttonSection.getStringList("lore");

            ItemStack item = loadItem(material, itemAmount, name, lore);
            buttons.add(new AdjustButtonTemplate(slot, item, amount, shiftMultiplier));
        }

        buttons.sort(Comparator.comparingInt(AdjustButtonTemplate::slot));
        return buttons;
    }

    private ButtonTemplate loadButton(String path, int defaultSlot, Material defaultMaterial, String defaultName) {
        ConfigurationSection section = cfg.getConfigurationSection(path);

        if (section == null) {
            return new ButtonTemplate(defaultSlot, loadItem(defaultMaterial, 1, defaultName, List.of("")));
        }

        int slot = section.getInt("slot", defaultSlot);
        int amount = Math.max(1, section.getInt("amount", 1));
        Material material = parseMaterial(section.getString("material"), defaultMaterial);
        String name = section.getString("name", defaultName);
        List<String> lore = section.getStringList("lore");

        return new ButtonTemplate(slot, loadItem(material, amount, name, lore));
    }

    private ItemStack loadItem(String path, Material defaultMaterial, String defaultName) {
        ConfigurationSection section = cfg.getConfigurationSection(path);

        if (section == null) {
            return loadItem(defaultMaterial, 1, defaultName, List.of(""));
        }

        Material material = parseMaterial(section.getString("material"), defaultMaterial);
        int amount = Math.max(1, section.getInt("amount", 1));
        String name = section.getString("name", defaultName);
        List<String> lore = section.getStringList("lore");

        return loadItem(material, amount, name, lore);
    }

    private ItemStack loadItem(Material material, int amount, String name, List<String> lore) {
        ItemStack item = Utils.createItem(material, name, lore);
        item.setAmount(Math.max(1, amount));
        return item;
    }

    private Material parseMaterial(String materialName, Material fallback) {
        if (materialName == null || materialName.isBlank()) {
            return fallback;
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        return material != null ? material : fallback;
    }

    private double clamp(double value, double min, double max) {
        double rounded = Utils.roundPrice(value);
        return Math.min(Math.max(rounded, min), max);
    }

    private double readMaxValue(String path) {
        double configured = cfg.getDouble(path, -1D);
        return configured < 0 ? Double.MAX_VALUE : configured;
    }

    private int normalizeMenuSize(int size) {
        if (size < 9) {
            return 9;
        }

        if (size > 54) {
            return 54;
        }

        return size % 9 == 0 ? size : 54;
    }

    private String getStr(String path, String fallback) {
        return Utils.format(cfg.getString(path, fallback));
    }

    public record ButtonTemplate(int slot, ItemStack item) {
    }

    public record AdjustButtonTemplate(int slot, ItemStack item, double amount, int shiftMultiplier) {
    }
}
