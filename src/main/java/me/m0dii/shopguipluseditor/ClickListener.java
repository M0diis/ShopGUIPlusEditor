package me.m0dii.shopguipluseditor;

import me.m0dii.shopguipluseditor.utils.Messages;
import me.m0dii.shopguipluseditor.utils.Utils;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.inventory.ShopInventoryHolder;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ClickListener implements Listener {
    private final ShopGUIPlusEditor plugin;

    public ClickListener(ShopGUIPlusEditor plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMoveItem(InventoryMoveItemEvent event) {
        if (isEditorInventory(event.getSource()) || isEditorInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopEditGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {
            return;
        }

        InventoryHolder holder = clickedInventory.getHolder();

        if (holder instanceof ShopEditGUI editor) {
            handleEditorClick(event, editor);
            return;
        }

        if (holder instanceof ShopInventoryHolder) {
            handleShopClick(event);
        }
    }

    private void handleEditorClick(InventoryClickEvent event, ShopEditGUI editor) {
        event.setCancelled(true);

        HumanEntity clicker = event.getWhoClicked();

        if (!clicker.hasPermission("shopguipluseditor.use")) {
            clicker.sendMessage(message(Messages.NO_PERMISSION, editor));
            return;
        }

        if (!(clicker instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        ItemMeta itemMeta = clicked.getItemMeta();

        if (itemMeta == null) {
            return;
        }

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        String action = pdc.get(new NamespacedKey(plugin, "editor-action"), PersistentDataType.STRING);

        if (action == null) {
            return;
        }

        switch (action) {
            case ShopEditGUI.ACTION_ADJUST_BUY -> {
                Double amount = pdc.get(new NamespacedKey(plugin, "amount"), PersistentDataType.DOUBLE);
                Integer multiplier = pdc.get(new NamespacedKey(plugin, "shift-multiplier"),
                        PersistentDataType.INTEGER);

                if (amount != null) {
                    double value = amount * (event.isShiftClick() && multiplier != null ? multiplier : 1);
                    editor.adjustBuyPrice(value, event.isRightClick());
                }
            }
            case ShopEditGUI.ACTION_ADJUST_SELL -> {
                Double amount = pdc.get(new NamespacedKey(plugin, "amount"), PersistentDataType.DOUBLE);
                Integer multiplier = pdc.get(new NamespacedKey(plugin, "shift-multiplier"),
                        PersistentDataType.INTEGER);

                if (amount != null) {
                    double value = amount * (event.isShiftClick() && multiplier != null ? multiplier : 1);
                    editor.adjustSellPrice(value, event.isRightClick());
                }
            }
            case ShopEditGUI.ACTION_SAVE -> handleSave(player, editor);
            case ShopEditGUI.ACTION_REMOVE -> handleRemove(player, editor);
            case ShopEditGUI.ACTION_BACK -> ShopGuiPlusApi.openShop(player, editor.getShopId(), editor.getPage());
            default -> {
            }
        }
    }

    private void handleSave(Player player, ShopEditGUI editor) {
        ShopFileService.SaveResult result;

        if (editor.isCreateMode()) {
            if (!player.hasPermission("shopguipluseditor.command.add")) {
                player.sendMessage(message(Messages.NO_PERMISSION, editor));
                return;
            }

            result = plugin.getShopFileService().createItem(editor);

            if (!result.success()) {
                player.sendMessage(message(Messages.SAVE_FAILED, editor));
                return;
            }

            player.sendMessage(message(Messages.ITEM_ADDED, editor));
            ShopGuiPlusApi.openShop(player, editor.getShopId(), editor.getPage());
            return;
        }

        result = plugin.getShopFileService().updatePrices(editor.getShopId(), editor.getItemId(),
                editor.getNewBuyPrice(), editor.getNewSellPrice());

        if (!result.success()) {
            player.sendMessage(message(Messages.SAVE_FAILED, editor));
            return;
        }

        player.sendMessage(message(Messages.SET_PRICES, editor));

        Shop reloadedShop = ShopGuiPlusApi.getShop(editor.getShopId());

        if (reloadedShop != null) {
            ShopItem reloadedItem = reloadedShop.getShopItem(editor.getItemId());

            if (reloadedItem != null) {
                new ShopEditGUI(reloadedItem).display(player);
                return;
            }
        }

        ShopGuiPlusApi.openShop(player, editor.getShopId(), editor.getPage());
    }

    private void handleRemove(Player player, ShopEditGUI editor) {
        if (!player.hasPermission("shopguipluseditor.command.remove")) {
            player.sendMessage(message(Messages.NO_PERMISSION, editor));
            return;
        }

        ShopFileService.SaveResult result = plugin.getShopFileService()
                .removeItem(editor.getShopId(), editor.getItemId());

        if (!result.success()) {
            player.sendMessage(message(Messages.SAVE_FAILED, editor));
            return;
        }

        player.sendMessage(message(Messages.ITEM_REMOVED, editor));
        ShopGuiPlusApi.openShop(player, editor.getShopId(), editor.getPage());
    }

    private void handleShopClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();

        if (!clicker.hasPermission("shopguipluseditor.use")
                || !(clicker instanceof Player player)
                || !event.isShiftClick()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(player, clicked);

        if (shopItem == null) {
            return;
        }

        event.setCancelled(true);
        new ShopEditGUI(shopItem).display(player);
    }

    private boolean isEditorInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ShopEditGUI;
    }

    private String message(Messages key, ShopEditGUI editor) {
        return Utils.setPlaceholders(editor, plugin.getCfg().getMessages().get(key));
    }
}
