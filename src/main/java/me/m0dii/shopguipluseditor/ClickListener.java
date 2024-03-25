package me.m0dii.shopguipluseditor;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.exception.player.PlayerDataNotLoadedException;
import net.brcdev.shopgui.inventory.ShopInventoryHolder;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;

public class ClickListener implements Listener {
    private final String sep = File.separator;

    private final File splus;

    private final ShopGUIPlusEditor plugin;

    public ClickListener(ShopGUIPlusEditor plugin) {
        this.plugin = plugin;

        File plugins = plugin.getDataFolder().getParentFile();

        splus = new File(plugins.getAbsolutePath() +
                sep + "ShopGUIPlus" + sep + "shops");
    }

    @EventHandler
    public void onMoveItem(InventoryMoveItemEvent e) {
        Inventory inv = e.getSource();

        InventoryHolder holder = inv.getHolder();

        if (holder instanceof ShopEditGUI) {
            e.setCancelled(true);
        }

        inv = e.getDestination();

        holder = inv.getHolder();

        if (holder instanceof ShopEditGUI) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory inv = e.getClickedInventory();
        HumanEntity clicker = e.getWhoClicked();

        if (inv != null) {
            InventoryHolder holder = inv.getHolder();

            if (holder instanceof ShopEditGUI) {
                if (!clicker.hasPermission("shopguipluseditor.use"))
                    return;

                ShopEditGUI se = (ShopEditGUI) holder;
                ShopItem shopItem = se.getShopItem();

                e.setCancelled(true);

                ItemStack clicked = e.getCurrentItem();

                if (clicked != null) {
                    ItemMeta itemMeta = clicked.getItemMeta();

                    PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();

                    NamespacedKey typeKey = new NamespacedKey(this.plugin, "type");

                    int multiplier = 1;

                    NamespacedKey multiplierKey = new NamespacedKey(this.plugin, "shift-multiplier");

                    if (pdc.has(multiplierKey, PersistentDataType.INTEGER) && e.isShiftClick()) {
                        multiplier = pdc.get(multiplierKey, PersistentDataType.INTEGER);
                    }

                    if (pdc.has(typeKey, PersistentDataType.STRING)) {
                        String type = pdc.get(typeKey, PersistentDataType.STRING);

                        if (type.equalsIgnoreCase("sell")) {
                            NamespacedKey amountKey = new NamespacedKey(this.plugin, "amount");

                            if (pdc.has(amountKey, PersistentDataType.DOUBLE)) {
                                double amount = pdc.get(amountKey, PersistentDataType.DOUBLE);

                                se.adjustSellPrice(amount, e.isRightClick());
                            }
                        }

                        if (type.equalsIgnoreCase("buy")) {
                            NamespacedKey amountKey = new NamespacedKey(this.plugin, "amount");

                            if (pdc.has(amountKey, PersistentDataType.DOUBLE)) {
                                double amount = pdc.get(amountKey, PersistentDataType.DOUBLE);

                                se.adjustBuyPrice(amount * multiplier, e.isRightClick());
                            }
                        }
                    }

                    if (clicked.getType().equals(Material.BARRIER)) {
                        try {
                            ShopGuiPlusApi.openShop(
                                    (Player) clicker, shopItem.getShop().getId(), shopItem.getPage());
                        } catch (PlayerDataNotLoadedException ex) {
                            ex.printStackTrace();
                        }
                    }

                    String shop = shopItem.getShop().getId();

                    File cfgFile = new File(splus + sep + shop + ".yml");

                    if (e.getSlot() == 20) {
                        shopItem.setBuyPrice(se.getNewBuyPrice());

                        setAndSave(cfgFile, String.format("%s.items.%s.buyPrice", shop, shopItem.getId()), se.getNewBuyPrice());

                        clicker.sendMessage(ChatColor.translateAlternateColorCodes(
                                '&', "&aSuccessfully updated the item price."));

                        clicker.openInventory(new ShopEditGUI(shopItem).getInventory());
                    } else if (e.getSlot() == 24) {
                        shopItem.setSellPrice(se.getNewSellPrice());

                        setAndSave(cfgFile, String.format("%s.items.%s.sellPrice", shop, shopItem.getId()), se.getNewSellPrice());

                        clicker.sendMessage(ChatColor.translateAlternateColorCodes(
                                '&', "&aSuccessfully updated the item price."));

                        clicker.openInventory(new ShopEditGUI(shopItem).getInventory());
                    }
                }
            }

            if (holder instanceof ShopInventoryHolder) {
                if (!clicker.hasPermission("shopguipluseditor.use"))
                    return;

                if (e.isShiftClick() && e.isRightClick()) {
                    ItemStack clicked = e.getCurrentItem();
                    ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(clicked);

                    if(shopItem == null && clicked != null) {
                        Bukkit.getLogger().warning(
                                "Failed to get ShopItem from ItemStack: " + clicked.getType().name()
                        );
                        return;
                    }

                    if(shopItem == null) {
                        return;
                    }

                    ShopEditGUI shopEditGUI = new ShopEditGUI(shopItem);

                    shopEditGUI.display(e.getWhoClicked());
                }
            }
        }
    }

    private void setAndSave(File cfgFile, String path, double value) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);

        cfg.set(path, value);

        try {
            cfg.save(cfgFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
