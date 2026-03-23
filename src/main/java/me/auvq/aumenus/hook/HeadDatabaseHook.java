package me.auvq.aumenus.hook;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HeadDatabaseHook {

    private HeadDatabaseHook() {}

    public static @Nullable ItemStack getItem(@NotNull String id) {
        return new HeadDatabaseAPI().getItemHead(id);
    }
}
