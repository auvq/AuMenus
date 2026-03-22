package me.auvq.aumenus.input;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnvilInput {

    private final AuMenus plugin;

    public AnvilInput(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    public void requestInput(@NotNull Player player,
                             @NotNull String title,
                             @NotNull String placeholder,
                             @NotNull List<Action> onSubmit,
                             @NotNull List<Action> onCancel) {
        requestInput(player, title, placeholder, onSubmit, onCancel, null);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void requestInput(@NotNull Player player,
                             @NotNull String title,
                             @NotNull String placeholder,
                             @NotNull List<Action> onSubmit,
                             @NotNull List<Action> onCancel,
                             @Nullable Map<String, Object> requireConfig) {
        player.getScheduler().run(plugin, task -> {
            AnvilView anvilView = MenuType.ANVIL.create(player, Util.parse(title));

            ItemStack inputItem = new ItemStack(Material.PAPER);
            ItemMeta meta = inputItem.getItemMeta();
            if (meta != null) {
                meta.displayName(Util.parse(placeholder));
                inputItem.setItemMeta(meta);
            }
            anvilView.getTopInventory().setItem(0, inputItem);

            Listener listener = createListener(player, anvilView, placeholder, onSubmit, onCancel, requireConfig);
            Bukkit.getPluginManager().registerEvents(listener, plugin);

            player.openInventory(anvilView);
        }, null);
    }

    private @NotNull Listener createListener(@NotNull Player player,
                                              @NotNull AnvilView anvilView,
                                              @NotNull String placeholder,
                                              @NotNull List<Action> onSubmit,
                                              @NotNull List<Action> onCancel,
                                              @Nullable Map<String, Object> requireConfig) {
        return new Listener() {
            private boolean submitted = false;

            @EventHandler
            public void onClick(InventoryClickEvent event) {
                if (!(event.getView() instanceof AnvilView clickedView) || clickedView != anvilView) {
                    return;
                }
                event.setCancelled(true);

                if (event.getRawSlot() != 2) {
                    return;
                }

                String renameText = anvilView.getRenameText();
                String inputText = (renameText != null && !renameText.isEmpty()) ? renameText : placeholder;

                if (requireConfig != null && !validateInput(player, inputText, requireConfig)) {
                    return;
                }

                submitted = true;
                anvilView.getTopInventory().clear();
                player.closeInventory();
                HandlerList.unregisterAll(this);

                List<Action> resolved = resolveInputActions(onSubmit, inputText);
                player.getScheduler().run(plugin, t ->
                        plugin.getActionRegistry().executeActions(player, resolved), null);
            }

            @EventHandler
            public void onClose(InventoryCloseEvent event) {
                if (!(event.getView() instanceof AnvilView closedView) || closedView != anvilView) {
                    return;
                }
                if (submitted) {
                    return;
                }

                HandlerList.unregisterAll(this);
                player.getScheduler().run(plugin, t ->
                        plugin.getActionRegistry().executeActions(player, onCancel), null);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    return;
                }
                HandlerList.unregisterAll(this);
            }
        };
    }

    private boolean validateInput(@NotNull Player player, @NotNull String inputText,
                                   @NotNull Map<String, Object> requireConfig) {
        Map<String, Object> resolvedConfig = resolveInputInConfig(requireConfig, inputText);
        RequirementList requirementList = plugin.getRequirementRegistry().parseRequirementConfig(resolvedConfig);

        if (requirementList.evaluate(player, plugin.getRequirementRegistry())) {
            return true;
        }

        if (!requirementList.getDenyActions().isEmpty()) {
            List<Action> denyResolved = resolveInputActions(requirementList.getDenyActions(), inputText);
            plugin.getActionRegistry().executeActions(player, denyResolved);
        }
        return false;
    }

    private @NotNull List<Action> resolveInputActions(@NotNull List<Action> actions, @NotNull String inputText) {
        return actions.stream()
                .map(action -> new Action(
                        action.getType(),
                        action.getValue().replace("{input}", inputText),
                        action.getDelay(),
                        action.getChance()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private @NotNull Map<String, Object> resolveInputInConfig(@NotNull Map<String, Object> config,
                                                               @NotNull String inputText) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str) {
                resolved.put(entry.getKey(), str.replace("{input}", inputText));
            } else if (value instanceof Map<?, ?> nested) {
                resolved.put(entry.getKey(), resolveInputInConfig((Map<String, Object>) nested, inputText));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }
}
