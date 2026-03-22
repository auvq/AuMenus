package me.auvq.aumenus.action;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ActionExecutor {
    void execute(@NotNull Player player, @NotNull String value);
}
