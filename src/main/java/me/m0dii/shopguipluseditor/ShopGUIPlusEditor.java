package me.m0dii.shopguipluseditor;

import me.m0dii.shopguipluseditor.utils.Commands;
import me.m0dii.shopguipluseditor.utils.Config;
import me.m0dii.shopguipluseditor.utils.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.MultiLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ShopGUIPlusEditor extends JavaPlugin {
    private static ShopGUIPlusEditor instance;

    private Config cfg;
    private ShopFileService shopFileService;

    public static ShopGUIPlusEditor getInstance() {
        return instance;
    }

    public Config getCfg() {
        return cfg;
    }

    public ShopFileService getShopFileService() {
        return shopFileService;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();

        this.cfg = new Config(this);
        this.cfg.load(this);
        this.shopFileService = new ShopFileService(this);

        getServer().getPluginManager().registerEvents(new ClickListener(this), this);

        PluginCommand command = getCommand("shopguipluseditor");

        if (command != null) {
            Commands commands = new Commands(this);
            command.setExecutor(commands);
            command.setTabCompleter(commands);
        }

        getLogger().info("ShopGUIPlusEditor has been enabled.");

        checkForUpdates();
        setupMetrics();
    }

    @Override
    public void onDisable() {
        getLogger().info("ShopGUIPlusEditor has been disabled.");
    }

    private void checkForUpdates() {
        new UpdateChecker(this, 94668).getVersion(ver ->
        {
            String currentVersion = getPluginMeta().getVersion();

            if (!currentVersion.equalsIgnoreCase(ver.replace("v", ""))) {
                getLogger().info("You are running an outdated version of ShopGUIPlusEditor.");
                getLogger().info("Latest version: " + ver + ", you are using: " + currentVersion);
                getLogger().info("You can download the latest version on Spigot:");
                getLogger().info("https://www.spigotmc.org/resources/94668/");
            }
        });
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 12210);

        CustomChart chart = new MultiLineChart("players_and_servers", () ->
        {
            Map<String, Integer> valueMap = new HashMap<>();

            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());

            return valueMap;
        });

        metrics.addCustomChart(chart);
    }
}
