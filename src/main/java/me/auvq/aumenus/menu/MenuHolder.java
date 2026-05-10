package me.auvq.aumenus.menu;

import lombok.Getter;
import lombok.Setter;
import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.util.InventoryUpdater;
import me.auvq.aumenus.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public final class MenuHolder implements InventoryHolder {

    @Setter
    private @NotNull Menu menu;
    private final @NotNull UUID playerId;
    @Setter
    private @Nullable OfflinePlayer target;
    private final @NotNull Inventory inventory;
    @Setter
    private @NotNull Map<String, String> arguments;
    private final @NotNull Map<Integer, MenuItem> activeItems;
    private final @NotNull Map<Integer, Integer> frameIndices;
    private final @NotNull Map<Integer, Long> frameLastUpdate;
    private final @NotNull Map<Integer, List<ItemStack>> cachedFrameStacks;
    private final @NotNull List<Integer> animatedSlots = new ArrayList<>();

    private boolean hasAnimatedItems = false;
    private int animatedCount = 0;
    @Setter
    private @Nullable String lastRenderedTitle;

    @Setter
    private int currentPage;
    @Setter
    private volatile boolean updating;
    @Setter
    private long lastClickTime;
    @Setter
    private volatile boolean reloading;
    @Setter
    private long lastUpdateTime;


    public MenuHolder(@NotNull Menu menu, @NotNull Player player, @NotNull Map<String, String> arguments) {
        this(menu, player, null, arguments, 1);
    }

    public MenuHolder(@NotNull Menu menu, @NotNull Player player, @Nullable OfflinePlayer target,
                       @NotNull Map<String, String> arguments) {
        this(menu, player, target, arguments, 1);
    }

    public MenuHolder(@NotNull Menu menu, @NotNull Player player, @Nullable OfflinePlayer target,
                       @NotNull Map<String, String> arguments, int initialPage) {
        this.menu = menu;
        this.playerId = player.getUniqueId();
        this.target = target;
        this.arguments = arguments;
        int capacity = (int) Math.ceil(menu.getSize() / 0.75);
        this.activeItems = new HashMap<>(capacity);
        this.frameIndices = new HashMap<>(capacity);
        this.frameLastUpdate = new HashMap<>(capacity);
        this.cachedFrameStacks = new HashMap<>(capacity);
        this.currentPage = initialPage;
        this.updating = false;
        this.lastClickTime = 0;

        String resolvedTitle = resolveTitle(player);
        if (menu.getInventoryType() == InventoryType.CHEST) {
            this.inventory = Bukkit.createInventory(this, menu.getSize(), Util.parse(resolvedTitle));
        } else {
            this.inventory = Bukkit.createInventory(this, menu.getInventoryType(), Util.parse(resolvedTitle));
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public @Nullable OfflinePlayer getTarget() {
        return target;
    }

    public @NotNull OfflinePlayer getPlaceholderTarget() {
        OfflinePlayer resolved = getTarget();
        if (resolved != null) {
            return resolved;
        }
        Player player = getPlayer();
        if (player != null) {
            return player;
        }
        throw new IllegalStateException("No valid player for placeholder resolution");
    }

    public @NotNull String getTargetName() {
        OfflinePlayer resolved = getTarget();
        if (resolved != null && resolved.getName() != null) {
            return resolved.getName();
        }
        Player player = getPlayer();
        return player != null ? player.getName() : "Unknown";
    }

    public @Nullable MenuItem getActiveItem(int slot) {
        return activeItems.get(slot);
    }

    public void setActiveItem(int slot, @Nullable MenuItem item) {
        if (item == null) {
            MenuItem removed = activeItems.remove(slot);
            frameIndices.remove(slot);
            frameLastUpdate.remove(slot);
            cachedFrameStacks.remove(slot);
            if (animatedSlots.remove(Integer.valueOf(slot)) && removed != null && removed.hasFrames()) {
                animatedCount--;
                hasAnimatedItems = animatedCount > 0;
            }
        } else {
            activeItems.put(slot, item);
            if (item.hasFrames()) {
                animatedCount++;
                hasAnimatedItems = true;
                if (!animatedSlots.contains(slot)) {
                    animatedSlots.add(slot);
                }
            }
        }
    }

    public boolean hasAnimatedItems() {
        return hasAnimatedItems;
    }

    public int getMaxPage() {
        if (!menu.isPaginated() || menu.getPageSlots().isEmpty()) {
            return 1;
        }
        int totalItems = menu.getPlayerListTemplate() != null
                ? Bukkit.getOnlinePlayers().size()
                : menu.getPageItems().size();
        return Math.max(1,
                (int) Math.ceil((double) totalItems / menu.getPageSlots().size()));
    }

    public void updateTitle(@NotNull Player player) {
        String resolvedTitle = resolveTitle(player);
        if (resolvedTitle.equals(lastRenderedTitle)) {
            return;
        }
        lastRenderedTitle = resolvedTitle;
        Component titleComponent = Util.parse(resolvedTitle);
        if (InventoryUpdater.sendSmoothUpdate(player, inventory, titleComponent, resolvedTitle)) {
            return;
        }
        String legacyTitle = LegacyComponentSerializer.legacySection().serialize(titleComponent);
        player.getOpenInventory().setTitle(legacyTitle);
    }

    public void resetForSwap() {
        activeItems.clear();
        frameIndices.clear();
        frameLastUpdate.clear();
        cachedFrameStacks.clear();
        animatedSlots.clear();
        animatedCount = 0;
        hasAnimatedItems = false;
        lastRenderedTitle = null;
        currentPage = 1;
        lastUpdateTime = 0L;
    }

    public @NotNull String resolveTitle(@NotNull Player player) {
        String title = menu.getTitle();

        for (Map.Entry<String, String> arg : arguments.entrySet()) {
            title = title.replace("{" + arg.getKey() + "}", arg.getValue());
        }

        title = title.replace("{page}", String.valueOf(currentPage));
        title = title.replace("{max_page}", String.valueOf(getMaxPage()));
        title = title.replace("{player}", player.getName());
        title = title.replace("{target}", getTargetName());

        OfflinePlayer papiTarget = getPlaceholderTarget();
        if (AuMenus.getInstance().getHookProvider().isPapiEnabled()) {
            title = AuMenus.getInstance().getHookProvider().papi().setPlaceholders(papiTarget, title);
        }

        return title;
    }
}
