package me.auvq.aumenus.input;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.action.ActionRegistry;
import me.auvq.aumenus.util.Util;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInput implements Listener {

    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();
    private final AuMenus plugin;

    public ChatInput(@NotNull AuMenus plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void requestInput(@NotNull Player player,
                             @NotNull String prompt,
                             int timeoutSeconds,
                             @NotNull String cancelWord,
                             @NotNull List<Action> onSubmit,
                             @NotNull List<Action> onCancel,
                             @NotNull List<Action> onTimeout) {
        UUID playerId = player.getUniqueId();
        cancel(playerId);

        player.sendMessage(Util.parse(prompt));

        ScheduledTask timeoutTask = null;
        if (timeoutSeconds > 0) {
            timeoutTask = player.getScheduler().runDelayed(plugin, task -> {
                PendingInput timedOut = pendingInputs.remove(playerId);
                if (timedOut == null) {
                    return;
                }
                plugin.getActionRegistry().executeActions(player, onTimeout);
            }, null, timeoutSeconds * 20L);
        }

        pendingInputs.put(playerId, new PendingInput(cancelWord, onSubmit, onCancel, timeoutTask));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(@NotNull AsyncChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PendingInput pending = pendingInputs.remove(playerId);
        if (pending == null) {
            return;
        }

        event.setCancelled(true);
        pending.cancelTimeout();

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Player player = event.getPlayer();
        ActionRegistry actionRegistry = plugin.getActionRegistry();

        if (message.equalsIgnoreCase(pending.cancelWord)) {
            player.getScheduler().run(plugin, task ->
                    actionRegistry.executeActions(player, pending.onCancel), null);
            return;
        }

        List<Action> resolved = InputActions.resolveInput(pending.onSubmit, message);
        player.getScheduler().run(plugin, task ->
                actionRegistry.executeActions(player, resolved), null);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }

    private void cancel(@NotNull UUID playerId) {
        PendingInput pending = pendingInputs.remove(playerId);
        if (pending == null) {
            return;
        }
        pending.cancelTimeout();
    }

    private record PendingInput(
            String cancelWord,
            List<Action> onSubmit,
            List<Action> onCancel,
            @Nullable ScheduledTask timeoutTask
    ) {
        void cancelTimeout() {
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
        }
    }
}
