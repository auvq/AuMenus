package me.auvq.aumenus.api;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.ActionExecutor;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.requirement.RequirementEvaluator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class AuMenusAPI {

    private AuMenusAPI() {}

    public static void openMenu(@NotNull Player player, @NotNull String menuName) {
        openMenu(player, menuName, Map.of());
    }

    public static void openMenu(@NotNull Player player, @NotNull String menuName,
                                 @NotNull Map<String, String> args) {
        AuMenus plugin = AuMenus.getInstance();
        Optional<Menu> menuOpt = plugin.getMenuRegistry().findByName(menuName);
        menuOpt.ifPresent(menu -> plugin.openMenu(player, menu, args));
    }

    public static @NotNull Optional<Menu> getMenu(@NotNull String name) {
        return AuMenus.getInstance().getMenuRegistry().findByName(name);
    }

    public static @NotNull Collection<Menu> getAllMenus() {
        return AuMenus.getInstance().getMenuRegistry().all();
    }

    public static boolean isInMenu(@NotNull Player player) {
        return AuMenus.getInstance().getMenuRegistry().getOpenMenu(player.getUniqueId()).isPresent();
    }

    public static void registerAction(@NotNull String key, @NotNull ActionExecutor executor) {
        AuMenus.getInstance().getActionRegistry().register(key, executor);
    }

    public static void registerRequirement(@NotNull String key, @NotNull RequirementEvaluator evaluator) {
        AuMenus.getInstance().getRequirementRegistry().register(key, evaluator);
    }
}
