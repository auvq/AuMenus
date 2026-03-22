package me.auvq.aumenus.menu;

import lombok.Builder;
import lombok.Getter;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.requirement.RequirementList;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public final class Menu {

    private final @NotNull String name;
    private final @NotNull String title;
    private final int size;
    @Builder.Default
    private final @NotNull InventoryType inventoryType = InventoryType.CHEST;
    private final @Nullable String command;
    @Builder.Default
    private final boolean registerCommand = true;
    private final int updateInterval;

    @Builder.Default
    private final @NotNull List<String> args = List.of();
    private final @Nullable String argsUsage;
    private final @Nullable Map<String, RequirementList> argRequirements;

    private final @Nullable RequirementList openRequire;
    @Builder.Default
    private final @NotNull List<Action> onOpen = List.of();
    @Builder.Default
    private final @NotNull List<Action> onClose = List.of();

    @Builder.Default
    private final @NotNull Map<Integer, List<MenuItem>> items = Map.of();

    private final boolean paginated;
    @Builder.Default
    private final @NotNull List<Integer> pageSlots = List.of();
    @Builder.Default
    private final @NotNull List<MenuItem> pageItems = List.of();

    @Builder.Default
    private final @NotNull String sourceFile = "";

    @Builder.Default
    private final @NotNull List<String> loadErrors = List.of();

    public boolean hasErrors() {
        return !loadErrors.isEmpty();
    }
}
