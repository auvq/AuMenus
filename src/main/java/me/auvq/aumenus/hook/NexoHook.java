package me.auvq.aumenus.hook;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NexoHook {

    private NexoHook() {}

    public static @Nullable ItemStack getItem(@NotNull String id) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) {
            return null;
        }
        return builder.build();
    }

    public static @Nullable String idFromItem(@NotNull ItemStack item) {
        try {
            if (!NexoItems.exists(item)) {
                return null;
            }
            return NexoItems.idFromItem(item);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
