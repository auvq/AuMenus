package me.auvq.aumenus.listener;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.menu.MenuHolder;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerListener implements Listener {

    private final AuMenus plugin;

    public PlayerListener(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().substring(1);
        String[] parts = message.split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String command = parts[0].toLowerCase();
        Optional<Menu> menuOpt = plugin.getMenuRegistry().findByCommand(command);
        if (menuOpt.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Menu menu = menuOpt.get();
        Player player = event.getPlayer();

        OfflinePlayer target = null;
        Map<String, String> args = new HashMap<>();
        int argIndex = 0;

        if (menu.isAllowTargetPlayer() && menu.isTargetPlayerArg() && parts.length > 1) {
            target = resolveTarget(parts[1], menu.isAllowOfflineTarget());
            if (target == null) {
                player.sendMessage(Util.playerNotFound(parts[1]));
                return;
            }
        }

        int startIndex = (target != null && menu.isTargetPlayerArg()) ? 2 : 1;
        for (int i = startIndex; i < parts.length; i++) {
            if (menu.isAllowTargetPlayer() && parts[i].toLowerCase().startsWith("-p:")) {
                String targetName = parts[i].substring(3);
                target = resolveTarget(targetName, menu.isAllowOfflineTarget());
                if (target == null) {
                    player.sendMessage(Util.playerNotFound(targetName));
                    return;
                }
                continue;
            }
            if (argIndex < menu.getArgs().size()) {
                String sanitized = MiniMessage.miniMessage().escapeTags(parts[i]);
                args.put(menu.getArgs().get(argIndex), sanitized);
                argIndex++;
            }
        }

        plugin.openMenu(player, target, menu, args);
    }

    private @Nullable OfflinePlayer resolveTarget(@NotNull String name, boolean allowOffline) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            return online;
        }
        if (allowOffline) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            if (offline.hasPlayedBefore()) {
                return offline;
            }
        }
        return null;
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        plugin.getLastOpenedMenus().remove(playerId);
        plugin.getPreviousMenus().remove(playerId);

        MenuHolder holder = plugin.getMenuRegistry().getOpenMenu(playerId).orElse(null);
        if (holder == null) {
            return;
        }

        holder.stopUpdateTask();
        holder.stopAnimationTask();
        plugin.getMenuRegistry().trackClose(playerId);
    }
}
