package me.auvq.aumenus.listener;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.menu.Menu;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerListener implements Listener {

    private final AuMenus plugin;

    public PlayerListener(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
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

        Map<String, String> args = new HashMap<>();
        for (int i = 0; i < Math.min(menu.getArgs().size(), parts.length - 1); i++) {
            String sanitized = MiniMessage.miniMessage().escapeTags(parts[i + 1]);
            args.put(menu.getArgs().get(i), sanitized);
        }

        plugin.openMenu(player, menu, args);
    }
}
