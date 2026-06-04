package me.m0dii.shopguipluseditor;

import me.m0dii.shopguipluseditor.utils.Utils;
import org.bukkit.block.banner.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopItemSerializer {
    public void writeItem(ConfigurationSection section, ItemStack item) {
        section.set("material", item.getType().name());
        section.set("quantity", item.getAmount());

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        if (meta.hasDisplayName()) {
            section.set("name", Utils.toConfigString(meta.getDisplayName()));
        }

        if (meta.hasLore() && meta.getLore() != null) {
            List<String> lore = new ArrayList<>();

            for (String line : meta.getLore()) {
                lore.add(Utils.toConfigString(line));
            }

            section.set("lore", lore);
        }

        if (!meta.getItemFlags().isEmpty()) {
            List<String> flags = new ArrayList<>();

            for (ItemFlag flag : meta.getItemFlags()) {
                flags.add(flag.name());
            }

            section.set("flags", flags);
        }

        if (meta.isUnbreakable()) {
            section.set("unbreakable", true);
        }

        if (meta.hasCustomModelData()) {
            section.set("customModelData", meta.getCustomModelData());
        }

        if (meta instanceof Damageable damageable && damageable.hasDamage() && damageable.getDamage() > 0) {
            section.set("damage", damageable.getDamage());
        }

        writeEnchantments(section, meta);
        writePotion(section, meta);
        writeSkull(section, meta);
        writeBanner(section, meta);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void writeEnchantments(ConfigurationSection section, ItemMeta meta) {
        Map<Enchantment, Integer> enchantments = meta instanceof EnchantmentStorageMeta storageMeta
                ? storageMeta.getStoredEnchants()
                : meta.getEnchants();

        if (enchantments.isEmpty()) {
            return;
        }

        List<String> serializedEnchantments = new ArrayList<>();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            serializedEnchantments.add(entry.getKey().getName() + ":" + entry.getValue());
        }

        section.set("enchantments", serializedEnchantments);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void writePotion(ConfigurationSection section, ItemMeta meta) {
        if (!(meta instanceof PotionMeta potionMeta)) {
            return;
        }

        PotionData potionData = potionMeta.getBasePotionData();

        if (potionData == null || potionData.getType() == null) {
            return;
        }

        ConfigurationSection potionSection = section.createSection("potion");
        potionSection.set("type", potionData.getType().name());
        potionSection.set("level", potionData.isUpgraded() ? 2 : 1);
        potionSection.set("extended", potionData.isExtended());
    }

    private void writeSkull(ConfigurationSection section, ItemMeta meta) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            return;
        }

        try {
            Method getPlayerProfile = skullMeta.getClass().getMethod("getPlayerProfile");
            Object profile = getPlayerProfile.invoke(skullMeta);

            if (profile == null) {
                return;
            }

            Method getProperties = profile.getClass().getMethod("getProperties");
            Object properties = getProperties.invoke(profile);

            if (!(properties instanceof Iterable<?> iterable)) {
                return;
            }

            for (Object property : iterable) {
                Method getName = property.getClass().getMethod("getName");
                String name = String.valueOf(getName.invoke(property));

                if (!"textures".equalsIgnoreCase(name)) {
                    continue;
                }

                Method getValue = property.getClass().getMethod("getValue");
                String value = String.valueOf(getValue.invoke(property));

                if (!value.isBlank()) {
                    section.set("skin", value);
                }

                return;
            }
        } catch (ReflectiveOperationException ignored) {
            // Paper exposes player profile APIs, but this fallback keeps compilation stable if that changes.
        }
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void writeBanner(ConfigurationSection section, ItemMeta meta) {
        if (!(meta instanceof BannerMeta bannerMeta) || bannerMeta.getPatterns().isEmpty()) {
            return;
        }

        ConfigurationSection patternsSection = section.createSection("patterns");
        int index = 1;

        for (Pattern pattern : bannerMeta.getPatterns()) {
            ConfigurationSection patternSection = patternsSection.createSection(String.valueOf(index++));
            patternSection.set("type", pattern.getPattern().name());
            patternSection.set("color", pattern.getColor().name());
        }
    }
}
