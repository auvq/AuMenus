package me.auvq.aumenus.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.auvq.aumenus.AuMenus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeadProvider {

    private static final Map<String, ItemStack> HEAD_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> FETCHING = ConcurrentHashMap.newKeySet();
    private static final ItemStack PLACEHOLDER_SKULL = new ItemStack(Material.PLAYER_HEAD);

    private HeadProvider() {}

    public static @NotNull ItemStack createPlayerHead(@NotNull String playerName) {
        String cacheKey = "player:" + playerName.toLowerCase();

        ItemStack cached = HEAD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached.clone();
        }

        if (FETCHING.add(cacheKey)) {
            Bukkit.getAsyncScheduler().runNow(AuMenus.getInstance(), task -> {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
                    skull.setItemMeta(meta);
                }
                HEAD_CACHE.put(cacheKey, skull);
                FETCHING.remove(cacheKey);
            });
        }

        return PLACEHOLDER_SKULL.clone();
    }

    public static @NotNull ItemStack createBase64Head(@NotNull String base64) {
        return HEAD_CACHE.computeIfAbsent("base64:" + base64, key -> {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta == null) {
                return skull;
            }
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
            skull.setItemMeta(meta);
            return skull;
        });
    }

    public static @NotNull ItemStack createTextureHead(@NotNull String textureId) {
        String url = "http://textures.minecraft.net/texture/" + textureId;
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return createBase64Head(base64);
    }

    public static void clearCache() {
        HEAD_CACHE.clear();
        FETCHING.clear();
    }
}
