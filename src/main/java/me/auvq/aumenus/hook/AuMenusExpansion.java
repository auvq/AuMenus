package me.auvq.aumenus.hook;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.menu.MenuHolder;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AuMenusExpansion extends PlaceholderExpansion {

    private final AuMenus plugin;

    public AuMenusExpansion(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "aumenus";
    }

    @Override
    public @NotNull String getAuthor() {
        return "auvq";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        switch (params) {
            case "is_in_menu" -> {
                boolean inMenu = plugin.getMenuRegistry().getOpenMenu(player.getUniqueId()).isPresent();
                return String.valueOf(inMenu);
            }
            case "opened_menu" -> {
                MenuHolder holder = plugin.getMenuRegistry().getOpenMenu(player.getUniqueId()).orElse(null);
                if (holder == null) {
                    return "";
                }
                return holder.getMenu().getName();
            }
            case "last_menu" -> {
                String last = plugin.getLastOpenedMenus().get(player.getUniqueId());
                return last != null ? last : "";
            }
        }

        if (params.startsWith("meta_has_value_")) {
            return handleMetaHasValue(player, params.substring("meta_has_value_".length()));
        }

        if (params.startsWith("meta_")) {
            return handleMeta(player, params.substring("meta_".length()));
        }

        return null;
    }

    private static final List<String> KNOWN_TYPES = List.of(
            "STRING", "BOOLEAN", "DOUBLE", "LONG", "INTEGER", "INT"
    );

    private record TypeScanResult(int position, @Nullable String type) {}

    private @NotNull TypeScanResult findTypeInParams(@NotNull String params) {
        String upper = params.toUpperCase();
        int lastUnderscore = -1;
        String matchedType = null;

        for (String knownType : KNOWN_TYPES) {
            int idx = upper.lastIndexOf("_" + knownType);
            if (idx >= 0 && idx > lastUnderscore) {
                lastUnderscore = idx;
                matchedType = knownType;
            }
        }

        return new TypeScanResult(lastUnderscore, matchedType);
    }

    private @NotNull String handleMeta(@NotNull Player player, @NotNull String params) {
        TypeScanResult scan = findTypeInParams(params);
        if (scan.type() == null) {
            return "";
        }

        String key = params.substring(0, scan.position());
        String remainder = params.substring(scan.position() + 1 + scan.type().length());
        String defaultValue = remainder.startsWith("_") ? remainder.substring(1) : "";

        return plugin.getMetaStore().get(player, key, scan.type(), defaultValue);
    }

    private @NotNull String handleMetaHasValue(@NotNull Player player, @NotNull String params) {
        TypeScanResult scan = findTypeInParams(params);
        String key = scan.type() != null ? params.substring(0, scan.position()) : params;

        return String.valueOf(plugin.getMetaStore().hasValue(player, key, scan.type()));
    }
}
