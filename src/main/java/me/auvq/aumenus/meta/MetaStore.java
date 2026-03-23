package me.auvq.aumenus.meta;

import me.auvq.aumenus.AuMenus;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class MetaStore {

    private final AuMenus plugin;

    public MetaStore(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    public void set(@NotNull Player player, @NotNull String key, @NotNull String type, @NotNull String value) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey nsKey = new NamespacedKey(plugin, key);

        try {
            switch (type.toUpperCase()) {
                case "STRING" -> pdc.set(nsKey, PersistentDataType.STRING, value);
                case "INTEGER", "INT" -> pdc.set(nsKey, PersistentDataType.INTEGER, Integer.parseInt(value));
                case "LONG" -> pdc.set(nsKey, PersistentDataType.LONG, Long.parseLong(value));
                case "DOUBLE" -> {
                    double parsed = Double.parseDouble(value);
                    if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                        return;
                    }
                    pdc.set(nsKey, PersistentDataType.DOUBLE, parsed);
                }
                case "BOOLEAN" -> pdc.set(nsKey, PersistentDataType.STRING, String.valueOf(Boolean.parseBoolean(value)));
                default -> plugin.getLogger().warning("Unknown meta type: " + type);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid value for meta key '" + key + "' (type " + type + "): " + value);
        }
    }

    public void remove(@NotNull Player player, @NotNull String key) {
        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        player.getPersistentDataContainer().remove(nsKey);
    }

    public void add(@NotNull Player player, @NotNull String key, @NotNull String type, @NotNull String value) {
        try {
            String current = get(player, key, type, "0");
            double result = Double.parseDouble(current) + Double.parseDouble(value);
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return;
            }
            set(player, key, type, formatNumber(result, type));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid number in meta add for key '" + key + "': " + e.getMessage());
        }
    }

    public void subtract(@NotNull Player player, @NotNull String key, @NotNull String type, @NotNull String value) {
        try {
            String current = get(player, key, type, "0");
            double result = Double.parseDouble(current) - Double.parseDouble(value);
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return;
            }
            set(player, key, type, formatNumber(result, type));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid number in meta subtract for key '" + key + "': " + e.getMessage());
        }
    }

    public void switchBoolean(@NotNull Player player, @NotNull String key) {
        String current = get(player, key, "BOOLEAN", "false");
        boolean value = !Boolean.parseBoolean(current);
        set(player, key, "BOOLEAN", String.valueOf(value));
    }

    public @NotNull String get(@NotNull Player player, @NotNull String key,
                                @NotNull String type, @NotNull String defaultValue) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey nsKey = new NamespacedKey(plugin, key);

        try {
            return switch (type.toUpperCase()) {
                case "STRING", "BOOLEAN" -> {
                    String val = pdc.get(nsKey, PersistentDataType.STRING);
                    yield val != null ? val : defaultValue;
                }
                case "INTEGER", "INT" -> {
                    Integer val = pdc.get(nsKey, PersistentDataType.INTEGER);
                    yield val != null ? String.valueOf(val) : defaultValue;
                }
                case "LONG" -> {
                    Long val = pdc.get(nsKey, PersistentDataType.LONG);
                    yield val != null ? String.valueOf(val) : defaultValue;
                }
                case "DOUBLE" -> {
                    Double val = pdc.get(nsKey, PersistentDataType.DOUBLE);
                    yield val != null ? String.valueOf(val) : defaultValue;
                }
                default -> defaultValue;
            };
        } catch (IllegalArgumentException e) {
            String detected = autoDetectGet(pdc, nsKey, null);
            return detected != null ? detected : defaultValue;
        }
    }

    private @Nullable String autoDetectGet(@NotNull PersistentDataContainer pdc,
                                            @NotNull NamespacedKey key,
                                            @Nullable String defaultValue) {
        String strVal = tryGet(pdc, key, PersistentDataType.STRING);
        if (strVal != null) {
            return strVal;
        }

        Integer intVal = tryGet(pdc, key, PersistentDataType.INTEGER);
        if (intVal != null) {
            return String.valueOf(intVal);
        }

        Double dblVal = tryGet(pdc, key, PersistentDataType.DOUBLE);
        if (dblVal != null) {
            return String.valueOf(dblVal);
        }

        Long longVal = tryGet(pdc, key, PersistentDataType.LONG);
        if (longVal != null) {
            return String.valueOf(longVal);
        }

        return defaultValue;
    }

    private <T> @Nullable T tryGet(@NotNull PersistentDataContainer pdc,
                                    @NotNull NamespacedKey key,
                                    @NotNull PersistentDataType<T, T> type) {
        try {
            return pdc.get(key, type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @NotNull List<String> getKeys(@NotNull Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        List<String> keys = new ArrayList<>();
        for (NamespacedKey key : pdc.getKeys()) {
            if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                keys.add(key.getKey());
            }
        }
        return keys;
    }

    public boolean hasValue(@NotNull Player player, @NotNull String key, @Nullable String type) {
        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (type == null) {
            return pdc.has(nsKey);
        }
        return switch (type.toUpperCase()) {
            case "STRING", "BOOLEAN" -> pdc.has(nsKey, PersistentDataType.STRING);
            case "INTEGER", "INT" -> pdc.has(nsKey, PersistentDataType.INTEGER);
            case "LONG" -> pdc.has(nsKey, PersistentDataType.LONG);
            case "DOUBLE" -> pdc.has(nsKey, PersistentDataType.DOUBLE);
            default -> false;
        };
    }

    public void executeMetaAction(@NotNull Player player, @NotNull String actionString) {
        String[] parts = actionString.trim().split("\\s+", 4);
        if (parts.length < 2) {
            return;
        }

        String operation = parts[0].toLowerCase();
        String key = parts[1];

        if (operation.equals("remove")) {
            remove(player, key);
            return;
        }

        if (operation.equals("switch")) {
            switchBoolean(player, key);
            return;
        }

        if (parts.length < 3) {
            return;
        }

        String type = parts[2];

        if (parts.length < 4) {
            return;
        }

        switch (operation) {
            case "set" -> set(player, key, type, parts[3]);
            case "add" -> add(player, key, type, parts[3]);
            case "subtract" -> subtract(player, key, type, parts[3]);
            default -> plugin.getLogger().warning("Unknown meta operation: " + operation);
        }
    }

    private @NotNull String formatNumber(double value, @NotNull String type) {
        return switch (type.toUpperCase()) {
            case "INTEGER", "INT" -> String.valueOf((int) value);
            case "LONG" -> String.valueOf((long) value);
            default -> String.valueOf(value);
        };
    }
}
