package me.M0dii.ShopGUIPlusEditor.Utils;

import me.M0dii.ShopGUIPlusEditor.ShopEditGUI;
import me.M0dii.ShopGUIPlusEditor.ShopGUIPlusEditor;
import net.brcdev.shopgui.shop.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Utils
{
    public static String setPlaceholders(ShopItem item, ShopEditGUI gui, String text)
    {
        text = text.replace("%item_id%", item.getId())
                .replace("%current_buy_price%", String.valueOf(item.getBuyPrice()))
                .replace("%new_buy_price%", String.valueOf(gui.getNewBuyPrice()))
                .replace("%current_sell_price%", String.valueOf(item.getSellPrice()))
                .replace("%new_sell_price%", String.valueOf(gui.getNewSellPrice()));
        
        return format(text);
    }
    
    public static String format(String text)
    {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public static ItemStack createItem(Material m,String name, List<String> lore)
    {
        ItemStack item = new ItemStack(m);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(format(name));
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        
        return item;
    }
    
    public static void createItem(Material m, int amount, String name, List<String> lore,
                            int slot, Inventory inv)
    {
        ItemStack item = new ItemStack(m, amount);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(format(name));
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        
        inv.setItem(slot, item);
    }
}
