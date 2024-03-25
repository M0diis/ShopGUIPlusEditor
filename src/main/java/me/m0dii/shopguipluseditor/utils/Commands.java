package me.m0dii.shopguipluseditor.utils;

import me.m0dii.shopguipluseditor.ShopGUIPlusEditor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {
    private final File splus;
    private final Config cfg;

    ShopGUIPlusEditor plugin;
    String sep = File.separator;

    public Commands(ShopGUIPlusEditor plugin) {
        this.plugin = plugin;

        splus = new File(plugin.getDataFolder().getParentFile().getAbsolutePath()
                + sep + "ShopGUIPlus" + sep + "shops");

        this.cfg = plugin.getCfg();
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd,
                             @Nonnull String alias, @Nonnull String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("shopguipluseditor.command.reload")) {
                    String msg = cfg.getMessages().get(Messages.NO_PERMISSION);

                    sender.sendMessage(msg);

                    return true;
                }

                plugin.getCfg().reload(this.plugin);

                String msg = cfg.getMessages().get(Messages.RELOADED);

                sender.sendMessage(msg);
            }
        }

        if (sender instanceof Player) {
            Player p = (Player) sender;

//            if(args.length > 1)
//            {
//                if(args[0].equalsIgnoreCase("add"))
//                {
//                    if(!p.hasPermission("shopguipluseditor.command.add"))
//                    {
//                        String msg = cfg.getMessages().get(Messages.NO_PERMISSION);
//
//                        p.sendMessage(msg);
//
//                        return true;
//                    }
//
//                    ItemStack inHand = p.getInventory().getItemInMainHand();
//
//                    if(inHand.getType() != Material.AIR)
//                    {
//                        if(args[1].equalsIgnoreCase("item"))
//                        {
//                            String shopname = args[2];
//
//                            String page = args[3];
//                            String slot = args[4];
//
//                            double buyPrice = Double.parseDouble(args[5]);
//
//                            double sellPrice = -1;
//
//                            if(args.length == 7)
//                                sellPrice = Double.parseDouble(args[6]);
//
////                            ShopGuiPlusApi.getShop()
////
////                            FileConfiguration cfg = YamlConfiguration
////                                    .loadConfiguration(splus.getAbsolutePath() + sep + shopname);
////
//
//
//
//                        }
//
//
//
//
//                    }
//                }
//            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command cmd,
                                      @Nonnull String alias, @Nonnull String[] args) {
        List<String> completes = new ArrayList<>();

        if (args.length == 1) {
            completes.add("reload");

            completes.add("add");
        }

        if (args.length == 2) {
            completes.add("item");
            completes.add("command");
            completes.add("permission");
        }


        return completes;
    }
}
