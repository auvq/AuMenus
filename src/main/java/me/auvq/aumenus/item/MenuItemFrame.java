package me.auvq.aumenus.item;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Builder
public final class MenuItemFrame {

    private final @Nullable String material;
    private final @Nullable String displayName;
    private final @Nullable List<String> lore;
    private final @Nullable Integer amount;
    private final @Nullable List<String> enchantments;
    private final @Nullable Boolean enchantmentGlintOverride;
    private final @Nullable Boolean hideTooltip;
    private final @Nullable String rarity;
    private final @Nullable List<String> itemFlags;
    private final @Nullable Boolean unbreakable;
    private final @Nullable Integer modelData;
    private final @Nullable String itemModel;
    private final @Nullable String tooltipStyle;
    private final @Nullable String rgb;
    private final @Nullable Integer damage;
}
