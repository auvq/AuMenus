package me.auvq.aumenus.action;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.menu.MenuHolder;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class ActionRegistry {

    private final Map<String, ActionExecutor> executors = new HashMap<>();
    private final AuMenus plugin;

    public ActionRegistry(@NotNull AuMenus plugin) {
        this.plugin = plugin;
        for (ActionType type : ActionType.values()) {
            for (String alias : type.getAliases()) {
                executors.put(alias.toLowerCase(), type.getExecutor());
            }
        }
    }

    public void register(@NotNull String key, @NotNull ActionExecutor executor) {
        executors.put(key.toLowerCase(), executor);
    }

    public void executeActions(@NotNull Player player, @NotNull List<Action> actions) {
        boolean chanceGroupFired = false;
        for (Action action : actions) {
            if (action.hasChance()) {
                if (chanceGroupFired) {
                    continue;
                }
                double roll = ThreadLocalRandom.current().nextDouble(100.0);
                if (roll >= action.getChance()) {
                    continue;
                }
                chanceGroupFired = true;
            } else {
                chanceGroupFired = false;
            }

            if (action.hasDelay()) {
                player.getScheduler().runDelayed(plugin, task -> executeSingle(player, action),
                        null, action.getDelay());
            } else {
                executeSingle(player, action);
            }
        }
    }

    public void executeSingle(@NotNull Player player, @NotNull Action action) {
        if (!player.isOnline()) return;

        String type = action.getType().toLowerCase();
        String value = resolveActionPlaceholders(player, action.getValue());

        switch (type) {
            case "open", "openguimenu", "openmenu" -> {
                handleOpenMenu(player, value);
                return;
            }
            case "refresh" -> {
                handleRefresh(player);
                return;
            }
            case "meta" -> {
                plugin.getMetaStore().executeMetaAction(player, value);
                return;
            }
            case "prev_page" -> {
                handlePageChange(player, -1);
                return;
            }
            case "next_page" -> {
                handlePageChange(player, 1);
                return;
            }
            case "anvil_input" -> {
                handleAnvilInput(player, action);
                return;
            }
            case "chat_input" -> {
                handleChatInput(player, action);
                return;
            }
        }

        ActionExecutor executor = executors.get(type);
        if (executor == null) {
            plugin.getLogger().warning("Unknown action type: " + action.getType());
            return;
        }

        try {
            executor.execute(player, value);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Failed to execute action '" + action.getType()
                    + "': " + e.getMessage());
        }
    }

    private void handleOpenMenu(@NotNull Player player, @NotNull String value) {
        String[] parts = value.split("\\s+", 2);
        String menuName = parts[0];

        Optional<Menu> found = plugin.getMenuRegistry().findByName(menuName);
        if (found.isEmpty()) {
            return;
        }

        Menu menu = found.get();
        Map<String, String> args = parseOpenMenuArgs(menu, parts);

        MenuHolder current = plugin.getMenuRegistry().getOpenMenu(player.getUniqueId()).orElse(null);
        if (current != null) {
            current.setReloading(true);
        }

        plugin.openMenu(player, menu, args);
    }

    private @NotNull Map<String, String> parseOpenMenuArgs(@NotNull Menu menu, @NotNull String[] parts) {
        Map<String, String> args = new HashMap<>();
        if (parts.length <= 1) {
            return args;
        }

        String[] argValues = parts[1].split("\\s+");
        List<String> argNames = menu.getArgs();
        for (int i = 0; i < Math.min(argNames.size(), argValues.length); i++) {
            String sanitized = MiniMessage.miniMessage()
                    .escapeTags(argValues[i]);
            args.put(argNames.get(i), sanitized);
        }
        return args;
    }

    private void handleRefresh(@NotNull Player player) {
        MenuHolder holder = plugin.getMenuRegistry().getOpenMenu(player.getUniqueId()).orElse(null);
        if (holder == null) {
            debugLog("Refresh: no tracked menu for " + player.getName());
            return;
        }
        debugLog("Refresh: re-rendering '" + holder.getMenu().getName() + "' for " + player.getName());
        plugin.getMenuRenderer().render(holder);
    }

    private void debugLog(@NotNull String message) {
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("[Debug] " + message);
        }
    }

    private void handlePageChange(@NotNull Player player, int delta) {
        MenuHolder holder = plugin.getMenuRegistry().getOpenMenu(player.getUniqueId()).orElse(null);
        if (holder == null) {
            return;
        }

        Menu menu = holder.getMenu();
        if (!menu.isPaginated()) {
            return;
        }

        int newPage = holder.getCurrentPage() + delta;
        if (newPage < 1 || newPage > holder.getMaxPage()) {
            return;
        }

        holder.setCurrentPage(newPage);

        boolean titleHasPagePlaceholder = menu.getTitle().contains("{page}")
                || menu.getTitle().contains("{max_page}");
        if (titleHasPagePlaceholder) {
            holder.stopUpdateTask();
            holder.setReloading(true);
            MenuHolder newHolder = new MenuHolder(menu, player, holder.getTarget(), holder.getArguments(), newPage);
            plugin.getMenuRenderer().render(newHolder);
            plugin.getMenuRegistry().trackOpen(player.getUniqueId(), newHolder);
            player.openInventory(newHolder.getInventory());
            newHolder.startUpdateTask(plugin);
        } else {
            plugin.getMenuRenderer().renderPage(holder);
        }
    }

    public @NotNull List<Action> parseActions(@Nullable List<?> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }

        List<Action> actions = new ArrayList<>();
        for (Object entry : rawList) {
            Action parsed = parseAction(entry);
            if (parsed != null) {
                actions.add(parsed);
            }
        }
        return actions;
    }

    @SuppressWarnings("unchecked")
    private @Nullable Action parseAction(@NotNull Object entry) {
        if (entry instanceof String str) {
            String[] parts = str.split("\\s+", 2);
            return new Action(parts[0], parts.length > 1 ? parts[1] : "");
        }
        if (!(entry instanceof Map<?, ?> map)) {
            return null;
        }

        int delay = map.containsKey("delay") ? ((Number) map.get("delay")).intValue() : 0;
        double chance = map.containsKey("chance") ? ((Number) map.get("chance")).doubleValue() : 100.0;

        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = e.getKey().toString();
            if (key.equals("delay") || key.equals("chance")) {
                continue;
            }

            Object rawValue = e.getValue();

            if (rawValue instanceof Map<?, ?> nestedMap
                    && (key.equals("anvil_input") || key.equals("chat_input"))) {
                Map<String, Object> configMap = (Map<String, Object>) nestedMap;
                return new Action(key, "", delay, chance, configMap);
            }

            String value = rawValue != null ? rawValue.toString() : "";
            return new Action(key, value, delay, chance);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void handleAnvilInput(@NotNull Player player, @NotNull Action action) {
        Map<String, Object> config = action.getConfigMap();
        if (config == null) {
            plugin.getLogger().warning("anvil_input action missing config map");
            return;
        }

        String title = config.containsKey("title") ? config.get("title").toString() : "Input";
        String placeholder = config.containsKey("placeholder") ? config.get("placeholder").toString() : "";
        List<Action> onSubmit = parseActions((List<?>) config.get("on_submit"));
        List<Action> onCancel = parseActions((List<?>) config.get("on_cancel"));
        Map<String, Object> requireConfig = config.containsKey("require")
                ? (Map<String, Object>) config.get("require") : null;

        plugin.getAnvilInput().requestInput(player, title, placeholder, onSubmit, onCancel, requireConfig);
    }

    private void handleChatInput(@NotNull Player player, @NotNull Action action) {
        Map<String, Object> config = action.getConfigMap();
        if (config == null) {
            plugin.getLogger().warning("chat_input action missing config map");
            return;
        }

        String prompt = config.containsKey("prompt") ? config.get("prompt").toString() : "Enter input:";
        int timeout = config.containsKey("timeout") ? ((Number) config.get("timeout")).intValue() : 0;
        String cancelWord = config.containsKey("cancel_word") ? config.get("cancel_word").toString() : "cancel";
        List<Action> onSubmit = parseActions((List<?>) config.get("on_submit"));
        List<Action> onCancel = parseActions((List<?>) config.get("on_cancel"));
        List<Action> onTimeout = parseActions((List<?>) config.get("on_timeout"));

        plugin.getChatInput().requestInput(player, prompt, timeout, cancelWord, onSubmit, onCancel, onTimeout);
    }

    private @NotNull String resolveActionPlaceholders(@NotNull Player player, @NotNull String value) {
        if (value.isEmpty()) {
            return value;
        }

        MenuHolder holder = plugin.getMenuRegistry().getOpenMenu(player.getUniqueId()).orElse(null);
        String result = replaceHolderArgs(holder, value);

        if (holder != null) {
            result = result.replace("{target}", holder.getTargetName());
        }

        OfflinePlayer placeholderTarget = holder != null ? holder.getPlaceholderTarget() : player;
        if (placeholderTarget instanceof Player onlineTarget) {
            result = Util.resolveBuiltInPlaceholders(onlineTarget, result);
        }

        if (plugin.getHookProvider().isPapiEnabled() && result.contains("%")) {
            result = plugin.getHookProvider().papi().setPlaceholders(placeholderTarget, result);
        }
        return result;
    }

    private @NotNull String replaceHolderArgs(@Nullable MenuHolder holder, @NotNull String value) {
        if (holder == null || !value.contains("{")) {
            return value;
        }

        String result = value;
        for (Map.Entry<String, String> arg : holder.getArguments().entrySet()) {
            result = result.replace("{" + arg.getKey() + "}", arg.getValue());
        }
        return result;
    }
}
