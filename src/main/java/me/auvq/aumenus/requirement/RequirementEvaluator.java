package me.auvq.aumenus.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@FunctionalInterface
public interface RequirementEvaluator {
    boolean evaluate(@NotNull Player player, @NotNull Map<String, Object> config);
}
