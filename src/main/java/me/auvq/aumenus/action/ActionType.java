package me.auvq.aumenus.action;

import lombok.Getter;
import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

@Getter
public enum ActionType {

    PLAYER(List.of("player", "p"), (player, value) -> {
        player.getScheduler().run(AuMenus.getInstance(), task ->
                player.chat("/" + value), null);
    }),

    CONSOLE(List.of("console", "c"), (player, value) -> {
        Bukkit.getGlobalRegionScheduler().run(AuMenus.getInstance(), task ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value));
    }),

    COMMAND_EVENT(List.of("commandevent"), (player, value) -> {
        player.chat(value.startsWith("/") ? value : "/" + value);
    }),

    MESSAGE(List.of("msg", "message"), (player, value) -> player.sendMessage(Util.parse(value))),

    MINIMESSAGE(List.of("minimessage"), (player, value) -> player.sendMessage(Util.parse(value))),

    BROADCAST(List.of("broadcast"), (player, value) -> Bukkit.getServer().sendMessage(Util.parse(value))),

    MINIBROADCAST(List.of("minibroadcast"), (player, value) -> Bukkit.getServer().sendMessage(Util.parse(value))),

    CHAT(List.of("chat"), Player::chat),

    JSON(List.of("json"), (player, value) -> {
        player.sendMessage(GsonComponentSerializer.gson().deserialize(value));
    }),

    JSON_BROADCAST(List.of("jsonbroadcast"), (player, value) -> {
        Bukkit.getServer().sendMessage(GsonComponentSerializer.gson().deserialize(value));
    }),

    CLOSE(List.of("close"), (player, value) -> {
        player.getScheduler().run(AuMenus.getInstance(), task -> player.closeInventory(), null);
    }),

    TAKE_MONEY(List.of("take_money", "takemoney"), (player, value) -> {
        if (!AuMenus.getInstance().getHookProvider().isVaultEnabled()) return;
        double amount = Double.parseDouble(value);
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) return;
        AuMenus.getInstance().getHookProvider().vault().takeMoney(player, amount);
    }),

    GIVE_MONEY(List.of("give_money", "givemoney"), (player, value) -> {
        if (!AuMenus.getInstance().getHookProvider().isVaultEnabled()) return;
        double amount = Double.parseDouble(value);
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) return;
        AuMenus.getInstance().getHookProvider().vault().giveMoney(player, amount);
    }),

    TAKE_EXP(List.of("take_exp", "takeexp"), (player, value) -> modifyExp(player, value, false)),

    GIVE_EXP(List.of("give_exp", "giveexp"), (player, value) -> modifyExp(player, value, true)),

    GIVE_PERMISSION(List.of("give_perm", "givepermission"), (player, value) -> {
        if (!AuMenus.getInstance().getHookProvider().isVaultEnabled()) return;
        AuMenus.getInstance().getHookProvider().vault().givePermission(player, value);
    }),

    TAKE_PERMISSION(List.of("take_perm", "takepermission"), (player, value) -> {
        if (!AuMenus.getInstance().getHookProvider().isVaultEnabled()) return;
        AuMenus.getInstance().getHookProvider().vault().takePermission(player, value);
    }),

    SOUND(List.of("sound"), (player, value) -> playSound(player, value)),

    BROADCAST_SOUND(List.of("broadcast_sound", "broadcastsound"), (player, value) -> {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.getScheduler().run(AuMenus.getInstance(), task -> playSound(online, value), null);
        }
    }),

    BROADCAST_SOUND_WORLD(List.of("broadcast_sound_world", "broadcastsoundworld"), (player, value) -> {
        for (Player online : player.getWorld().getPlayers()) {
            online.getScheduler().run(AuMenus.getInstance(), task -> playSound(online, value), null);
        }
    }),

    CONNECT(List.of("connect"), (player, value) -> {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        try {
            out.writeUTF("Connect");
            out.writeUTF(value);
        } catch (IOException e) {
            AuMenus.getInstance().getLogger().warning("Failed to write BungeeCord connect message for server '"
                    + value + "': " + e.getMessage());
            return;
        }
        player.sendPluginMessage(AuMenus.getInstance(), "BungeeCord", bytes.toByteArray());
    }),

    PLACEHOLDER(List.of("placeholder"), (player, value) -> {
        if (!AuMenus.getInstance().getHookProvider().isPapiEnabled()) return;
        AuMenus.getInstance().getHookProvider().papi().setPlaceholders(player, value);
    });

    private final List<String> aliases;
    private final ActionExecutor executor;

    ActionType(@NotNull List<String> aliases, @NotNull ActionExecutor executor) {
        this.aliases = aliases;
        this.executor = executor;
    }

    private static void playSound(@NotNull Player player, @NotNull String value) {
        String[] parts = value.split("\\s+");
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
        String soundKey = parts[0].toLowerCase();
        if (soundKey.contains("_") && !soundKey.contains(".")) {
            soundKey = soundKey.replace("_", ".");
        }
        player.playSound(player.getLocation(), soundKey, volume, pitch);
    }

    private static void modifyExp(@NotNull Player player, @NotNull String value, boolean give) {
        boolean levels = value.toUpperCase().endsWith("L");
        String numStr = levels ? value.substring(0, value.length() - 1).trim() : value.trim();
        int amount = Integer.parseInt(numStr);

        if (give && levels) {
            player.setLevel(player.getLevel() + amount);
            return;
        }
        if (give) {
            player.giveExp(amount);
            return;
        }
        if (levels) {
            player.setLevel(Math.max(0, player.getLevel() - amount));
            return;
        }
        player.setTotalExperience(Math.max(0, player.getTotalExperience() - amount));
    }
}
