package me.auvq.aumenus.hook;

import me.auvq.aumenus.AuMenus;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PapiHook {

    public void registerAuMenusExpansion(@NotNull AuMenus plugin) {
        new AuMenusExpansion(plugin).register();
    }

    public @NotNull String setPlaceholders(@NotNull OfflinePlayer player, @NotNull String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
