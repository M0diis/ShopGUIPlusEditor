package me.M0dii.ShopGUIPlusEditor;

import me.M0dii.ShopGUIPlusEditor.Utils.Config;
import me.M0dii.ShopGUIPlusEditor.Utils.Utils;
import net.brcdev.shopgui.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ShopEditGUI implements InventoryHolder
{
    private static final NumberFormat formatter = new DecimalFormat("#0.00");
    private Config cfg = ShopGUIPlusEditor.instance.getCfg();
    
    private Inventory inv;
    private final ShopItem item;
    
    private double newBuyPrice = 0;
    private double newSellPrice = 0;
    
    public double getNewBuyPrice()
    {
        return this.newBuyPrice;
    }
    
    public double getNewSellPrice()
    {
        return this.newSellPrice;
    }
    
    public ShopItem getShopItem()
    {
        return this.item;
    }
    
    public ShopEditGUI(ShopItem item)
    {
        this.item = item;
        
        this.newBuyPrice = item.getBuyPrice();
        this.newSellPrice = item.getSellPrice();
        
        initialise();
    }
    
    private void initialise()
    {
        this.inv = Bukkit.createInventory(this, 54,
                format(cfg.getPriceEditTitle()));
        
        this.inv.setItem(22, item.getPlaceholder());
        
        ItemStack buyPriceButton = new ItemStack(cfg.getBuyPriceButton());
        inv.setItem(20, updateLore(buyPriceButton));
    
        ItemStack sellPriceButton = new ItemStack(cfg.getSellPriceButton());
        
        inv.setItem(24, updateLore(sellPriceButton));
        
        List<String> editLore = new ArrayList<>();
        editLore.add(format("&8» &aLeft click to add."));
        editLore.add(format("&8» &aRight click to subtract."));
        editLore.add("");
        editLore.add(format("&7Shift-click to multiply by 2"));
    
        Utils.createItem(Material.LIME_CONCRETE, 1, "&a$0.1", editLore, 37, inv);
        Utils.createItem(Material.LIME_CONCRETE, 2, "&a$1", editLore, 38, inv);
        Utils.createItem(Material.LIME_CONCRETE, 3, "&a$10", editLore, 39, inv);
    
        Utils.createItem(Material.RED_CONCRETE, 3, "&c$10", editLore, 41, inv);
        Utils.createItem(Material.RED_CONCRETE, 2, "&c$1", editLore, 42, inv);
        Utils.createItem(Material.RED_CONCRETE, 1, "&c$0.1", editLore, 43, inv);
    
        Utils.createItem(Material.BARRIER, 1, "&8» &aGo back", new ArrayList<>(), 4, inv);
    
        fill();
    }
    
    private ItemStack updateLore(ItemStack item)
    {
        List<String> lore = item.getLore();
        List<String> newLore = new ArrayList<>();
        
        if(lore != null)
            for(String s : lore)
                newLore.add(format(s));
        
        ItemMeta m = item.getItemMeta();
        
        m.setLore(newLore);
        item.setItemMeta(m);
        
        return item;
    }
    
    private void fill()
    {
        for(int i = 0; i < inv.getSize(); i++)
        {
            ItemStack item = inv.getItem(i);
            
            if(item == null)
                inv.setItem(i, cfg.getFillItem());
        }
    }
    
    public void adjustBuyPrice(double amount, boolean subtract)
    {
        newBuyPrice = subtract ? newBuyPrice - amount : newBuyPrice + amount;
    
        newBuyPrice = Double.parseDouble(formatter.format(newBuyPrice));
    
        ItemStack buyPriceButton = new ItemStack(cfg.getBuyPriceButton());

        inv.setItem(20, updateLore(buyPriceButton));
    }
    
    public void adjustSellPrice(double amount, boolean subtract)
    {
        newSellPrice = subtract ? newSellPrice - amount : newSellPrice + amount;
        
        newSellPrice = Double.parseDouble(formatter.format(newSellPrice));
    
        ItemStack sellPriceButton = new ItemStack(cfg.getSellPriceButton());
    
        inv.setItem(24,  updateLore(sellPriceButton));
    }
    
    private String format(String text)
    {
        return Utils.setPlaceholders(item, this, text);
    }
    
    public void display(HumanEntity e)
    {
        e.openInventory(this.inv);
    }
    
    @Override
    public Inventory getInventory()
    {
        return inv;
    }
}
