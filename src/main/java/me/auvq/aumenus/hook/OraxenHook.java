package me.auvq.aumenus.hook;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OraxenHook {

    private OraxenHook() {
    }

    public static @Nullable ItemStack getItem(@NotNull String id) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) {
            return null;
        }
        ItemStack stack = builder.build();
        return stack != null ? stack.clone() : null;
    }
}
