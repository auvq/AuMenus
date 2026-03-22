package me.auvq.aumenus.menu;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuRegistry {

    private final Map<String, Menu> menusByName = new ConcurrentHashMap<>();
    private final Map<String, Menu> menusByCommand = new ConcurrentHashMap<>();
    private final Map<UUID, MenuHolder> openMenus = new ConcurrentHashMap<>();

    public void register(@NotNull Menu menu) {
        menusByName.put(menu.getName().toLowerCase(), menu);
        if (menu.getCommand() != null && !menu.getCommand().isEmpty()) {
            menusByCommand.put(menu.getCommand().toLowerCase(), menu);
        }
    }

    public void unregister(@NotNull String name) {
        Menu menu = menusByName.remove(name.toLowerCase());
        if (menu != null && menu.getCommand() != null) {
            menusByCommand.remove(menu.getCommand().toLowerCase());
        }
    }

    public void clear() {
        menusByName.clear();
        menusByCommand.clear();
    }

    public @NotNull Optional<Menu> findByName(@NotNull String name) {
        return Optional.ofNullable(menusByName.get(name.toLowerCase()));
    }

    public @NotNull Optional<Menu> findByCommand(@NotNull String command) {
        return Optional.ofNullable(menusByCommand.get(command.toLowerCase()));
    }

    public @NotNull Collection<Menu> all() {
        return menusByName.values();
    }

    public int size() {
        return menusByName.size();
    }

    public void trackOpen(@NotNull UUID playerId, @NotNull MenuHolder holder) {
        openMenus.put(playerId, holder);
    }

    public void trackClose(@NotNull UUID playerId) {
        openMenus.remove(playerId);
    }

    public @NotNull Optional<MenuHolder> getOpenMenu(@NotNull UUID playerId) {
        return Optional.ofNullable(openMenus.get(playerId));
    }

    public @NotNull Map<UUID, MenuHolder> getOpenMenus() {
        return openMenus;
    }
}
