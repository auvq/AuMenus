package me.auvq.aumenus.action;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Getter
public final class Action {

    private final @NotNull String type;
    private final @NotNull String value;
    private final int delay;
    private final double chance;
    private final @Nullable Map<String, Object> configMap;

    public Action(@NotNull String type, @NotNull String value, int delay, double chance,
                  @Nullable Map<String, Object> configMap) {
        this.type = type;
        this.value = value;
        this.delay = delay;
        this.chance = chance;
        this.configMap = configMap;
    }

    public Action(@NotNull String type, @NotNull String value, int delay, double chance) {
        this(type, value, delay, chance, null);
    }

    public Action(@NotNull String type, @NotNull String value) {
        this(type, value, 0, 100.0, null);
    }

    public boolean hasDelay() {
        return delay > 0;
    }

    public boolean hasChance() {
        return chance < 100.0;
    }
}
