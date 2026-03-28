package me.auvq.aumenus.menu;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.util.ItemBuilder;
import me.auvq.aumenus.requirement.Requirement;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.requirement.RequirementRegistry;
import me.auvq.aumenus.util.Util;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MenuRenderer {

    private final AuMenus plugin;
    private final RequirementRegistry requirementRegistry;
    private final ItemBuilder itemBuilder;

    public MenuRenderer(@NotNull AuMenus plugin, @NotNull RequirementRegistry requirementRegistry) {
        this.plugin = plugin;
        this.requirementRegistry = requirementRegistry;
        this.itemBuilder = new ItemBuilder(plugin);
    }

    public void render(@NotNull MenuHolder holder) {
        Player player = holder.getPlayer();
        if (player == null) {
            return;
        }

        Menu menu = holder.getMenu();
        Inventory inventory = holder.getInventory();

        for (Map.Entry<Integer, List<MenuItem>> entry : menu.getItems().entrySet()) {
            int slot = entry.getKey();
            MenuItem item = resolveItem(player, entry.getValue(), holder);
            if (item == null) {
                continue;
            }
            holder.setActiveItem(slot, item);
            inventory.setItem(slot, itemBuilder.buildItemStack(player, item, holder));
        }

        if (menu.isPaginated()) {
            renderPage(holder);
        }
    }

    public void renderPage(@NotNull MenuHolder holder) {
        Player player = holder.getPlayer();
        if (player == null) {
            return;
        }

        Menu menu = holder.getMenu();
        List<Integer> pageSlots = menu.getPageSlots();
        List<MenuItem> pageItems = menu.getPageItems();

        int itemsPerPage = pageSlots.size();
        int startIndex = (holder.getCurrentPage() - 1) * itemsPerPage;

        for (int i = 0; i < pageSlots.size(); i++) {
            int slot = pageSlots.get(i);
            int itemIndex = startIndex + i;

            if (itemIndex < pageItems.size()) {
                MenuItem item = pageItems.get(itemIndex);
                holder.setActiveItem(slot, item);
                holder.getInventory().setItem(slot, itemBuilder.buildItemStack(player, item, holder));
            } else {
                holder.setActiveItem(slot, null);
                holder.getInventory().setItem(slot, null);
            }
        }
    }

    public void refreshUpdatableItems(@NotNull MenuHolder holder) {
        Player player = holder.getPlayer();
        if (player == null) {
            return;
        }

        holder.setUpdating(true);
        try {
            for (Map.Entry<Integer, MenuItem> entry : holder.getActiveItems().entrySet()) {
                MenuItem item = entry.getValue();
                if (item.isUpdate()) {
                    holder.getInventory().setItem(entry.getKey(), itemBuilder.buildItemStack(player, item, holder));
                }
            }
        } finally {
            holder.setUpdating(false);
        }
    }

    private @Nullable MenuItem resolveItem(@NotNull Player player,
                                            @NotNull List<MenuItem> candidates,
                                            @NotNull MenuHolder holder) {
        MenuItem fallback = null;
        boolean hasConditionalItems = candidates.stream().anyMatch(i -> i.getViewRequire() != null);

        for (MenuItem item : candidates) {
            if (item.getViewRequire() == null) {
                if (hasConditionalItems) {
                    fallback = item;
                    continue;
                }
                return item;
            }
            RequirementList resolved = resolveRequirementPlaceholders(item.getViewRequire(), player, holder);
            if (resolved.evaluate(player, requirementRegistry)) {
                return item;
            }
        }
        return fallback;
    }

    public @NotNull RequirementList resolveRequirementPlaceholders(@NotNull RequirementList list,
                                                                      @NotNull Player player,
                                                                      @NotNull MenuHolder holder) {
        if (!containsPlaceholders(list)) {
            return list;
        }

        List<Requirement> resolvedRequirements = new ArrayList<>();
        for (Requirement req : list.getRequirements()) {
            Map<String, Object> resolvedConfig = resolveConfig(req.getConfig(), player, holder);
            resolvedRequirements.add(Requirement.builder()
                    .name(req.getName())
                    .type(req.getType())
                    .optional(req.isOptional())
                    .config(resolvedConfig)
                    .denyActions(req.getDenyActions())
                    .successActions(req.getSuccessActions())
                    .build());
        }
        return RequirementList.builder()
                .requirements(resolvedRequirements)
                .minimumRequired(list.getMinimumRequired())
                .stopAtSuccess(list.isStopAtSuccess())
                .denyActions(list.getDenyActions())
                .successActions(list.getSuccessActions())
                .build();
    }

    private boolean containsPlaceholders(@NotNull RequirementList list) {
        for (Requirement req : list.getRequirements()) {
            for (Object value : req.getConfig().values()) {
                if (value instanceof String str && (str.contains("{") || str.contains("%"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private @NotNull Map<String, Object> resolveConfig(@NotNull Map<String, Object> config,
                                                         @NotNull Player player,
                                                         @NotNull MenuHolder holder) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str) {
                resolved.put(entry.getKey(), resolvePlaceholders(player, str, holder));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    private @NotNull String resolvePlaceholders(@NotNull Player player,
                                                 @NotNull String text,
                                                 @NotNull MenuHolder holder) {
        return Util.resolvePlaceholders(player, text, holder, plugin.getHookProvider());
    }
}
