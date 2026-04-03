package me.auvq.aumenus.item;

import lombok.Builder;
import lombok.Getter;
import me.auvq.aumenus.action.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Builder
public final class PlayerListTemplate {

    private final @NotNull String material;
    private final @Nullable String displayName;
    private final @Nullable List<String> lore;
    @Builder.Default
    private final @NotNull List<Action> clickActions = List.of();
    @Builder.Default
    private final @NotNull List<Action> leftClickActions = List.of();
    @Builder.Default
    private final @NotNull List<Action> rightClickActions = List.of();
}
