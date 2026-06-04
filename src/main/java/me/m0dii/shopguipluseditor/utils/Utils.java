package me.m0dii.shopguipluseditor.utils;

import me.m0dii.shopguipluseditor.ShopEditGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Utils {
    private static final NumberFormat FORMATTER =
            new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public static String setPlaceholders(ShopEditGUI gui, String text) {
        text = text.replace("%shop_id%", gui.getShopId())
                .replace("%item_id%", gui.getItemId())
                .replace("%item_slot%", String.valueOf(gui.getSlot()))
                .replace("%item_page%", String.valueOf(gui.getPage()))
                .replace("%editor_mode%", gui.isCreateMode() ? "create" : "edit")
                .replace("%current_buy_price%", formatPrice(gui.getOriginalBuyPrice()))
                .replace("%new_buy_price%", formatPrice(gui.getNewBuyPrice()))
                .replace("%current_sell_price%", formatPrice(gui.getOriginalSellPrice()))
                .replace("%new_sell_price%", formatPrice(gui.getNewSellPrice()));

        return format(text);
    }

    public static String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String toConfigString(String text) {
        return text.replace(ChatColor.COLOR_CHAR, '&');
    }

    public static String formatPrice(double value) {
        return FORMATTER.format(roundPrice(value));
    }

    public static double roundPrice(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static ItemStack createItem(Material m, String name, List<String> lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(format(name));
            itemMeta.setLore(format(lore));
            item.setItemMeta(itemMeta);
        }

        return item;
    }

    public static ItemStack applyPlaceholders(ItemStack template, ShopEditGUI gui) {
        ItemStack item = template.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (meta.hasDisplayName()) {
            meta.setDisplayName(setPlaceholders(gui, meta.getDisplayName()));
        }

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();

            if (lore != null) {
                List<String> renderedLore = new ArrayList<>(lore.size());

                for (String line : lore) {
                    renderedLore.add(setPlaceholders(gui, line));
                }

                meta.setLore(renderedLore);
            }
        }

        item.setItemMeta(meta);

        return item;
    }

    public static List<String> format(List<String> lines) {
        List<String> formatted = new ArrayList<>(lines.size());

        for (String line : lines) {
            formatted.add(format(line));
        }

        return formatted;
    }

    public static void createItem(Material m, int amount, String name, List<String> lore,
                                  int slot, Inventory inv) {
        ItemStack item = new ItemStack(m, amount);
        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(format(name));
            itemMeta.setLore(format(lore));
            item.setItemMeta(itemMeta);
        }

        inv.setItem(slot, item);
    }
}
