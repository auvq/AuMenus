package me.auvq.aumenus.hook;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemsAdderHook {

    private ItemsAdderHook() {
    }

    public static @Nullable ItemStack getItem(@NotNull String id) {
        CustomStack stack = CustomStack.getInstance(id);
        if (stack == null) {
            return null;
        }
        ItemStack itemStack = stack.getItemStack();
        return itemStack != null ? itemStack.clone() : null;
    }
}
