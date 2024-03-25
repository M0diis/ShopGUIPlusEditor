package me.m0dii.shopguipluseditor;

import me.m0dii.shopguipluseditor.utils.Config;
import me.m0dii.shopguipluseditor.utils.Utils;
import net.brcdev.shopgui.shop.item.ShopItem;
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

public class ShopEditGUI implements InventoryHolder {
    private static final NumberFormat formatter = new DecimalFormat("#0.00");
    private final Config cfg = ShopGUIPlusEditor.getInstance().getCfg();

    private Inventory inv;
    private final ShopItem item;

    private double newBuyPrice = 0;
    private double newSellPrice = 0;

    public double getNewBuyPrice() {
        return this.newBuyPrice;
    }

    public double getNewSellPrice() {
        return this.newSellPrice;
    }

    public ShopItem getShopItem() {
        return this.item;
    }

    public ShopEditGUI(ShopItem item) {
        this.item = item;

        this.newBuyPrice = item.getBuyPrice();
        this.newSellPrice = item.getSellPrice();

        initialise();
    }

    private void initialise() {
        this.inv = Bukkit.createInventory(this, 54, format(cfg.getPriceEditTitle()));

        this.inv.setItem(22, item.getPlaceholder());

        ItemStack buyPriceButton = new ItemStack(cfg.getBuyPriceButton());
        inv.setItem(20, updateLore(buyPriceButton));

        ItemStack sellPriceButton = new ItemStack(cfg.getSellPriceButton());

        inv.setItem(24, updateLore(sellPriceButton));

        ItemStack buy1 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("buy", this, getShopItem(), 1);
        ItemStack buy2 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("buy", this, getShopItem(), 2);
        ItemStack buy3 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("buy", this, getShopItem(), 3);

        inv.setItem(37, buy1);
        inv.setItem(38, buy2);
        inv.setItem(39, buy3);

        ItemStack sell1 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("sell", this, getShopItem(), 1);
        ItemStack sell2 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("sell", this, getShopItem(), 2);
        ItemStack sell3 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("sell", this, getShopItem(), 3);

        inv.setItem(41, sell3);
        inv.setItem(42, sell2);
        inv.setItem(43, sell1);

        Utils.createItem(Material.BARRIER, 1, "&8» &aGo back", new ArrayList<>(), 4, inv);

        fill();
    }

    private ItemStack updateLore(ItemStack item) {
        List<String> lore = item.getLore();
        List<String> newLore = new ArrayList<>();

        if (lore != null) {
            for (String s : lore) {
                newLore.add(format(s));
            }
        }

        ItemMeta m = item.getItemMeta();

        m.setLore(newLore);
        item.setItemMeta(m);

        return item;
    }

    private void fill() {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);

            if (item == null)
                inv.setItem(i, cfg.getFillItem());
        }
    }

    public void adjustBuyPrice(double amount, boolean subtract) {
        newBuyPrice = subtract ? newBuyPrice - amount : newBuyPrice + amount;

        newBuyPrice = Double.parseDouble(formatter.format(newBuyPrice));

        ItemStack buyPriceButton = new ItemStack(cfg.getBuyPriceButton());

        ItemStack buy1 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("buy", this, getShopItem(), 1);
        ItemStack buy2 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("buy", this, getShopItem(), 2);
        ItemStack buy3 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("buy", this, getShopItem(), 3);

        inv.setItem(37, updateLore(buy1));
        inv.setItem(38, updateLore(buy2));
        inv.setItem(39, updateLore(buy3));

        inv.setItem(20, updateLore(buyPriceButton));
    }

    public void adjustSellPrice(double amount, boolean subtract) {
        newSellPrice = subtract ? newSellPrice - amount : newSellPrice + amount;

        newSellPrice = Double.parseDouble(formatter.format(newSellPrice));

        ItemStack sellPriceButton = new ItemStack(cfg.getSellPriceButton());

        ItemStack sell1 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("sell", this, getShopItem(), 1);
        ItemStack sell2 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("sell", this, getShopItem(), 2);
        ItemStack sell3 = ShopGUIPlusEditor.getInstance().getCfg().getAdjustButton("sell", this, getShopItem(), 3);

        inv.setItem(41, updateLore(sell3));
        inv.setItem(42, updateLore(sell2));
        inv.setItem(43, updateLore(sell1));

        inv.setItem(24, updateLore(sellPriceButton));
    }

    private String format(String text) {
        return Utils.setPlaceholders(item, this, text);
    }

    public void display(HumanEntity e) {
        e.openInventory(this.inv);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
