package me.auvq.aumenus.item;

import lombok.Builder;
import lombok.Getter;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.requirement.RequirementList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Builder
public final class MenuItem {

    private final @NotNull String name;
    private final @NotNull String material;
    private final @Nullable String displayName;
    private final @Nullable List<String> lore;
    private final int amount;
    private final @Nullable String dynamicAmount;
    private final @NotNull List<Integer> slots;
    private final int priority;
    private final boolean update;

    private final @Nullable List<String> enchantments;
    private final @Nullable Boolean enchantmentGlintOverride;
    private final @Nullable Boolean hideTooltip;
    private final @Nullable String rarity;
    private final @Nullable List<String> itemFlags;
    private final boolean unbreakable;
    private final @Nullable Integer modelData;
    private final @Nullable String itemModel;
    private final @Nullable String tooltipStyle;
    private final @Nullable List<String> bannerMeta;
    private final @Nullable String baseColor;
    private final @Nullable Integer lightLevel;
    private final @Nullable String trimMaterial;
    private final @Nullable String trimPattern;
    private final @Nullable List<String> potionEffects;
    private final @Nullable String rgb;
    private final @Nullable Integer damage;
    private final @Nullable String loreAppendMode;

    @Builder.Default
    private final @NotNull List<Action> clickActions = List.of();
    @Builder.Default
    private final @NotNull List<Action> leftClickActions = List.of();
    @Builder.Default
    private final @NotNull List<Action> rightClickActions = List.of();
    @Builder.Default
    private final @NotNull List<Action> shiftLeftClickActions = List.of();
    @Builder.Default
    private final @NotNull List<Action> shiftRightClickActions = List.of();
    @Builder.Default
    private final @NotNull List<Action> middleClickActions = List.of();

    private final @Nullable RequirementList clickRequire;
    private final @Nullable RequirementList leftClickRequire;
    private final @Nullable RequirementList rightClickRequire;
    private final @Nullable RequirementList shiftLeftClickRequire;
    private final @Nullable RequirementList shiftRightClickRequire;
    private final @Nullable RequirementList middleClickRequire;

    private final @Nullable RequirementList viewRequire;

    private final @Nullable String errorMessage;

    public boolean hasError() {
        return errorMessage != null;
    }
}
