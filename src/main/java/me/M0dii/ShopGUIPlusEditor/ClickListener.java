package me.M0dii.ShopGUIPlusEditor;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.exception.player.PlayerDataNotLoadedException;
import net.brcdev.shopgui.inventory.ShopInventoryHolder;
import net.brcdev.shopgui.shop.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import java.io.File;
import java.io.IOException;

public class ClickListener implements Listener
{
    private final File plugins = ShopGUIPlusEditor.instance.getDataFolder().getParentFile();
    
    private final String sep = File.separator;
    
    private final File splus = new File(plugins.getAbsolutePath() +
            sep + "ShopGUIPlus" + sep + "shops");
    
    @EventHandler
    public void onMoveItem(InventoryMoveItemEvent e)
    {
        Inventory inv = e.getSource();
        
        InventoryHolder holder = inv.getHolder();

        if(holder instanceof ShopEditGUI)
        {
            e.setCancelled(true);
        }
        
        inv = e.getDestination();
    
        holder = inv.getHolder();
    
        if(holder instanceof ShopEditGUI)
        {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onClick(InventoryClickEvent e)
    {
        Inventory inv = e.getClickedInventory();
        HumanEntity clicker = e.getWhoClicked();
        
        if(inv != null)
        {
            InventoryHolder holder = inv.getHolder();
            
            if(holder instanceof ShopEditGUI)
            {
                if(!clicker.hasPermission("shopguipluseditor.use"))
                    return;
                
                ShopEditGUI se = (ShopEditGUI)holder;
                ShopItem shopItem = se.getShopItem();
                
                e.setCancelled(true);
                
                ItemStack clicked = e.getCurrentItem();
                
                if(clicked != null)
                {
                    int multiplier = e.isShiftClick() ? 2 : 1;
    
                    if(clicked.getType().equals(Material.LIME_CONCRETE))
                    {
                        if(clicked.getAmount() == 1)
                            se.adjustBuyPrice(0.1 * multiplier, e.isRightClick());
                        if(clicked.getAmount() == 2)
                            se.adjustBuyPrice(1 * multiplier, e.isRightClick());
                        if(clicked.getAmount() == 3)
                            se.adjustBuyPrice(10 * multiplier, e.isRightClick());
                    }
    
                    if(clicked.getType().equals(Material.RED_CONCRETE))
                    {
                        if(clicked.getAmount() == 1)
                            se.adjustSellPrice(0.1 * multiplier, e.isRightClick());
                        if(clicked.getAmount() == 2)
                            se.adjustSellPrice(1 * multiplier, e.isRightClick());
                        if(clicked.getAmount() == 3)
                            se.adjustSellPrice(10 * multiplier, e.isRightClick());
                    }
                    
                    if(clicked.getType().equals(Material.BARRIER))
                    {
                        try
                        {
                            ShopGuiPlusApi.openShop(
                                    (Player)clicker, shopItem.getShop().getId(), shopItem.getPage());
                        }
                        catch(PlayerDataNotLoadedException ex)
                        {
                            ex.printStackTrace();
                        }
                    }
    
                    if(clicked.getType().equals(Material.PAPER))
                    {
                        String shop = shopItem.getShop().getId();
    
                        File cfgFile = new File(splus + sep + shop + ".yml");

                        if(e.getSlot() == 20)
                        {
                            shopItem.setBuyPrice(se.getNewBuyPrice());
    
                            setAndSave(cfgFile, String.format("%s.items.%s.buyPrice", shop, shopItem.getId()), se.getNewBuyPrice());
                        }
                        else
                        {
                            shopItem.setSellPrice(se.getNewSellPrice());
    
                            setAndSave(cfgFile, String.format("%s.items.%s.sellPrice", shop, shopItem.getId()), se.getNewSellPrice());
                        }
                        
                        clicker.sendMessage(ChatColor.translateAlternateColorCodes(
                                '&', "&aSuccessfully updated the item price."));
                        
                        clicker.openInventory(new ShopEditGUI(shopItem).getInventory());
                    }
                }
            }
            
            if(holder instanceof ShopInventoryHolder)
            {
                if(!clicker.hasPermission("shopguipluseditor.use"))
                    return;
                
                if(e.isShiftClick() && e.isRightClick())
                {
                    ItemStack clicked = e.getCurrentItem();
                    ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(clicked);
                    ShopEditGUI shopEditGUI = new ShopEditGUI(shopItem);
                    
                    shopEditGUI.display(e.getWhoClicked());
                }
            }
        }
    }
    
    private void setAndSave(File cfgFile, String path, double value)
    {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
        
        cfg.set(path, value);
        
        try
        {
            cfg.save(cfgFile);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
