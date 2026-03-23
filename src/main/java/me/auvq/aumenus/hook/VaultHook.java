package me.auvq.aumenus.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VaultHook {

    private @Nullable Economy economy;
    private @Nullable Permission permission;

    boolean setup() {
        RegisteredServiceProvider<Economy> ecoProvider =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (ecoProvider != null) {
            this.economy = ecoProvider.getProvider();
        }

        RegisteredServiceProvider<Permission> permProvider =
                Bukkit.getServicesManager().getRegistration(Permission.class);
        if (permProvider != null) {
            this.permission = permProvider.getProvider();
        }

        return economy != null;
    }

    public double getBalance(@NotNull Player player) {
        if (economy == null) {
            return 0;
        }
        return economy.getBalance(player);
    }

    public boolean hasMoney(@NotNull Player player, double amount) {
        if (economy == null) {
            return false;
        }
        return economy.has(player, amount);
    }

    public boolean takeMoney(@NotNull Player player, double amount) {
        if (economy == null) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean giveMoney(@NotNull Player player, double amount) {
        if (economy == null) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean givePermission(@NotNull Player player, @NotNull String perm) {
        if (permission == null) {
            return false;
        }
        return permission.playerAdd(player, perm);
    }

    public boolean takePermission(@NotNull Player player, @NotNull String perm) {
        if (permission == null) {
            return false;
        }
        return permission.playerRemove(player, perm);
    }
}
