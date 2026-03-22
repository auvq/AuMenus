package me.auvq.aumenus.hook;

import me.auvq.aumenus.AuMenus;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PapiHook {

    public void registerAuMenusExpansion(@NotNull AuMenus plugin) {
        new AuMenusExpansion(plugin).register();
    }

    public @NotNull String setPlaceholders(@NotNull Player player, @NotNull String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public @NotNull List<String> setPlaceholders(@NotNull Player player, @NotNull List<String> lines) {
        return PlaceholderAPI.setPlaceholders(player, lines);
    }

    public boolean containsPlaceholders(@NotNull String text) {
        return PlaceholderAPI.containsPlaceholders(text);
    }
}
