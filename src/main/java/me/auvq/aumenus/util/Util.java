package me.auvq.aumenus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.auvq.aumenus.hook.HookProvider;
import me.auvq.aumenus.menu.MenuHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Util {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern SECTION_HEX_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("§([0-9a-fk-orA-FK-OR])");

    private Util() {}

    public static @NotNull String toLegacyMiniMessage(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String result = input;

        Matcher sectionHexMatcher = SECTION_HEX_PATTERN.matcher(result);
        StringBuilder sectionHexBuilder = new StringBuilder();
        while (sectionHexMatcher.find()) {
            String hex = sectionHexMatcher.group().replaceAll("§[xX]|§", "");
            sectionHexMatcher.appendReplacement(sectionHexBuilder, "<color:#" + hex + ">");
        }
        sectionHexMatcher.appendTail(sectionHexBuilder);
        result = sectionHexBuilder.toString();

        result = SECTION_CODE_PATTERN.matcher(result).replaceAll("&$1");

        Matcher hexMatcher = HEX_PATTERN.matcher(result);
        StringBuilder hexBuilder = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuilder, "<color:#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(hexBuilder);
        result = hexBuilder.toString();

        result = replaceLegacyCodes(result);

        return result;
    }

    public static @NotNull Component parse(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String prepared = input.contains("&") ? toLegacyMiniMessage(input) : input;
        return MINI_MESSAGE.deserialize("<!italic>" + prepared);
    }

    public static @NotNull List<Component> parseLines(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        return lines.stream().map(Util::parse).toList();
    }

    public static @NotNull String resolveBuiltInPlaceholders(@NotNull Player player, @NotNull String text) {
        return text
                .replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%player_displayname%", player.getName())
                .replace("%player_level%", String.valueOf(player.getLevel()))
                .replace("%player_health%", String.format("%.1f", player.getHealth()))
                .replace("%player_max_health%", String.format("%.1f", player.getMaxHealth()))
                .replace("%player_food_level%", String.valueOf(player.getFoodLevel()))
                .replace("%player_world%", player.getWorld().getName())
                .replace("%player_x%", String.valueOf(player.getLocation().getBlockX()))
                .replace("%player_y%", String.valueOf(player.getLocation().getBlockY()))
                .replace("%player_z%", String.valueOf(player.getLocation().getBlockZ()))
                .replace("%player_gamemode%", player.getGameMode().name())
                .replace("%player_uuid%", player.getUniqueId().toString())
                .replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%server_max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%random_100%", String.valueOf(ThreadLocalRandom.current().nextInt(100)))
                .replace("%random_1000%", String.valueOf(ThreadLocalRandom.current().nextInt(1000)));
    }

    public static @NotNull ItemStack buildErrorItem(@NotNull String itemName, @Nullable String errorMessage) {
        ItemStack errorStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = errorStack.getItemMeta();
        if (meta == null) {
            return errorStack;
        }
        meta.displayName(Util.parse("&c&lConfig Error: " + itemName));
        List<Component> lore = new ArrayList<>();
        lore.add(Util.parse("&7This item failed to load."));
        if (errorMessage != null) {
            lore.add(Util.parse("&c" + errorMessage));
        }
        lore.add(Util.parse("&7Check console for details."));
        meta.lore(lore);
        errorStack.setItemMeta(meta);
        return errorStack;
    }

    public static @NotNull String resolvePlaceholders(@NotNull Player player,
                                                         @NotNull String text,
                                                         @NotNull MenuHolder holder,
                                                         @NotNull HookProvider hookProvider) {
        String result = text;

        if (!result.contains("{") && !result.contains("%")) {
            return result;
        }

        for (Map.Entry<String, String> arg : holder.getArguments().entrySet()) {
            result = result.replace("{" + arg.getKey() + "}", arg.getValue());
        }

        result = result.replace("{page}", String.valueOf(holder.getCurrentPage()));
        result = result.replace("{max_page}", String.valueOf(holder.getMaxPage()));
        result = result.replace("{player}", player.getName());

        if (result.contains("%")) {
            result = resolveBuiltInPlaceholders(player, result);
            if (hookProvider.isPapiEnabled()) {
                result = hookProvider.papi().setPlaceholders(player, result);
            }
        }

        return result;
    }

    private static @NotNull String replaceLegacyCodes(@NotNull String input) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = LEGACY_CODE_PATTERN.matcher(input);
        while (matcher.find()) {
            String code = matcher.group(1).toLowerCase();
            String replacement = switch (code) {
                case "0" -> "<black>";
                case "1" -> "<dark_blue>";
                case "2" -> "<dark_green>";
                case "3" -> "<dark_aqua>";
                case "4" -> "<dark_red>";
                case "5" -> "<dark_purple>";
                case "6" -> "<gold>";
                case "7" -> "<gray>";
                case "8" -> "<dark_gray>";
                case "9" -> "<blue>";
                case "a" -> "<green>";
                case "b" -> "<aqua>";
                case "c" -> "<red>";
                case "d" -> "<light_purple>";
                case "e" -> "<yellow>";
                case "f" -> "<white>";
                case "k" -> "<obfuscated>";
                case "l" -> "<bold>";
                case "m" -> "<strikethrough>";
                case "n" -> "<underlined>";
                case "o" -> "<italic>";
                case "r" -> "<reset>";
                default -> matcher.group(0);
            };
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
