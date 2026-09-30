package me.m0dii.shopguipluseditor.utils;

import me.m0dii.shopguipluseditor.ShopEditGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Utils {
    private static final NumberFormat FORMATTER = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private Utils() {
        /* This utility class should not be instantiated */
    }

    public static Component format(String text) {
        if (text == null) {
            return Component.empty();
        }

        return MINI_MESSAGE.deserialize(convertLegacyFormatting(text));
    }

    public static List<Component> format(List<String> lines) {
        List<Component> formatted = new ArrayList<>(lines.size());

        for (String line : lines) {
            formatted.add(format(line));
        }

        return formatted;
    }

    public static Component setPlaceholders(ShopEditGUI gui, String text) {
        return setPlaceholders(gui, format(text));
    }

    public static Component setPlaceholders(ShopEditGUI gui, Component component, String... extraReplacements) {
        String[] guiReplacements = {
                "%shop_id%", gui.getShopId(),
                "%item_id%", gui.getItemId(),
                "%item_slot%", String.valueOf(gui.getSlot()),
                "%item_page%", String.valueOf(gui.getPage()),
                "%editor_mode%", gui.isCreateMode() ? "create" : "edit",
                "%current_buy_price%", formatPrice(gui.getOriginalBuyPrice()),
                "%new_buy_price%", formatPrice(gui.getNewBuyPrice()),
                "%current_sell_price%", formatPrice(gui.getOriginalSellPrice()),
                "%new_sell_price%", formatPrice(gui.getNewSellPrice())
        };

        String[] replacements = Arrays.copyOf(guiReplacements, guiReplacements.length + extraReplacements.length);
        System.arraycopy(extraReplacements, 0, replacements, guiReplacements.length, extraReplacements.length);
        return replacePlaceholders(component, replacements);
    }

    public static Component replacePlaceholders(Component component, String... replacements) {
        Component result = component;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String placeholder = replacements[i];

            if (placeholder == null || placeholder.isEmpty()) {
                continue;
            }

            TextReplacementConfig replacement = TextReplacementConfig.builder()
                    .matchLiteral(placeholder)
                    .replacement(Component.text(replacements[i + 1]))
                    .build();
            result = result.replaceText(replacement);
        }

        return result;
    }

    public static String toConfigString(Component component) {
        return LEGACY_AMPERSAND.serialize(component);
    }

    public static String formatPrice(double value) {
        return FORMATTER.format(roundPrice(value));
    }

    public static double roundPrice(double value) {
        if (!Double.isFinite(value)) {
            return 0D;
        }

        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(format(name));
            meta.lore(format(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack applyPlaceholders(ItemStack template, ShopEditGUI gui) {
        ItemStack item = template.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (meta.hasDisplayName()) {
            meta.displayName(setPlaceholders(gui, meta.displayName()));
        }

        if (meta.hasLore() && meta.lore() != null) {
            List<Component> renderedLore = new ArrayList<>(meta.lore().size());

            for (Component line : meta.lore()) {
                renderedLore.add(setPlaceholders(gui, line));
            }

            meta.lore(renderedLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static void createItem(Material material, int amount, String name, List<String> lore,
                                  int slot, Inventory inventory) {
        ItemStack item = createItem(material, name, lore);
        item.setAmount(amount);
        inventory.setItem(slot, item);
    }

    private static String convertLegacyFormatting(String text) {
        StringBuilder converted = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);

            if (current == '<') {
                int tagEnd = miniMessageTagEnd(text, i);

                if (tagEnd >= 0) {
                    converted.append(text, i, tagEnd + 1);
                    i = tagEnd;
                    continue;
                }
            }

            if (current != '&' && current != '\u00a7') {
                converted.append(current);
                continue;
            }

            if (i + 1 >= text.length()) {
                converted.append(current);
                continue;
            }

            if (text.charAt(i + 1) == '#' && i + 8 <= text.length()
                    && isHexColor(text, i + 2, 6)) {
                converted.append("<reset><#").append(text, i + 2, i + 8).append('>');
                i += 7;
                continue;
            }

            if (Character.toLowerCase(text.charAt(i + 1)) == 'x'
                    && i + 14 <= text.length()
                    && isRepeatedHexColor(text, i, current)) {
                converted.append("<reset><#");

                for (int digit = 0; digit < 6; digit++) {
                    converted.append(text.charAt(i + 3 + digit * 2));
                }

                converted.append('>');
                i += 13;
                continue;
            }

            String legacyTag = legacyTag(text.charAt(i + 1));

            if (legacyTag == null) {
                converted.append(current);
                continue;
            }

            converted.append(legacyTag);
            i++;
        }

        return converted.toString();
    }

    private static String legacyTag(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<reset><black>";
            case '1' -> "<reset><dark_blue>";
            case '2' -> "<reset><dark_green>";
            case '3' -> "<reset><dark_aqua>";
            case '4' -> "<reset><dark_red>";
            case '5' -> "<reset><dark_purple>";
            case '6' -> "<reset><gold>";
            case '7' -> "<reset><gray>";
            case '8' -> "<reset><dark_gray>";
            case '9' -> "<reset><blue>";
            case 'a' -> "<reset><green>";
            case 'b' -> "<reset><aqua>";
            case 'c' -> "<reset><red>";
            case 'd' -> "<reset><light_purple>";
            case 'e' -> "<reset><yellow>";
            case 'f' -> "<reset><white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    private static boolean isHexColor(String text, int start, int length) {
        for (int i = start; i < start + length; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean isRepeatedHexColor(String text, int start, char marker) {
        for (int digit = 0; digit < 6; digit++) {
            int markerIndex = start + 2 + digit * 2;
            int digitIndex = markerIndex + 1;

            if (text.charAt(markerIndex) != marker || Character.digit(text.charAt(digitIndex), 16) < 0) {
                return false;
            }
        }

        return true;
    }

    private static int miniMessageTagEnd(String text, int start) {
        if (start + 1 >= text.length()) {
            return -1;
        }

        char firstTagCharacter = text.charAt(start + 1);

        if (!Character.isLetter(firstTagCharacter) && firstTagCharacter != '#' && firstTagCharacter != '/'
                && firstTagCharacter != '!' && firstTagCharacter != '?') {
            return -1;
        }

        char quote = 0;
        boolean escaped = false;

        for (int i = start + 1; i < text.length(); i++) {
            char current = text.charAt(i);

            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }

                continue;
            }

            if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '>') {
                return i;
            }
        }

        return -1;
    }
}
