package me.M0dii.ShopGUIPlusEditor;

import me.M0dii.ShopGUIPlusEditor.Utils.Commands;
import me.M0dii.ShopGUIPlusEditor.Utils.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.MultiLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import me.M0dii.ShopGUIPlusEditor.Utils.Config;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ShopGUIPlusEditor extends JavaPlugin
{
    public static ShopGUIPlusEditor instance;
    
    private PluginManager manager;
    
    private Config cfg;
    
    public Config getCfg()
    {
        if(cfg == null)
            cfg = new Config(this);
        
        return this.cfg;
    }
    
    public void onEnable()
    {
        instance = this;
    
        this.cfg = new Config(this);
        this.cfg.load(this);
        

        
        this.manager = getServer().getPluginManager();
        
        this.manager.registerEvents(new ClickListener(this), this);
        
        getCommand("shopguipluseditor").setExecutor(new Commands(this));
        
        prepareConfig();
        
        getLogger().info("ShopGUIPlusEditor has been enabled.");
    
        checkForUpdates();
        setupMetrics();
    }
    
    private void checkForUpdates()
    {
        new UpdateChecker(this, 94668).getVersion(ver ->
        {
            String curr = this.getDescription().getVersion();
            
            if (!curr.equalsIgnoreCase(
                    ver.replace("v", "")))
            {
                getLogger().info("You are running an outdated version of ShopGUIPlusEditor.");
                getLogger().info("Latest version: " + ver + ", you are using: " + curr);
                getLogger().info("You can download the latest version on Spigot:");
                getLogger().info("https://www.spigotmc.org/resources/94668/");
            }
        });
    }
    
    private void setupMetrics()
    {
        Metrics metrics = new Metrics(this, 12210);
        
        CustomChart c = new MultiLineChart("players_and_servers", () ->
        {
            Map<String, Integer> valueMap = new HashMap<>();
            
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            
            return valueMap;
        });
        
        metrics.addCustomChart(c);
    }
    
    public void onDisable()
    {
        getLogger().info("ShopGUIPlusEditor has been disabled.");
        
        this.manager.disablePlugin(this);
    }
    
    private void prepareConfig()
    {
        File configFile = new File(this.getDataFolder(), "config.yml");
        
        
        if(!configFile.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            configFile.getParentFile().mkdirs();
            
            this.copy(this.getResource("config.yml"), configFile);
        }
    
        getConfig().options().copyDefaults(true);
        
        try
        {
            getConfig().save(configFile);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    
        YamlConfiguration.loadConfiguration(configFile);
    }
    
    private void copy(InputStream in, File file)
    {
        if(in != null)
        {
            try
            {
                OutputStream out = new FileOutputStream(file);
                
                byte[] buf = new byte[1024];
                
                int len;
                
                while((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
                
                out.close();
                in.close();
            }
            catch(Exception e)
            {
                getLogger().warning("Error copying resource: " + e.getMessage());
                
                e.printStackTrace();
            }
        }
    }
}
