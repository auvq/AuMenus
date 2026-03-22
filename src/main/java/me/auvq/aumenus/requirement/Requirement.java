package me.auvq.aumenus.requirement;

import lombok.Builder;
import lombok.Getter;
import me.auvq.aumenus.action.Action;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public final class Requirement {

    private final @NotNull String name;
    private final @NotNull String type;
    private final boolean optional;
    private final @NotNull Map<String, Object> config;
    private final @NotNull List<Action> denyActions;
    private final @NotNull List<Action> successActions;
}
