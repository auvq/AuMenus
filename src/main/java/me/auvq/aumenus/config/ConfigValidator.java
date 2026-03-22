package me.auvq.aumenus.config;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ConfigValidator {

    private static final Set<Integer> VALID_CHEST_SIZES = Set.of(9, 18, 27, 36, 45, 54);
    private static final Set<String> VALID_INVENTORY_TYPES = Set.of(
            "CHEST", "BARREL", "ENDER_CHEST", "SHULKER_BOX",
            "HOPPER", "DISPENSER", "DROPPER",
            "FURNACE", "BLAST_FURNACE", "SMOKER", "BREWING",
            "WORKBENCH", "ANVIL"
    );

    private final List<String> errors = new ArrayList<>();
    private final String fileName;

    public ConfigValidator(@NotNull String fileName) {
        this.fileName = fileName;
    }

    public boolean validateSize(int size) {
        if (!VALID_CHEST_SIZES.contains(size)) {
            errors.add(fileName + ": Invalid chest size " + size + ". Must be 9, 18, 27, 36, 45, or 54.");
            return false;
        }
        return true;
    }

    public boolean validateInventoryType(@Nullable String type) {
        if (type == null) {
            return true;
        }
        if (!VALID_INVENTORY_TYPES.contains(type.toUpperCase())) {
            errors.add(fileName + ": Invalid inventory type '" + type + "'.");
            return false;
        }
        return true;
    }

    public boolean validateMaterial(@NotNull String materialStr, @NotNull String itemName) {
        if (isSpecialMaterial(materialStr)) {
            return true;
        }
        Material material = Material.matchMaterial(materialStr);
        if (material == null) {
            errors.add(fileName + ": Item '" + itemName + "' has invalid material '" + materialStr + "'.");
            return false;
        }
        return true;
    }

    public boolean validateSlot(int slot, int menuSize, @NotNull String itemName) {
        if (slot < 0 || slot >= menuSize) {
            errors.add(fileName + ": Item '" + itemName + "' slot " + slot
                    + " is out of range (0-" + (menuSize - 1) + ").");
            return false;
        }
        return true;
    }

    public void addError(@NotNull String error) {
        errors.add(fileName + ": " + error);
    }

    public @NotNull List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private static boolean isSpecialMaterial(@NotNull String material) {
        return material.startsWith("head-")
                || material.startsWith("basehead-")
                || material.startsWith("texture-")
                || material.startsWith("hdb-")
                || material.startsWith("placeholder-")
                || material.equals("air")
                || material.equals("water_bottle")
                || material.equals("main_hand")
                || material.equals("off_hand")
                || material.startsWith("armor_");
    }
}
