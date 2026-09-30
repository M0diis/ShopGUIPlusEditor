package me.m0dii.shopguipluseditor.utils;

import me.m0dii.shopguipluseditor.ShopGUIPlusEditor;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {
    private final ShopGUIPlusEditor plugin;
    private final int resourceId;

    public UpdateChecker(ShopGUIPlusEditor plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URLConnection connection = URI.create(
                                "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId)
                        .toURL().openConnection();
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout(5_000);

                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    if (scanner.hasNextLine()) {
                        String version = scanner.nextLine().trim();

                        if (!version.isEmpty() && plugin.isEnabled()) {
                            consumer.accept(version);
                        }
                    }
                }
            } catch (IOException ex) {
                plugin.getLogger().fine("Failed to check for updates: " + ex.getMessage());
            }
        });
    }
}
