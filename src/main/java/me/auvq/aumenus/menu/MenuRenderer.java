package me.auvq.aumenus.menu;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.item.MenuItemFrame;
import me.auvq.aumenus.item.PlayerListTemplate;
import me.auvq.aumenus.util.ItemBuilder;
import me.auvq.aumenus.requirement.Requirement;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.requirement.RequirementRegistry;
import me.auvq.aumenus.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
            if (item.isProgress()) {
                renderProgressItem(player, item, holder, inventory);
            } else if (item.hasFrames()) {
                holder.getFrameIndices().put(slot, 0);
                holder.getFrameLastUpdate().put(slot, player.getWorld().getGameTime());
                List<ItemStack> builtFrames = new ArrayList<>();
                for (MenuItemFrame frame : item.getFrames()) {
                    builtFrames.add(itemBuilder.buildFrameItemStack(player, item, frame, holder));
                }
                holder.getCachedFrameStacks().put(slot, builtFrames);
                inventory.setItem(slot, builtFrames.getFirst());
            } else {
                inventory.setItem(slot, itemBuilder.buildItemStack(player, item, holder));
            }
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
        int itemsPerPage = pageSlots.size();
        int startIndex = (holder.getCurrentPage() - 1) * itemsPerPage;

        List<MenuItem> pageItems;
        if (menu.getPlayerListTemplate() != null) {
            pageItems = generatePlayerListItems(menu.getPlayerListTemplate(), startIndex, itemsPerPage);
        } else {
            pageItems = menu.getPageItems();
        }

        for (int i = 0; i < pageSlots.size(); i++) {
            int slot = pageSlots.get(i);
            int itemIndex = menu.getPlayerListTemplate() != null ? i : startIndex + i;

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

    private @NotNull List<MenuItem> generatePlayerListItems(@NotNull PlayerListTemplate template,
                                                              int startIndex, int maxItems) {
        List<? extends Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int endIndex = Math.min(startIndex + maxItems, onlinePlayers.size());
        if (startIndex >= onlinePlayers.size()) {
            return List.of();
        }

        List<MenuItem> items = new ArrayList<>(endIndex - startIndex);
        for (int i = startIndex; i < endIndex; i++) {
            Player onlinePlayer = onlinePlayers.get(i);
            String material = resolvePlayerListPlaceholders(template.getMaterial(), onlinePlayer);
            String displayName = template.getDisplayName() != null
                    ? resolvePlayerListPlaceholders(template.getDisplayName(), onlinePlayer)
                    : null;
            List<String> lore = template.getLore() != null
                    ? template.getLore().stream()
                        .map(line -> resolvePlayerListPlaceholders(line, onlinePlayer))
                        .toList()
                    : null;

            items.add(MenuItem.builder()
                    .name("player_list_" + onlinePlayer.getName())
                    .material(material)
                    .displayName(displayName)
                    .lore(lore)
                    .amount(1)
                    .slots(List.of())
                    .priority(0)
                    .configOrder(0)
                    .update(false)
                    .clickActions(template.getClickActions())
                    .leftClickActions(template.getLeftClickActions())
                    .rightClickActions(template.getRightClickActions())
                    .build());
        }
        return items;
    }

    private @NotNull String resolvePlayerListPlaceholders(@NotNull String text, @NotNull Player onlinePlayer) {
        String result = Util.resolveBuiltInPlaceholders(onlinePlayer, text);
        if (plugin.getHookProvider().isPapiEnabled() && result.contains("%")) {
            result = plugin.getHookProvider().papi().setPlaceholders(onlinePlayer, result);
        }
        return result;
    }

    public void refreshUpdatableItems(@NotNull MenuHolder holder) {
        Player player = holder.getPlayer();
        if (player == null) {
            return;
        }

        holder.setUpdating(true);
        try {
            List<MenuItem> refreshedProgress = new ArrayList<>();
            for (Map.Entry<Integer, MenuItem> entry : holder.getActiveItems().entrySet()) {
                MenuItem item = entry.getValue();
                if (!item.isUpdate()) {
                    continue;
                }
                if (item.isProgress()) {
                    if (refreshedProgress.contains(item)) {
                        continue;
                    }
                    refreshedProgress.add(item);
                    renderProgressItem(player, item, holder, holder.getInventory());
                } else {
                    int slot = entry.getKey();
                    ItemStack built = itemBuilder.buildItemStack(player, item, holder);
                    ItemStack current = holder.getInventory().getItem(slot);
                    if (!built.equals(current)) {
                        holder.getInventory().setItem(slot, built);
                    }
                }
            }
        } finally {
            holder.setUpdating(false);
        }
    }

    public void refreshAnimatedItems(@NotNull MenuHolder holder) {
        Player player = holder.getPlayer();
        if (player == null) {
            return;
        }

        long currentTick = player.getWorld().getGameTime();

        for (int slot : holder.getAnimatedSlots()) {
            MenuItem item = holder.getActiveItem(slot);
            if (item == null || !item.hasFrames()) {
                continue;
            }

            int frameCount = item.getFrames().size();
            long lastUpdate = holder.getFrameLastUpdate().getOrDefault(slot, 0L);

            if (currentTick - lastUpdate < item.getFrameInterval()) {
                continue;
            }

            int currentIndex = holder.getFrameIndices().getOrDefault(slot, 0);
            int nextIndex = advanceFrame(currentIndex, frameCount, item.isFrameReverse(), item.isFrameLoop());

            if (nextIndex == currentIndex && holder.getFrameLastUpdate().containsKey(slot)) {
                continue;
            }

            holder.getFrameIndices().put(slot, nextIndex);
            holder.getFrameLastUpdate().put(slot, currentTick);

            List<ItemStack> cached = holder.getCachedFrameStacks().get(slot);
            if (cached != null && nextIndex < cached.size()) {
                holder.getInventory().setItem(slot, cached.get(nextIndex));
            }
        }
    }

    private int advanceFrame(int current, int frameCount, boolean reverse, boolean loop) {
        if (reverse) {
            if (current <= 0) {
                return loop ? frameCount - 1 : 0;
            }
            return current - 1;
        }

        if (current >= frameCount - 1) {
            return loop ? 0 : frameCount - 1;
        }
        return current + 1;
    }

    private void renderProgressItem(@NotNull Player player,
                                      @NotNull MenuItem item,
                                      @NotNull MenuHolder holder,
                                      @NotNull Inventory inventory) {
        List<Integer> slots = item.getSlots();
        if (slots.isEmpty()) {
            return;
        }

        int totalSlots = slots.size();
        int filledCount = calculateFilledSlots(player, item, holder, totalSlots);

        for (int i = 0; i < totalSlots; i++) {
            int slot = slots.get(i);
            boolean filled = i < filledCount;
            MenuItemFrame frame = filled ? item.getProgressFilled() : item.getProgressEmpty();
            ItemStack stack;
            if (frame != null) {
                stack = itemBuilder.buildFrameItemStack(player, item, frame, holder);
            } else {
                stack = itemBuilder.buildItemStack(player, item, holder);
            }
            inventory.setItem(slot, stack);
        }
    }

    private int calculateFilledSlots(@NotNull Player player,
                                      @NotNull MenuItem item,
                                      @NotNull MenuHolder holder,
                                      int totalSlots) {
        double current = resolveProgressValue(player, item.getProgressCurrent(), holder);
        double max = resolveProgressValue(player, item.getProgressMax(), holder);
        if (max <= 0) {
            return 0;
        }
        double ratio = Math.max(0.0, Math.min(1.0, current / max));
        return (int) Math.round(ratio * totalSlots);
    }

    private double resolveProgressValue(@NotNull Player player,
                                         @NotNull String value,
                                         @NotNull MenuHolder holder) {
        String resolved = resolvePlaceholders(player, value, holder);
        try {
            return Double.parseDouble(resolved);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private @Nullable MenuItem resolveItem(@NotNull Player player,
                                            @NotNull List<MenuItem> candidates,
                                            @NotNull MenuHolder holder) {
        MenuItem fallback = null;
        boolean hasConditionalItems = false;
        for (MenuItem candidate : candidates) {
            if (candidate.getViewRequire() != null) {
                hasConditionalItems = true;
                break;
            }
        }

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
