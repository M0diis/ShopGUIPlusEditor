package me.m0dii.shopguipluseditor.utils;

import me.m0dii.shopguipluseditor.ShopEditGUI;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

public class Utils {
    private static final NumberFormat formatter = new DecimalFormat("#0.00");

    public static String setPlaceholders(ShopItem item, ShopEditGUI gui, String text) {
        text = text.replace("%item_id%", item.getId())
                .replace("%current_buy_price%", formatter.format(item.getBuyPrice()))
                .replace("%new_buy_price%", formatter.format(gui.getNewBuyPrice()))
                .replace("%current_sell_price%", formatter.format(item.getSellPrice()))
                .replace("%new_sell_price%", formatter.format(gui.getNewSellPrice()));

        return format(text);
    }

    public static String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static ItemStack createItem(Material m, String name, List<String> lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(format(name));
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);

        return item;
    }

    public static void createItem(Material m, int amount, String name, List<String> lore,
                                  int slot, Inventory inv) {
        ItemStack item = new ItemStack(m, amount);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(format(name));
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);

        inv.setItem(slot, item);
    }
}
