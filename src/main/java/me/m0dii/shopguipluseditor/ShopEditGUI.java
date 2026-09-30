package me.m0dii.shopguipluseditor;

import me.m0dii.shopguipluseditor.utils.Config;
import me.m0dii.shopguipluseditor.utils.Utils;
import net.brcdev.shopgui.shop.item.ShopItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ShopEditGUI implements InventoryHolder {
    public static final String ACTION_ADJUST_BUY = "adjust-buy";
    public static final String ACTION_ADJUST_SELL = "adjust-sell";
    public static final String ACTION_SAVE = "save";
    public static final String ACTION_REMOVE = "remove";
    public static final String ACTION_BACK = "back";

    private final ShopGUIPlusEditor plugin = ShopGUIPlusEditor.getInstance();
    private final Config cfg = plugin.getCfg();

    private final boolean createMode;
    private final String shopId;
    private final String itemId;
    private final ItemStack previewItem;
    private final int page;
    private final int slot;

    private final ShopItem existingItem;

    private final double originalBuyPrice;
    private final double originalSellPrice;

    private double newBuyPrice;
    private double newSellPrice;

    private Inventory inv;

    public ShopEditGUI(ShopItem item) {
        this.createMode = false;
        this.existingItem = item;
        this.shopId = item.getShop().getId();
        this.itemId = item.getId();
        this.page = item.getPage();
        this.slot = item.getSlot();
        this.previewItem = buildPreview(item);
        this.originalBuyPrice = item.getBuyPrice();
        this.originalSellPrice = item.getSellPrice();
        this.newBuyPrice = item.getBuyPrice();
        this.newSellPrice = item.getSellPrice();

        initialise();
    }

    public ShopEditGUI(String shopId, String itemId, ItemStack previewItem, int page, int slot,
                       double buyPrice, double sellPrice) {
        this.createMode = true;
        this.existingItem = null;
        this.shopId = shopId;
        this.itemId = itemId;
        this.page = page;
        this.slot = slot;
        this.previewItem = previewItem.clone();
        this.originalBuyPrice = buyPrice;
        this.originalSellPrice = sellPrice;
        this.newBuyPrice = buyPrice;
        this.newSellPrice = sellPrice;

        initialise();
    }

    public boolean isCreateMode() {
        return createMode;
    }

    public String getShopId() {
        return shopId;
    }

    public String getItemId() {
        return itemId;
    }

    public ItemStack getPreviewItem() {
        return previewItem.clone();
    }

    public int getPage() {
        return page;
    }

    public int getSlot() {
        return slot;
    }

    public double getOriginalBuyPrice() {
        return originalBuyPrice;
    }

    public double getOriginalSellPrice() {
        return originalSellPrice;
    }

    public double getNewBuyPrice() {
        return newBuyPrice;
    }

    public double getNewSellPrice() {
        return newSellPrice;
    }

    public ShopItem getExistingItem() {
        return existingItem;
    }

    public void adjustBuyPrice(double amount, boolean subtract) {
        newBuyPrice = cfg.clampBuyPrice(newBuyPrice + (subtract ? -amount : amount));
        refresh();
    }

    public void adjustSellPrice(double amount, boolean subtract) {
        newSellPrice = cfg.clampSellPrice(newSellPrice + (subtract ? -amount : amount));
        refresh();
    }

    public void display(HumanEntity entity) {
        entity.openInventory(inv);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    private void initialise() {
        this.inv = Bukkit.createInventory(this, cfg.getEditMenuSize(),
                Utils.setPlaceholders(this, cfg.getPriceEditTitle()));

        refresh();
    }

    private void refresh() {
        fill();

        if (isValidSlot(cfg.getPreviewSlot())) {
            inv.setItem(cfg.getPreviewSlot(), previewItem.clone());
        }

        placeButton(cfg.getBuyPriceButton(), null, null);
        placeButton(cfg.getSellPriceButton(), null, null);
        placeButton(cfg.getSaveButton(), ACTION_SAVE, null);
        placeButton(cfg.getBackButton(), ACTION_BACK, null);

        if (!createMode) {
            placeButton(cfg.getRemoveButton(), ACTION_REMOVE, null);
        }

        for (Config.AdjustButtonTemplate button : cfg.getBuyAdjustButtons()) {
            placeButton(new Config.ButtonTemplate(button.slot(), button.item()),
                    ACTION_ADJUST_BUY, button);
        }

        for (Config.AdjustButtonTemplate button : cfg.getSellAdjustButtons()) {
            placeButton(new Config.ButtonTemplate(button.slot(), button.item()),
                    ACTION_ADJUST_SELL, button);
        }
    }

    private void placeButton(Config.ButtonTemplate template, String action,
                             Config.AdjustButtonTemplate adjustButton) {
        if (template == null || !isValidSlot(template.slot())) {
            return;
        }

        ItemStack item = render(template.item(), adjustButton);

        if (action != null) {
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(new NamespacedKey(plugin, "editor-action"), PersistentDataType.STRING, action);

                if (adjustButton != null) {
                    pdc.set(new NamespacedKey(plugin, "amount"), PersistentDataType.DOUBLE, adjustButton.amount());
                    pdc.set(new NamespacedKey(plugin, "shift-multiplier"),
                            PersistentDataType.INTEGER, adjustButton.shiftMultiplier());
                }

                item.setItemMeta(meta);
            }
        }

        inv.setItem(template.slot(), item);
    }

    private ItemStack render(ItemStack template, Config.AdjustButtonTemplate adjustButton) {
        ItemStack item = template.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (meta.hasDisplayName() && meta.displayName() != null) {
            Component displayName = meta.displayName();
            meta.displayName(Utils.setPlaceholders(this, displayName, adjustmentReplacements(adjustButton)));
        }

        if (meta.hasLore() && meta.lore() != null) {
            List<Component> renderedLore = meta.lore().stream()
                    .map(line -> Utils.setPlaceholders(this, line, adjustmentReplacements(adjustButton)))
                    .toList();
            meta.lore(renderedLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String[] adjustmentReplacements(Config.AdjustButtonTemplate adjustButton) {
        if (adjustButton == null) {
            return new String[0];
        }

        return new String[]{
                "%amount%", Utils.formatPrice(adjustButton.amount()),
                "%shift_multiplier%", String.valueOf(adjustButton.shiftMultiplier())
        };
    }

    private void fill() {
        for (int slotIndex = 0; slotIndex < inv.getSize(); slotIndex++) {
            inv.setItem(slotIndex, cfg.getFillItem());
        }
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < inv.getSize();
    }

    private ItemStack buildPreview(ShopItem item) {
        ItemStack source;

        if (item.getItem() != null) {
            source = item.getItem().clone();
        } else if (item.getPlaceholder() != null) {
            source = item.getPlaceholder().clone();
        } else {
            source = new ItemStack(Material.STONE);
        }

        source.setAmount(Math.max(1, source.getAmount()));
        return source;
    }
}
