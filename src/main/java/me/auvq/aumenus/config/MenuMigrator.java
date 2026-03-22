package me.auvq.aumenus.config;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MenuMigrator {

    private final AuMenus plugin;

    private static final Map<String, String> DM_ACTION_PREFIXES = Map.ofEntries(
            Map.entry("[player] ", "player"),
            Map.entry("[console] ", "console"),
            Map.entry("[commandevent] ", "commandevent"),
            Map.entry("[message] ", "msg"),
            Map.entry("[minimessage] ", "minimessage"),
            Map.entry("[openguimenu] ", "open"),
            Map.entry("[connect] ", "connect"),
            Map.entry("[close]", "close"),
            Map.entry("[refresh]", "refresh"),
            Map.entry("[broadcast] ", "broadcast"),
            Map.entry("[sound] ", "sound"),
            Map.entry("[broadcastsound] ", "broadcast_sound"),
            Map.entry("[takemoney] ", "take_money"),
            Map.entry("[givemoney] ", "give_money"),
            Map.entry("[takeexp] ", "take_exp"),
            Map.entry("[giveexp] ", "give_exp"),
            Map.entry("[givepermission] ", "give_perm"),
            Map.entry("[takepermission] ", "take_perm"),
            Map.entry("[json] ", "json"),
            Map.entry("[chat] ", "chat"),
            Map.entry("[meta] ", "meta"),
            Map.entry("[placeholder] ", "placeholder")
    );

    private static final Set<String> COMPARATOR_TYPES = Set.of("==", "!=", ">", "<", ">=", "<=");

    public MenuMigrator(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    public @Nullable File findSourceFolder() {
        File dmPlugin = new File("plugins/DeluxeMenus/gui_menus");
        if (dmPlugin.exists() && dmPlugin.isDirectory()) {
            return dmPlugin;
        }

        File migrationFolder = new File(plugin.getDataFolder(), "migration");
        if (migrationFolder.exists() && migrationFolder.isDirectory()) {
            return migrationFolder;
        }

        return null;
    }

    public @NotNull List<String> listAvailableFiles() {
        File source = findSourceFolder();
        if (source == null) {
            return List.of();
        }

        List<File> files = findYamlFiles(source);
        List<String> names = new ArrayList<>();
        for (File file : files) {
            names.add(file.getName().replace(".yml", "").replace(".yaml", ""));
        }
        return names;
    }

    public int migrateAll(@Nullable Player notifier) {
        File source = findSourceFolder();
        if (source == null) {
            return 0;
        }

        List<File> files = findYamlFiles(source);

        int migrated = 0;
        for (File file : files) {
            if (migrateFile(file, notifier)) {
                migrated++;
            }
        }
        return migrated;
    }

    public boolean migrateSingle(@NotNull String name, @Nullable Player notifier) {
        File source = findSourceFolder();
        if (source == null) {
            return false;
        }

        List<File> allFiles = findYamlFiles(source);
        for (File file : allFiles) {
            String fileName = file.getName().replace(".yml", "").replace(".yaml", "");
            if (fileName.equalsIgnoreCase(name)) {
                return migrateFile(file, notifier);
            }
        }
        return false;
    }

    private boolean migrateFile(@NotNull File sourceFile, @Nullable Player notifier) {
        String menuName = sourceFile.getName().replace(".yml", "").replace(".yaml", "");
        File outputFile = new File(plugin.getDataFolder(), "menus/" + menuName + ".yml");

        if (outputFile.exists()) {
            log(notifier, "&#FF474DSkipping '" + menuName + "' - already exists in menus/");
            return false;
        }

        try {
            YamlConfiguration dmConfig = YamlConfiguration.loadConfiguration(sourceFile);
            YamlConfiguration ourConfig = convertMenu(dmConfig, menuName);

            outputFile.getParentFile().mkdirs();
            ourConfig.save(outputFile);
            log(notifier, "&#00FF7FMigrated '" + menuName + "' successfully.");
            return true;
        } catch (Exception e) {
            log(notifier, "&#FF474DFailed to migrate '" + menuName + "': " + e.getMessage());
            plugin.getLogger().warning("Migration failed for " + menuName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private @NotNull YamlConfiguration convertMenu(@NotNull YamlConfiguration dm, @NotNull String menuName) {
        YamlConfiguration out = new YamlConfiguration();

        String title = dm.getString("menu_title", dm.getString("title", "&8" + menuName));
        out.set("title", title);

        Object openCmd = dm.get("open_command");
        if (openCmd instanceof String cmd) {
            out.set("command", cmd);
        } else if (openCmd instanceof List<?> cmds && !cmds.isEmpty()) {
            out.set("command", cmds.getFirst().toString());
            if (cmds.size() > 1) {
                List<String> aliases = cmds.stream().skip(1).map(Object::toString).toList();
                out.set("command_aliases", aliases);
            }
        }

        out.set("register_command", dm.getBoolean("register_command", true));
        out.set("size", dm.getInt("size", 54));

        String type = dm.getString("inventory_type");
        if (type != null && !type.equalsIgnoreCase("CHEST")) {
            out.set("type", type);
        }

        int updateInterval = dm.getInt("update_interval", 0);
        if (updateInterval > 0) {
            out.set("update_interval", updateInterval);
        }

        List<String> args = dm.getStringList("args");
        if (!args.isEmpty()) {
            out.set("args", args);
        }

        String argsUsage = dm.getString("args_usage_message");
        if (argsUsage != null) {
            out.set("args_usage", argsUsage);
        }

        convertRequirement(dm, "open_requirement", out, "open_require");
        convertActions(dm, "open_commands", out, "on_open");
        convertActions(dm, "close_commands", out, "on_close");

        ConfigurationSection itemsSection = dm.getConfigurationSection("items");
        if (itemsSection == null) {
            return out;
        }

        for (String itemName : itemsSection.getKeys(false)) {
            ConfigurationSection dmItem = itemsSection.getConfigurationSection(itemName);
            if (dmItem == null) {
                continue;
            }
            convertItem(dmItem, itemName, out);
        }

        return out;
    }

    private void convertItem(@NotNull ConfigurationSection dmItem, @NotNull String itemName,
                              @NotNull YamlConfiguration out) {
        String prefix = "items." + itemName;

        String material = dmItem.getString("material", "STONE");
        out.set(prefix + ".material", material);

        String displayName = dmItem.getString("display_name");
        if (displayName != null) {
            out.set(prefix + ".name", displayName);
        }

        List<String> lore = dmItem.getStringList("lore");
        if (!lore.isEmpty()) {
            out.set(prefix + ".lore", lore);
        }

        if (dmItem.contains("slot")) {
            out.set(prefix + ".slot", dmItem.getInt("slot"));
        }
        if (dmItem.contains("slots")) {
            out.set(prefix + ".slots", dmItem.get("slots"));
        }

        int amount = dmItem.getInt("amount", 1);
        if (amount != 1) {
            out.set(prefix + ".amount", amount);
        }

        String dynamicAmount = dmItem.getString("dynamic_amount");
        if (dynamicAmount != null) {
            out.set(prefix + ".dynamic_amount", dynamicAmount);
        }

        int priority = dmItem.getInt("priority", 0);
        if (priority != 0) {
            out.set(prefix + ".priority", priority);
        }

        if (dmItem.getBoolean("update", false)) {
            out.set(prefix + ".update", true);
        }

        if (dmItem.contains("enchantments")) {
            out.set(prefix + ".enchantments", dmItem.getStringList("enchantments"));
        }
        if (dmItem.contains("hide_enchantments")) {
            out.set(prefix + ".item_flags", List.of("HIDE_ENCHANTS"));
        }
        if (dmItem.contains("hide_attributes")) {
            List<String> flags = new ArrayList<>(dmItem.getStringList("item_flags"));
            flags.add("HIDE_ATTRIBUTES");
            out.set(prefix + ".item_flags", flags);
        }
        if (dmItem.contains("item_flags")) {
            out.set(prefix + ".item_flags", dmItem.getStringList("item_flags"));
        }
        if (dmItem.getBoolean("unbreakable", false)) {
            out.set(prefix + ".unbreakable", true);
        }
        if (dmItem.contains("model_data")) {
            out.set(prefix + ".model_data", dmItem.getInt("model_data"));
        }
        if (dmItem.contains("banner_meta")) {
            out.set(prefix + ".banner_meta", dmItem.getStringList("banner_meta"));
        }
        if (dmItem.contains("potion_effects")) {
            out.set(prefix + ".potion_effects", dmItem.getStringList("potion_effects"));
        }
        if (dmItem.contains("rgb")) {
            out.set(prefix + ".rgb", dmItem.getString("rgb"));
        }
        if (dmItem.contains("trim_material")) {
            out.set(prefix + ".trim_material", dmItem.getString("trim_material"));
        }
        if (dmItem.contains("trim_pattern")) {
            out.set(prefix + ".trim_pattern", dmItem.getString("trim_pattern"));
        }

        convertActions(dmItem, "click_commands", out, prefix + ".on_click");
        convertActions(dmItem, "left_click_commands", out, prefix + ".on_left_click");
        convertActions(dmItem, "right_click_commands", out, prefix + ".on_right_click");
        convertActions(dmItem, "shift_left_click_commands", out, prefix + ".on_shift_left_click");
        convertActions(dmItem, "shift_right_click_commands", out, prefix + ".on_shift_right_click");

        convertRequirement(dmItem, "view_requirement", out, prefix + ".view_require");
        convertRequirement(dmItem, "click_requirement", out, prefix + ".click_require");
        convertRequirement(dmItem, "left_click_requirement", out, prefix + ".left_click_require");
        convertRequirement(dmItem, "right_click_requirement", out, prefix + ".right_click_require");
        convertRequirement(dmItem, "shift_left_click_requirement", out, prefix + ".shift_left_click_require");
        convertRequirement(dmItem, "shift_right_click_requirement", out, prefix + ".shift_right_click_require");
    }

    private void convertActions(@NotNull ConfigurationSection source, @NotNull String sourceKey,
                                 @NotNull YamlConfiguration out, @NotNull String outKey) {
        List<String> dmActions = source.getStringList(sourceKey);
        if (dmActions.isEmpty()) {
            return;
        }

        List<Object> converted = new ArrayList<>();
        for (String dmAction : dmActions) {
            converted.add(convertAction(dmAction));
        }
        out.set(outKey, converted);
    }

    private @NotNull Object convertAction(@NotNull String dmAction) {
        String action = dmAction.trim();

        int delayStart = action.indexOf("<delay=");
        int chanceStart = action.indexOf("<chance=");
        int delay = 0;
        double chance = 100;

        if (delayStart != -1) {
            int delayEnd = action.indexOf(">", delayStart);
            if (delayEnd != -1) {
                delay = Integer.parseInt(action.substring(delayStart + 7, delayEnd).trim());
                action = action.substring(0, delayStart) + action.substring(delayEnd + 1);
            }
        }
        if (chanceStart != -1) {
            int chanceEnd = action.indexOf(">", chanceStart);
            if (chanceEnd != -1) {
                chance = Double.parseDouble(action.substring(chanceStart + 8, chanceEnd).trim());
                action = action.substring(0, chanceStart) + action.substring(chanceEnd + 1);
            }
        }
        action = action.trim();

        String type = "player";
        String value = action;
        boolean matched = false;

        for (Map.Entry<String, String> entry : DM_ACTION_PREFIXES.entrySet()) {
            if (action.startsWith(entry.getKey())) {
                type = entry.getValue();
                value = action.substring(entry.getKey().length());
                matched = true;
                break;
            }
        }

        if (!matched && (action.startsWith("&") || action.startsWith("§") || action.startsWith("#") || action.startsWith("<"))) {
            type = "msg";
            value = action;
        }

        if (delay == 0 && chance >= 100) {
            if (value.isEmpty()) {
                return type;
            }
            return Map.of(type, value);
        }

        Map<String, Object> actionMap = new LinkedHashMap<>();
        actionMap.put(type, value);
        if (delay > 0) {
            actionMap.put("delay", delay);
        }
        if (chance < 100) {
            actionMap.put("chance", chance);
        }
        return actionMap;
    }

    private void convertRequirement(@NotNull ConfigurationSection source, @NotNull String sourceKey,
                                     @NotNull YamlConfiguration out, @NotNull String outKey) {
        ConfigurationSection reqSection = source.getConfigurationSection(sourceKey);
        if (reqSection == null) {
            return;
        }

        ConfigurationSection requirements = reqSection.getConfigurationSection("requirements");
        if (requirements == null) {
            return;
        }

        for (String reqName : requirements.getKeys(false)) {
            ConfigurationSection req = requirements.getConfigurationSection(reqName);
            if (req == null) {
                continue;
            }

            String type = req.getString("type", "");
            String checkPath = outKey + ".checks." + reqName;

            out.set(checkPath + ".type", convertRequirementType(type));

            switch (type.toLowerCase().replace(" ", "_")) {
                case "has_permission" -> out.set(checkPath + ".permission", req.getString("permission"));
                case "has_money" -> out.set(checkPath + ".amount", req.getDouble("amount"));
                case "has_exp" -> {
                    out.set(checkPath + ".amount", req.getInt("amount"));
                    if (req.getBoolean("level", false)) {
                        out.set(checkPath + ".level", true);
                    }
                }
                case "has_item" -> {
                    out.set(checkPath + ".material", req.getString("material"));
                    out.set(checkPath + ".amount", req.getInt("amount", 1));
                    if (req.contains("name")) {
                        out.set(checkPath + ".name", req.getString("name"));
                    }
                    if (req.contains("lore")) {
                        out.set(checkPath + ".lore", req.getString("lore"));
                    }
                }
                case "string_equals", "string_equals_ignorecase", "string_contains" -> {
                    out.set(checkPath + ".input", req.getString("input"));
                    out.set(checkPath + ".output", req.getString("output"));
                }
                case "regex_matches" -> {
                    out.set(checkPath + ".input", req.getString("input"));
                    out.set(checkPath + ".regex", req.getString("regex"));
                }
                case "has_meta" -> {
                    out.set(checkPath + ".key", req.getString("key"));
                    out.set(checkPath + ".meta_type", req.getString("meta_type"));
                    out.set(checkPath + ".value", req.getString("value"));
                }
                case "is_near" -> {
                    out.set(checkPath + ".location", req.getString("location"));
                    out.set(checkPath + ".distance", req.getDouble("distance"));
                }
                case "string_length" -> {
                    out.set(checkPath + ".input", req.getString("input"));
                    if (req.contains("min")) {
                        out.set(checkPath + ".min", req.getInt("min"));
                    }
                    if (req.contains("max")) {
                        out.set(checkPath + ".max", req.getInt("max"));
                    }
                }
                default -> {
                    if (type.startsWith("(") || COMPARATOR_TYPES.contains(type)) {
                        out.set(checkPath + ".input", req.getString("input"));
                        out.set(checkPath + ".output", req.getString("output"));
                    }
                }
            }

            convertActions(req, "deny_commands", out, checkPath + ".deny");
            convertActions(req, "success_commands", out, checkPath + ".success");
        }

        if (reqSection.contains("minimum_requirements")) {
            out.set(outKey + ".minimum", reqSection.getInt("minimum_requirements"));
        }
        if (reqSection.getBoolean("stop_at_success", false)) {
            out.set(outKey + ".stop_at_success", true);
        }

        convertActions(reqSection, "deny_commands", out, outKey + ".deny");
    }

    private @NotNull String convertRequirementType(@NotNull String dmType) {
        return dmType.toLowerCase().replace(" ", "_");
    }

    private @NotNull List<File> findYamlFiles(@NotNull File directory) {
        List<File> result = new ArrayList<>();
        File[] entries = directory.listFiles();
        if (entries == null) {
            return result;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                result.addAll(findYamlFiles(entry));
                continue;
            }
            String name = entry.getName();
            if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                result.add(entry);
            }
        }
        return result;
    }

    private void log(@Nullable Player player, @NotNull String message) {
        if (player != null) {
            player.sendMessage(Util.parse(message));
        }
        plugin.getLogger().info(message.replaceAll("&#[0-9a-fA-F]{6}", "").replaceAll("&[0-9a-fk-or]", ""));
    }
}
