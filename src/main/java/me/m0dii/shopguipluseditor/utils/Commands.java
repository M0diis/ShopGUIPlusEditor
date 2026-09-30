package me.m0dii.shopguipluseditor.utils;

import me.m0dii.shopguipluseditor.ShopEditGUI;
import me.m0dii.shopguipluseditor.ShopFileService;
import me.m0dii.shopguipluseditor.ShopGUIPlusEditor;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.item.ShopItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class Commands implements CommandExecutor, TabCompleter {
    private final ShopGUIPlusEditor plugin;
    private final Config cfg;
    private final ShopFileService shopFileService;

    public Commands(ShopGUIPlusEditor plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getCfg();
        this.shopFileService = plugin.getShopFileService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String alias, @NotNull String @NonNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "add" -> handleAdd(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "remove" -> handleRemove(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String alias, @NotNull String @NonNull [] args) {
        if (args.length == 1) {
            return partialMatches(args[0], List.of("reload", "add", "edit", "remove"));
        }

        if (args.length == 2 && (isSubcommand(args, "add") || isSubcommand(args, "remove"))) {
            return partialMatches(args[1], new ArrayList<>(shopFileService.getKnownShopIds()));
        }

        if (args.length == 2 && isSubcommand(args, "edit")) {
            List<String> options = new ArrayList<>();

            for (String shopId : shopFileService.getKnownShopIds()) {
                for (String itemId : shopFileService.getItemIds(shopId)) {
                    options.add(shopId + "/" + itemId);
                }
            }

            return partialMatches(args[1], options);
        }

        if (args.length == 3 && isSubcommand(args, "remove")) {
            return partialMatches(args[2], new ArrayList<>(shopFileService.getItemIds(args[1])));
        }

        if (args.length == 3 && isSubcommand(args, "edit")) {
            return partialMatches(args[2], new ArrayList<>(shopFileService.getKnownShopIds()));
        }

        if (args.length == 4 && isSubcommand(args, "add")) {
            return List.of(String.valueOf(cfg.getDefaultPage()));
        }

        if (args.length == 5 && isSubcommand(args, "add")) {
            return List.of(shopFileService.nextItemId(args[1]));
        }

        return Collections.emptyList();
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("shopguipluseditor.command.reload")) {
            sender.sendMessage(message(Messages.NO_PERMISSION));
            return true;
        }

        plugin.getCfg().reload();
        sender.sendMessage(message(Messages.RELOADED));
        return true;
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message(Messages.PLAYER_ONLY));
            return true;
        }

        if (!player.hasPermission("shopguipluseditor.command.edit")) {
            player.sendMessage(message(Messages.NO_PERMISSION));
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            sendEditUsage(player);
            return true;
        }

        if (args[1].contains("/")) {
            return handleDirectEdit(player, args[1]);
        }

        Material material = shopFileService.parseMaterial(args[1]);

        if (material == null) {
            player.sendMessage(message(Messages.MATERIAL_NOT_FOUND, "%material%", args[1]));
            return true;
        }

        if (args.length == 3) {
            return handleMaterialEditInShop(player, material, args[2]);
        }

        List<ShopFileService.ShopItemMatch> matches = shopFileService.findByMaterial(material);

        if (matches.isEmpty()) {
            player.sendMessage(message(Messages.MATERIAL_NOT_FOUND, "%material%", material.name()));
            return true;
        }

        if (matches.size() == 1) {
            openEditor(player, matches.getFirst().shopItem());
            return true;
        }

        Set<String> shops = new LinkedHashSet<>();

        for (ShopFileService.ShopItemMatch match : matches) {
            shops.add(match.shopId());
        }

        player.sendMessage(message(Messages.MATERIAL_AMBIGUOUS,
                "%material%", material.name(),
                "%matches%", String.join(", ", shops)));
        sendEditUsage(player);
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message(Messages.PLAYER_ONLY));
            return true;
        }

        if (!player.hasPermission("shopguipluseditor.command.add")) {
            player.sendMessage(message(Messages.NO_PERMISSION));
            return true;
        }

        if (args.length < 3 || args.length > 5) {
            sendHelp(sender);
            return true;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (heldItem.getType().isAir()) {
            player.sendMessage(message(Messages.HOLD_ITEM));
            sendAddUsage(player);
            return true;
        }

        String shopId = args[1];
        Shop shop = ShopGuiPlusApi.getShop(shopId);

        if (shop == null) {
            player.sendMessage(message(Messages.SHOP_NOT_FOUND, "%shop_id%", shopId));
            return true;
        }

        Integer slot = parseInteger(args[2]);

        if (slot == null) {
            player.sendMessage(message(Messages.INVALID_NUMBER, "%shop_id%", shopId, "%input%", args[2]));
            return true;
        }

        if (slot < 0 || slot >= shop.getSize()) {
            player.sendMessage(message(Messages.INVALID_SLOT,
                    "%shop_id%", shopId,
                    "%item_slot%", String.valueOf(slot),
                    "%max_slot%", String.valueOf(shop.getSize() - 1)));
            return true;
        }

        int page = cfg.getDefaultPage();

        if (args.length >= 4) {
            Integer parsedPage = parseInteger(args[3]);

            if (parsedPage == null) {
                player.sendMessage(message(Messages.INVALID_NUMBER, "%shop_id%", shopId, "%input%", args[3]));
                return true;
            }

            if (parsedPage < 1) {
                player.sendMessage(message(Messages.INVALID_PAGE));
                return true;
            }

            page = parsedPage;
        }

        String itemId = args.length >= 5 ? args[4] : shopFileService.nextItemId(shopId);

        if (!shopFileService.isValidItemId(itemId)) {
            player.sendMessage(message(Messages.INVALID_ITEM_ID, "%shop_id%", shopId, "%item_id%", itemId));
            return true;
        }

        if (shopFileService.itemIdExists(shopId, itemId)) {
            player.sendMessage(message(Messages.ITEM_ALREADY_EXISTS,
                    "%shop_id%", shopId,
                    "%item_id%", itemId,
                    "%item_slot%", String.valueOf(slot),
                    "%item_page%", String.valueOf(page)));
            return true;
        }

        if (!shopFileService.isSlotAvailable(shop, slot, page)) {
            player.sendMessage(message(Messages.SLOT_OCCUPIED,
                    "%shop_id%", shopId,
                    "%item_id%", itemId,
                    "%item_slot%", String.valueOf(slot),
                    "%item_page%", String.valueOf(page)));
            return true;
        }

        ItemStack previewItem = shopFileService.prepareItemForCreation(heldItem);

        new ShopEditGUI(shopId, itemId, previewItem, page, slot,
                cfg.getDefaultBuyPrice(), cfg.getDefaultSellPrice()).display(player);

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shopguipluseditor.command.remove")) {
            sender.sendMessage(message(Messages.NO_PERMISSION));
            return true;
        }

        if (args.length != 3) {
            sendHelp(sender);
            return true;
        }

        String shopId = args[1];
        String itemId = args[2];

        if (ShopGuiPlusApi.getShop(shopId) == null) {
            sender.sendMessage(message(Messages.SHOP_NOT_FOUND, "%shop_id%", shopId, "%item_id%", itemId));
            return true;
        }

        if (!shopFileService.itemIdExists(shopId, itemId)) {
            sender.sendMessage(message(Messages.ITEM_NOT_FOUND, "%shop_id%", shopId, "%item_id%", itemId));
            return true;
        }

        ShopFileService.SaveResult result = shopFileService.removeItem(shopId, itemId);

        if (!result.success()) {
            sender.sendMessage(message(Messages.SAVE_FAILED, "%shop_id%", shopId, "%item_id%", itemId));
            return true;
        }

        sender.sendMessage(message(Messages.ITEM_REMOVED, "%shop_id%", shopId, "%item_id%", itemId));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        if (!cfg.getCommandHelp().isEmpty()) {
            for (Component line : cfg.getCommandHelp()) {
                sender.sendMessage(line);
            }
            return;
        }

        sender.sendMessage(Utils.format("&8/shopguipluseditor reload"));
        sender.sendMessage(Utils.format("&7Hold the item to add in your main hand."));
        sender.sendMessage(Utils.format("&8/shopguipluseditor add <shop> <slot> [page] [item-id] &7(uses held item)"));
        sender.sendMessage(Utils.format("&8/shopguipluseditor edit <shop>/<item-id>"));
        sender.sendMessage(Utils.format("&8/shopguipluseditor edit <material> [shop]"));
        sender.sendMessage(Utils.format("&8/shopguipluseditor remove <shop> <item-id>"));
    }

    private void sendAddUsage(CommandSender sender) {
        sender.sendMessage(Utils.format("&7Hold the item to add in your main hand."));
        sender.sendMessage(Utils.format("&8/shopguipluseditor add <shop> <slot> [page] [item-id] &7(uses held item)"));
    }

    private void sendEditUsage(CommandSender sender) {
        sender.sendMessage(Utils.format("&8/shopguipluseditor edit <shop>/<item-id>"));
        sender.sendMessage(Utils.format("&8/shopguipluseditor edit <material> [shop]"));
    }

    private List<String> partialMatches(String token, List<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        return matches;
    }

    private boolean isSubcommand(String[] args, String subcommand) {
        return args.length > 0 && args[0].equalsIgnoreCase(subcommand);
    }

    private Integer parseInteger(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private boolean handleDirectEdit(Player player, String target) {
        String[] parts = target.split("/", 2);

        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            sendEditUsage(player);
            return true;
        }

        String shopId = parts[0];
        String itemId = parts[1];

        ShopItem item = shopFileService.getShopItem(shopId, itemId);

        if (item == null) {
            if (ShopGuiPlusApi.getShop(shopId) == null) {
                player.sendMessage(message(Messages.SHOP_NOT_FOUND, "%shop_id%", shopId));
            } else {
                player.sendMessage(message(Messages.ITEM_NOT_FOUND, "%shop_id%", shopId, "%item_id%", itemId));
            }

            return true;
        }

        openEditor(player, item);
        return true;
    }

    private boolean handleMaterialEditInShop(Player player, Material material, String shopId) {
        Shop shop = ShopGuiPlusApi.getShop(shopId);

        if (shop == null) {
            player.sendMessage(message(Messages.SHOP_NOT_FOUND, "%shop_id%", shopId));
            return true;
        }

        List<ShopFileService.ShopItemMatch> matches = shopFileService.findByMaterial(material, shopId);

        if (matches.isEmpty()) {
            player.sendMessage(message(Messages.MATERIAL_NOT_FOUND,
                    "%material%", material.name(),
                    "%shop_id%", shopId));
            return true;
        }

        if (matches.size() == 1) {
            openEditor(player, matches.get(0).shopItem());
            return true;
        }

        List<String> found = new ArrayList<>();

        for (ShopFileService.ShopItemMatch match : matches) {
            found.add(match.itemId() + " (page " + match.page() + ", slot " + match.slot() + ")");
        }

        player.sendMessage(message(Messages.MATERIAL_AMBIGUOUS_IN_SHOP,
                "%material%", material.name(),
                "%shop_id%", shopId,
                "%matches%", String.join(", ", found)));
        return true;
    }

    private void openEditor(Player player, ShopItem item) {
        new ShopEditGUI(item).display(player);
    }

    private Component message(Messages key, String... replacements) {
        return Utils.replacePlaceholders(cfg.getMessages().get(key), replacements);
    }
}
