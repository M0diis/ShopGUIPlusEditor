package me.M0dii.ShopGUIPlusEditor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class NewItemGUI implements InventoryHolder
{
    private Inventory inv;
    
    public NewItemGUI(String shopName, ItemStack item, double sellPrice, double buyPrice)
    {
    
    }
    
    @Override
    public Inventory getInventory()
    {
        return inv;
    }
}
