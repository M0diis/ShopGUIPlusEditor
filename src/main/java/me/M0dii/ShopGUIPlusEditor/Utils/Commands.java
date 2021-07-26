package me.M0dii.ShopGUIPlusEditor.Utils;

import me.M0dii.ShopGUIPlusEditor.ShopGUIPlusEditor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter
{
    ShopGUIPlusEditor plugin;
    
    public Commands(ShopGUIPlusEditor plugin)
    {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd,
                             @Nonnull String alias, @Nonnull String[] args)
    {
        if(args.length == 1)
        {
            if(args[0].equalsIgnoreCase("reload"))
            {
                if(!sender.hasPermission("shopguipluseditor.command.reload"))
                {
                    String msg = plugin.getCfg().getMessages().get(Messages.NO_PERMISSION);
                    
                    sender.sendMessage(msg);
                    
                    return true;
                }
                
                plugin.getCfg().reload(this.plugin);
                
                String msg = plugin.getCfg().getMessages().get(Messages.RELOADED);
                
                sender.sendMessage(msg);
            }
        }
        
        return true;
    }
    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command cmd,
                                      @Nonnull String alias, @Nonnull String[] args)
    {
        List<String> completes = new ArrayList<>();
        
        if(args.length == 1)
        {
            completes.add("reload");
        }
        
        
        return completes;
    }
}
