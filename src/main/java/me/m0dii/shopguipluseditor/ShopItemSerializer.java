package me.m0dii.shopguipluseditor;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.m0dii.shopguipluseditor.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ShopItemSerializer {
    private final Consumer<String> warningLogger;

    public ShopItemSerializer(Consumer<String> warningLogger) {
        this.warningLogger = warningLogger;
    }

    public void writeItem(ConfigurationSection section, ItemStack item) {
        section.set("material", item.getType().name());
        section.set("quantity", item.getAmount());

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        if (meta.hasDisplayName()) {
            Component displayName = meta.displayName();
            if (displayName != null) {
                section.set("name", Utils.toConfigString(displayName));
            }
        }

        if (meta.hasLore() && meta.lore() != null) {
            List<String> lore = new ArrayList<>();

            for (Component line : meta.lore()) {
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

        writeCustomModelData(section, meta, item);

        if (meta instanceof Damageable damageable && damageable.hasDamage() && damageable.getDamage() > 0) {
            section.set("damage", damageable.getDamage());
        }

        writeEnchantments(section, meta);
        writePotion(section, meta);
        writeSkull(section, meta);
        writeBanner(section, meta);
        writeLeatherColor(section, meta);
        writeArmorTrim(section, meta, item);
        writeFireworks(section, meta, item);
    }

    private void writeEnchantments(ConfigurationSection section, ItemMeta meta) {
        Map<Enchantment, Integer> enchantments = meta instanceof EnchantmentStorageMeta storageMeta
                ? storageMeta.getStoredEnchants()
                : meta.getEnchants();

        if (enchantments.isEmpty()) {
            return;
        }

        List<String> serializedEnchantments = new ArrayList<>();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            NamespacedKey key = entry.getKey().getKey();

            if (!NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
                warn("Skipping unsupported custom enchantment " + key + " on " + meta.getClass().getSimpleName());
                continue;
            }

            serializedEnchantments.add(key.getKey().toUpperCase(Locale.ROOT) + ":" + entry.getValue());
        }

        if (!serializedEnchantments.isEmpty()) {
            section.set("enchantments", serializedEnchantments);
        }
    }

    private void writePotion(ConfigurationSection section, ItemMeta meta) {
        if (!(meta instanceof PotionMeta potionMeta)) {
            return;
        }

        PotionType potionType = potionMeta.getBasePotionType();

        if (potionType == null) {
            return;
        }

        ConfigurationSection potionSection = section.createSection("potion");
        potionSection.set("type", potionType.name());

        Color color = potionMeta.getColor();
        if (color != null) {
            potionSection.set("color", colorToConfig(color));
        }

        if (!potionMeta.getCustomEffects().isEmpty()) {
            warn("Custom potion effects are not supported by ShopGUIPlus item configs for "
                    + meta.getClass().getSimpleName());
        }
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

    private void writeBanner(ConfigurationSection section, ItemMeta meta) {
        if (!(meta instanceof BannerMeta bannerMeta) || bannerMeta.getPatterns().isEmpty()) {
            return;
        }

        ConfigurationSection patternsSection = section.createSection("patterns");
        int index = 1;

        for (Pattern pattern : bannerMeta.getPatterns()) {
            NamespacedKey patternKey = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BANNER_PATTERN)
                    .getKeyOrThrow(pattern.getPattern());

            if (!NamespacedKey.MINECRAFT.equals(patternKey.namespace())) {
                warn("Skipping custom banner pattern " + patternKey
                        + "; ShopGUIPlus 1.113.0 accepts built-in pattern names only");
                continue;
            }

            ConfigurationSection patternSection = patternsSection.createSection(String.valueOf(index++));
            patternSection.set("type", patternKey.value().toUpperCase(Locale.ROOT));
            patternSection.set("color", pattern.getColor().name());
        }
    }

    private void writeCustomModelData(ConfigurationSection section, ItemMeta meta, ItemStack item) {
        if (!meta.hasCustomModelDataComponent()) {
            return;
        }

        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        List<Float> values = component.getFloats();
        boolean supportedShape = values.size() == 1
                && component.getFlags().isEmpty()
                && component.getColors().isEmpty()
                && component.getStrings().isEmpty();

        if (supportedShape) {
            float value = values.getFirst();
            if (Float.isFinite(value) && value == Math.rint(value)
                    && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                section.set("customModelData", (int) value);
                return;
            }
        }

        warn("Custom model data on " + item.getType()
                + " cannot be represented by ShopGUIPlus's integer customModelData field");
    }

    private void writeLeatherColor(ConfigurationSection section, ItemMeta meta) {
        if (meta instanceof LeatherArmorMeta leatherMeta && leatherMeta.getColor() != null) {
            section.set("color", colorToConfig(leatherMeta.getColor()));
        }
    }

    private void writeArmorTrim(ConfigurationSection section, ItemMeta meta, ItemStack item) {
        if (!(meta instanceof ArmorMeta armorMeta) || armorMeta.getTrim() == null) {
            return;
        }

        var trim = armorMeta.getTrim();
        NamespacedKey materialKey = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_MATERIAL)
                .getKeyOrThrow(trim.getMaterial());
        NamespacedKey patternKey = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_PATTERN)
                .getKeyOrThrow(trim.getPattern());
        if (!NamespacedKey.MINECRAFT.equals(materialKey.namespace())
                || !NamespacedKey.MINECRAFT.equals(patternKey.namespace())) {
            warn("Skipping non-vanilla armor trim on " + item.getType()
                    + "; ShopGUIPlus 1.113.0 accepts vanilla trim keys only");
            return;
        }

        ConfigurationSection trimSection = section.createSection("armorTrim");
        trimSection.set("material", materialKey.value());
        trimSection.set("pattern", patternKey.value());
    }

    private void writeFireworks(ConfigurationSection section, ItemMeta meta, ItemStack item) {
        if (meta instanceof FireworkMeta fireworkMeta) {
            if (fireworkMeta.getPower() != 1) {
                section.set("fireworkPower", fireworkMeta.getPower());
            }

            if (fireworkMeta.hasEffects()) {
                ConfigurationSection effectsSection = section.createSection("fireworkEffects");
                int index = 1;
                for (FireworkEffect effect : fireworkMeta.getEffects()) {
                    writeFireworkEffect(effectsSection.createSection(String.valueOf(index++)), effect, item);
                }
            }
        } else if (meta instanceof FireworkEffectMeta effectMeta) {
            FireworkEffect effect;
            try {
                effect = effectMeta.getEffect();
            } catch (IllegalStateException ignored) {
                return;
            }

            if (effect == null) {
                return;
            }

            if (effect.getType() != FireworkEffect.Type.BALL || effect.hasFlicker() || effect.hasTrail()) {
                warn("ShopGUIPlus cannot preserve the shape or trail of the firework star on " + item.getType());
            }

            writeFireworkColors(section, "fireworkColor", effect.getColors(), item);
            writeFireworkColors(section, "fireworkFadeColor", effect.getFadeColors(), item);
        }
    }

    private void writeFireworkEffect(ConfigurationSection section, FireworkEffect effect, ItemStack item) {
        section.set("type", effect.getType().name());
        writeFireworkColors(section, "colors", effect.getColors(), item);
        writeFireworkColors(section, "fadeColors", effect.getFadeColors(), item);
        if (effect.hasFlicker()) {
            section.set("flicker", true);
        }
        if (effect.hasTrail()) {
            section.set("trail", true);
        }
    }

    private void writeFireworkColors(ConfigurationSection section, String key, List<Color> colors, ItemStack item) {
        if (colors.isEmpty()) {
            return;
        }

        List<String> dyeColors = new ArrayList<>();
        for (Color color : colors) {
            DyeColor dyeColor = findDyeColor(color);
            if (dyeColor == null) {
                warn("Skipping a custom RGB firework color on " + item.getType()
                        + "; ShopGUIPlus 1.113.0 accepts named dye colors only");
            } else {
                dyeColors.add(dyeColor.name());
            }
        }

        if (!dyeColors.isEmpty()) {
            section.set(key, dyeColors);
        }
    }

    private DyeColor findDyeColor(Color color) {
        for (DyeColor dyeColor : DyeColor.values()) {
            if (dyeColor.getFireworkColor().equals(color)) {
                return dyeColor;
            }
        }

        return null;
    }

    private String colorToConfig(Color color) {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    private void warn(String warning) {
        warningLogger.accept("Item metadata was not fully serialized: " + warning);
    }
}
