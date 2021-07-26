package me.M0dii.ShopGUIPlusEditor.Utils;

import me.M0dii.ShopGUIPlusEditor.ShopGUIPlusEditor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

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
    
    private String priceEditTitle;
    
    public String getPriceEditTitle()
    {
        return this.priceEditTitle;
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
    }
    
    
    private String getStr(String path)
    {
        return Utils.format(this.cfg.getString(path, ""));
    }
}
