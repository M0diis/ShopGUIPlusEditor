package me.M0dii.ShopGUIPlusEditor.Utils;

import me.M0dii.ShopGUIPlusEditor.ShopGUIPlusEditor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class Config
{
    FileConfiguration cfg;
    
    public void reload(ShopGUIPlusEditor plugin)
    {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
        
        this.load(plugin);
    }
    
    HashMap<Messages, String> messages;
    
    private String priceEditTitle;
    private String savedPricesMessage, noPermissionMessage;
    
    public String getPriceEditTitle()
    {
        return this.priceEditTitle;
    }
    
    public String getSavedPricesMessage()
    {
        return this.savedPricesMessage;
    }
    
    private ItemStack buyPriceButton;
    private ItemStack sellPriceButton;
    
    private ItemStack fillItem;
    
    public ItemStack getBuyPriceButton()
    {
        return this.buyPriceButton;
    }
    
    public ItemStack getSellPriceButton()
    {
        return this.sellPriceButton;
    }
    
    public ItemStack getFillItem()
    {
        return this.fillItem;
    }
    
    public void load(ShopGUIPlusEditor plugin)
    {
        this.cfg = plugin.getConfig();
        
        this.messages = new HashMap<>();
        
        this.priceEditTitle = getStr("edit-menu.title");
        
        List<String> buyPriceButtonLore = cfg.getStringList("edit-menu.buy-price-button.lore");
        List<String> sellPriceButtonLore = cfg.getStringList("edit-menu.sell-price-button.lore");
        
        List<String> fillItemLore = cfg.getStringList("edit-menu.fill-item.lore");
        
        Material buyPriceButtonMat = Material.getMaterial(cfg.getString("edit-menu.buy-price-button.material", "PAPER"));
        Material sellPriceButtonMat = Material.getMaterial(cfg.getString("edit-menu.sell-price-button.material", "PAPER"));
        
        Material fillItemMat = Material.getMaterial(cfg.getString("edit-menu.fill-item.material",
                "GREEN_STAINED_GLASS_PANE"));
        
        String buyPriceButtonName = getStr("edit-menu.buy-price-button.name");
        String sellPriceButtonName = getStr("edit-menu.sell-price-button.name");
        
        String fillItemName = getStr("edit-menu.fill-item.name");
        
        this.buyPriceButton = Utils.createItem(buyPriceButtonMat, buyPriceButtonName, buyPriceButtonLore);
        this.sellPriceButton = Utils.createItem(sellPriceButtonMat, sellPriceButtonName, sellPriceButtonLore);
        
        this.fillItem = Utils.createItem(fillItemMat, fillItemName, fillItemLore);
        
        messages.put(Messages.SET_PRICES, getStr("messages.successfully-set"));
        messages.put(Messages.NO_PERMISSION, getStr("messages.no-permission"));
        messages.put(Messages.RELOADED, getStr("messages.reloaded"));
    }
    
    public HashMap<Messages, String> getMessages()
    {
        return this.messages;
    }
    private String getStr(String path)
    {
        return Utils.format(this.cfg.getString(path, ""));
    }
}
