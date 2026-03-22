package me.auvq.aumenus.hook;

import lombok.Getter;
import me.auvq.aumenus.AuMenus;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class HookProvider {

    @Getter
    private final boolean vaultEnabled;
    @Getter
    private final boolean papiEnabled;

    private final VaultHook vaultHook;
    private final PapiHook papiHook;

    public HookProvider(@NotNull AuMenus plugin) {
        Logger logger = plugin.getLogger();

        this.vaultHook = initVault(logger);
        this.vaultEnabled = vaultHook != null;

        this.papiHook = initPapi(plugin, logger);
        this.papiEnabled = papiHook != null;
    }

    public @NotNull VaultHook vault() {
        if (vaultHook == null) {
            throw new IllegalStateException("Vault is not available.");
        }
        return vaultHook;
    }

    public @NotNull PapiHook papi() {
        if (papiHook == null) {
            throw new IllegalStateException("PlaceholderAPI is not available.");
        }
        return papiHook;
    }

    private static VaultHook initVault(@NotNull Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.info("Vault not found. Economy and permission features disabled.");
            return null;
        }

        VaultHook hook = new VaultHook();
        if (!hook.setup()) {
            logger.warning("Vault found but no economy/permission provider registered.");
            return null;
        }

        logger.info("Vault hooked successfully.");
        return hook;
    }

    private static PapiHook initPapi(@NotNull AuMenus plugin, @NotNull Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            logger.info("PlaceholderAPI not found. External placeholders disabled.");
            return null;
        }

        logger.info("PlaceholderAPI hooked successfully.");
        return new PapiHook();
    }
}
