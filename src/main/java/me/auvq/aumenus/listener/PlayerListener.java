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
        for (int i = 1; i < parts.length; i++) {
            if (menu.isAllowTargetPlayer() && parts[i].toLowerCase().startsWith("-p:")) {
                String targetName = parts[i].substring(3);
                target = Bukkit.getPlayer(targetName);
                if (target == null && menu.isAllowOfflineTarget()) {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                    if (offline.hasPlayedBefore()) {
                        target = offline;
                    }
                }
                if (target == null) {
                    player.sendMessage(Util.parse("&cPlayer '" + targetName + "' not found."));
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

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        plugin.getLastOpenedMenus().remove(playerId);

        MenuHolder holder = plugin.getMenuRegistry().getOpenMenu(playerId).orElse(null);
        if (holder == null) {
            return;
        }

        holder.stopUpdateTask();
        plugin.getMenuRegistry().trackClose(playerId);
    }
}
