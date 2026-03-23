package me.auvq.aumenus.requirement;

import lombok.Getter;
import me.auvq.aumenus.AuMenus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public enum RequirementType {

    HAS_PERMISSION(List.of("has_permission", "has permission"), (player, config) -> {
        String perm = (String) config.get("permission");
        return perm != null && player.hasPermission(perm);
    }),

    HAS_PERMISSIONS(List.of("has_permissions", "has permissions"), (player, config) -> {
        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) config.get("permissions");
        if (perms == null) {
            return false;
        }
        int minimum = config.containsKey("minimum") ? ((Number) config.get("minimum")).intValue() : perms.size();
        int count = 0;
        for (String perm : perms) {
            if (player.hasPermission(perm)) {
                count++;
            }
        }
        return count >= minimum;
    }),

    HAS_MONEY(List.of("has_money", "has money"), (player, config) -> {
        AuMenus plugin = AuMenus.getInstance();
        if (!plugin.getHookProvider().isVaultEnabled()) {
            return false;
        }
        double amount = ((Number) config.get("amount")).doubleValue();
        return plugin.getHookProvider().vault().hasMoney(player, amount);
    }),

    HAS_EXP(List.of("has_exp", "has exp"), (player, config) -> {
        int amount = ((Number) config.get("amount")).intValue();
        boolean levels = config.containsKey("level") && Boolean.TRUE.equals(config.get("level"));
        if (levels) {
            return player.getLevel() >= amount;
        }
        return player.getTotalExperience() >= amount;
    }),

    HAS_ITEM(List.of("has_item", "has item"), (player, config) -> {
        String materialStr = (String) config.get("material");
        if (materialStr == null) {
            return false;
        }
        Material material = Material.matchMaterial(materialStr);
        if (material == null) {
            return false;
        }

        int amount = config.containsKey("amount") ? ((Number) config.get("amount")).intValue() : 1;
        String slotType = config.containsKey("slot") ? (String) config.get("slot") : null;

        List<ItemStack> toCheck = getItemsToCheck(player, slotType);

        String name = (String) config.get("name");
        String lore = (String) config.get("lore");
        boolean nameContains = Boolean.TRUE.equals(config.get("name_contains"));
        boolean loreContains = Boolean.TRUE.equals(config.get("lore_contains"));
        boolean nameIgnoreCase = Boolean.TRUE.equals(config.get("name_ignorecase"));
        boolean loreIgnoreCase = Boolean.TRUE.equals(config.get("lore_ignorecase"));

        int found = 0;
        for (ItemStack item : toCheck) {
            if (item.getType() != material) {
                continue;
            }
            if (name != null && !matchesText(getDisplayName(item), name, nameContains, nameIgnoreCase)) {
                continue;
            }
            if (lore != null && !matchesLore(item, lore, loreContains, loreIgnoreCase)) {
                continue;
            }
            found += item.getAmount();
        }
        return found >= amount;
    }),

    HAS_META(List.of("has_meta", "has meta"), (player, config) -> {
        String key = (String) config.get("key");
        String metaType = (String) config.get("meta_type");
        String value = config.containsKey("value") ? config.get("value").toString() : null;
        if (key == null) {
            return false;
        }

        AuMenus plugin = AuMenus.getInstance();
        if (value == null) {
            return plugin.getMetaStore().hasValue(player, key, metaType);
        }

        String stored = plugin.getMetaStore().get(player, key, metaType != null ? metaType : "STRING", "");
        return stored.equals(value);
    }),

    STRING_EQUALS(List.of("string_equals", "string equals"), (player, config) -> {
        String input = (String) config.get("input");
        String output = (String) config.get("output");
        return input != null && input.equals(output);
    }),

    STRING_EQUALS_IGNORECASE(List.of("string_equals_ignorecase", "string equals ignorecase"), (player, config) -> {
        String input = (String) config.get("input");
        String output = (String) config.get("output");
        return input != null && input.equalsIgnoreCase(output);
    }),

    STRING_CONTAINS(List.of("string_contains", "string contains"), (player, config) -> {
        String input = (String) config.get("input");
        String output = (String) config.get("output");
        return input != null && output != null && input.contains(output);
    }),

    STRING_CONTAINS_IGNORECASE(List.of("string_contains_ignorecase", "string contains ignorecase"), (player, config) -> {
        String input = (String) config.get("input");
        String output = (String) config.get("output");
        return input != null && output != null && input.toLowerCase().contains(output.toLowerCase());
    }),

    STRING_LENGTH(List.of("string_length", "string length"), (player, config) -> {
        String input = (String) config.get("input");
        if (input == null) {
            return false;
        }
        int len = input.length();
        int min = config.containsKey("min") ? ((Number) config.get("min")).intValue() : 0;
        int max = config.containsKey("max") ? ((Number) config.get("max")).intValue() : Integer.MAX_VALUE;
        return len >= min && len <= max;
    }),

    REGEX_MATCHES(List.of("regex_matches", "regex matches"), (player, config) -> {
        String input = (String) config.get("input");
        String regex = (String) config.get("regex");
        if (input == null || regex == null) {
            return false;
        }
        return RegexCache.PATTERNS.computeIfAbsent(regex, Pattern::compile).matcher(input).matches();
    }),

    COMPARATOR_EQUALS(List.of("=="), (player, config) -> compareValues(config) == 0),
    COMPARATOR_NOT_EQUALS(List.of("!="), (player, config) -> compareValues(config) != 0),
    COMPARATOR_GREATER(List.of(">"), (player, config) -> compareValues(config) > 0),
    COMPARATOR_LESS(List.of("<"), (player, config) -> compareValues(config) < 0),
    COMPARATOR_GREATER_EQUALS(List.of(">="), (player, config) -> compareValues(config) >= 0),
    COMPARATOR_LESS_EQUALS(List.of("<="), (player, config) -> compareValues(config) <= 0),

    IS_NEAR(List.of("is_near", "is near"), (player, config) -> {
        String locationStr = (String) config.get("location");
        if (locationStr == null) {
            return false;
        }
        double distance = ((Number) config.get("distance")).doubleValue();
        String[] parts = locationStr.split(",");
        if (parts.length < 4) {
            return false;
        }
        World world = Bukkit.getWorld(parts[0].trim());
        if (world == null || !player.getWorld().equals(world)) {
            return false;
        }
        double x = Double.parseDouble(parts[1].trim());
        double y = Double.parseDouble(parts[2].trim());
        double z = Double.parseDouble(parts[3].trim());
        Location loc = new Location(world, x, y, z);
        return player.getLocation().distanceSquared(loc) <= distance * distance;
    }),

    IS_OBJECT(List.of("is_object", "is object"), (player, config) -> {
        String input = (String) config.get("input");
        String objectType = (String) config.get("object");
        if (input == null || objectType == null) {
            return false;
        }
        return switch (objectType.toUpperCase()) {
            case "INT", "INTEGER" -> {
                try {
                    Integer.parseInt(input);
                    yield true;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case "DOUBLE" -> {
                try {
                    Double.parseDouble(input);
                    yield true;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case "UUID" -> {
                try {
                    UUID.fromString(input);
                    yield true;
                } catch (IllegalArgumentException e) {
                    yield false;
                }
            }
            case "PLAYER" -> Bukkit.getPlayerExact(input) != null;
            default -> false;
        };
    });

    private final List<String> aliases;
    private final RequirementEvaluator evaluator;

    RequirementType(@NotNull List<String> aliases, @NotNull RequirementEvaluator evaluator) {
        this.aliases = aliases;
        this.evaluator = evaluator;
    }

    private static int compareValues(@NotNull Map<String, Object> config) {
        String input = config.get("input") != null ? config.get("input").toString() : null;
        String output = config.get("output") != null ? config.get("output").toString() : null;
        if (input == null || output == null) {
            return -1;
        }
        try {
            double inputValue = Double.parseDouble(input);
            double outputValue = Double.parseDouble(output);
            return Double.compare(inputValue, outputValue);
        } catch (NumberFormatException e) {
            return input.compareTo(output);
        }
    }

    private static @NotNull List<ItemStack> getItemsToCheck(@NotNull Player player, @Nullable String slotType) {
        if (slotType == null) {
            return filterNulls(player.getInventory().getContents());
        }
        return switch (slotType) {
            case "main_hand" -> filterNulls(player.getInventory().getItemInMainHand());
            case "off_hand" -> filterNulls(player.getInventory().getItemInOffHand());
            case "armor_helmet" -> filterNulls(player.getInventory().getHelmet());
            case "armor_chestplate" -> filterNulls(player.getInventory().getChestplate());
            case "armor_leggings" -> filterNulls(player.getInventory().getLeggings());
            case "armor_boots" -> filterNulls(player.getInventory().getBoots());
            default -> filterNulls(player.getInventory().getContents());
        };
    }

    private static @NotNull List<ItemStack> filterNulls(@Nullable ItemStack... items) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private static @NotNull String getDisplayName(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "";
        }
        Component displayName = meta.displayName();
        if (displayName == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(displayName);
    }

    private static boolean matchesText(@NotNull String actual, @NotNull String expected,
                                        boolean contains, boolean ignoreCase) {
        if (contains) {
            return ignoreCase ? actual.toLowerCase().contains(expected.toLowerCase())
                    : actual.contains(expected);
        }
        return ignoreCase ? actual.equalsIgnoreCase(expected) : actual.equals(expected);
    }

    private static boolean matchesLore(@NotNull ItemStack item, @NotNull String lore,
                                        boolean contains, boolean ignoreCase) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }
        List<Component> loreComponents = meta.lore();
        if (loreComponents == null) {
            return false;
        }
        String fullLore = loreComponents.stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .collect(Collectors.joining("\n"));
        return matchesText(fullLore, lore, contains, ignoreCase);
    }

    private static final class RegexCache {
        static final ConcurrentHashMap<String, Pattern> PATTERNS = new ConcurrentHashMap<>();
    }
}
