package me.auvq.aumenus.util;

import me.auvq.aumenus.AuMenus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Level;

public final class UpdateChecker implements Listener {

    private final AuMenus plugin;
    private @Nullable String latestVersion;

    public UpdateChecker(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    public void check() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String current = plugin.getPluginMeta().getVersion();
            String latest = fetchLatestVersion();
            if (latest == null) {
                return;
            }
            if (latest.equals(current)) {
                return;
            }
            latestVersion = latest;
            plugin.getLogger().info("A new version is available: v" + latest + " (current: v" + current + ")");
        });
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (latestVersion == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp() && !player.hasPermission("aumenus.admin")) {
            return;
        }
        String current = plugin.getPluginMeta().getVersion();
        player.getScheduler().runDelayed(plugin, task ->
                player.sendMessage(Util.parse("<gold>AuMenus <gray>- Update available: <white>v" + latestVersion + " <gray>(current: <white>v" + current + "<gray>)")),
                null, 40);
    }

    private @Nullable String fetchLatestVersion() {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(
                    "https://api.github.com/repos/auvq/AuMenus/releases/latest"
            ).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AuMenus/" + plugin.getPluginMeta().getVersion());
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            int tagIndex = json.indexOf("\"tag_name\":\"");
            if (tagIndex == -1) {
                return null;
            }
            int start = tagIndex + 12;
            int end = json.indexOf("\"", start);
            String tag = json.substring(start, end);
            return tag.startsWith("v") ? tag.substring(1) : tag;
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Update check failed", e);
            return null;
        }
    }
}
