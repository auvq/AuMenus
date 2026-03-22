package me.auvq.aumenus.menu;

import lombok.Getter;
import lombok.Setter;
import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public final class MenuHolder implements InventoryHolder {

    private final @NotNull Menu menu;
    private final @NotNull UUID playerId;
    private final @NotNull Inventory inventory;
    private final @NotNull Map<String, String> arguments;
    private final @NotNull Map<Integer, MenuItem> activeItems;

    @Setter
    private int currentPage;
    @Setter
    private boolean updating;
    @Setter
    private long lastClickTime;
    @Setter
    private boolean reloading;

    private ScheduledTask updateTask;

    public MenuHolder(@NotNull Menu menu, @NotNull Player player, @NotNull Map<String, String> arguments) {
        this.menu = menu;
        this.playerId = player.getUniqueId();
        this.arguments = arguments;
        this.activeItems = new HashMap<>();
        this.currentPage = 1;
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

    public @Nullable MenuItem getActiveItem(int slot) {
        return activeItems.get(slot);
    }

    public void setActiveItem(int slot, @Nullable MenuItem item) {
        if (item == null) {
            activeItems.remove(slot);
        } else {
            activeItems.put(slot, item);
        }
    }

    public void startUpdateTask(@NotNull AuMenus plugin) {
        if (menu.getUpdateInterval() <= 0) {
            return;
        }

        Player player = getPlayer();
        if (player == null) {
            return;
        }

        this.updateTask = player.getScheduler().runAtFixedRate(plugin, task -> {
            Player onlinePlayer = getPlayer();
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                stopUpdateTask();
                return;
            }
            plugin.getMenuRenderer().refreshUpdatableItems(this);
        }, null, menu.getUpdateInterval(), menu.getUpdateInterval());
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public int getMaxPage() {
        if (!menu.isPaginated() || menu.getPageSlots().isEmpty()) {
            return 1;
        }
        return Math.max(1,
                (int) Math.ceil((double) menu.getPageItems().size() / menu.getPageSlots().size()));
    }

    private @NotNull String resolveTitle(@NotNull Player player) {
        String title = menu.getTitle();

        for (Map.Entry<String, String> arg : arguments.entrySet()) {
            title = title.replace("{" + arg.getKey() + "}", arg.getValue());
        }

        title = title.replace("{page}", String.valueOf(currentPage));
        title = title.replace("{max_page}", String.valueOf(getMaxPage()));
        title = title.replace("{player}", player.getName());

        if (AuMenus.getInstance().getHookProvider().isPapiEnabled()) {
            title = AuMenus.getInstance().getHookProvider().papi().setPlaceholders(player, title);
        }

        return title;
    }
}
