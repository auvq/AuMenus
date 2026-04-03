package me.auvq.aumenus.config;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Map.entry("[jsonbroadcast] ", "jsonbroadcast"),
            Map.entry("[chat] ", "chat"),
            Map.entry("[broadcastsoundworld] ", "broadcast_sound_world"),
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
            replaceDmPlaceholders(ourConfig);

            outputFile.getParentFile().mkdirs();
            ourConfig.save(outputFile);
            log(notifier, "&#00FF7FMigrated '" + menuName + "' successfully.");
            return true;
        } catch (Exception e) {
            log(notifier, "&#FF474DFailed to migrate '" + menuName + "': " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "Migration failed for " + menuName, e);
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

        if (out.contains("command") && !dm.getBoolean("register_command", true)) {
            out.set("register_command", false);
        }
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

        if (dmItem.contains("priority")) {
            out.set(prefix + ".priority", dmItem.getInt("priority"));
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
        if (dmItem.getBoolean("hide_tooltip", false)) {
            out.set(prefix + ".hide_tooltip", true);
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
        convertActions(dmItem, "middle_click_commands", out, prefix + ".on_middle_click");

        convertRequirement(dmItem, "view_requirement", out, prefix + ".view_require");
        convertRequirement(dmItem, "click_requirement", out, prefix + ".click_require");
        convertRequirement(dmItem, "left_click_requirement", out, prefix + ".left_click_require");
        convertRequirement(dmItem, "right_click_requirement", out, prefix + ".right_click_require");
        convertRequirement(dmItem, "shift_left_click_requirement", out, prefix + ".shift_left_click_require");
        convertRequirement(dmItem, "shift_right_click_requirement", out, prefix + ".shift_right_click_require");
        convertRequirement(dmItem, "middle_click_requirement", out, prefix + ".middle_click_require");
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
        int delay = 0;
        double chance = 100;

        if (delayStart != -1) {
            int delayEnd = action.indexOf(">", delayStart);
            if (delayEnd != -1) {
                delay = Integer.parseInt(action.substring(delayStart + 7, delayEnd).trim());
                action = action.substring(0, delayStart) + action.substring(delayEnd + 1);
            }
        }
        int chanceStart = action.indexOf("<chance=");
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

        if (action.equals("[message]")) {
            type = "msg";
            value = " ";
            matched = true;
        }

        for (Map.Entry<String, String> entry : DM_ACTION_PREFIXES.entrySet()) {
            if (!matched && action.startsWith(entry.getKey())) {
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
            String normalized = type.toLowerCase().replace(" ", "_");
            if (normalized.startsWith("!")) {
                normalized = normalized.substring(1);
            }

            String checkPath = outKey + ".checks." + reqName;

            out.set(checkPath + ".type", convertRequirementType(type));
            convertRequirementFields(type, req, out, checkPath);

            if (req.getBoolean("optional", false)) {
                out.set(checkPath + ".optional", true);
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

    private void convertRequirementFields(@NotNull String type, @NotNull ConfigurationSection req,
                                           @NotNull YamlConfiguration out, @NotNull String checkPath) {
        String normalized = type.toLowerCase().replace(" ", "_");
        if (normalized.startsWith("!")) {
            normalized = normalized.substring(1);
        }
        switch (normalized) {
            case "has_permission" -> out.set(checkPath + ".permission", req.getString("permission"));
            case "has_money" -> out.set(checkPath + ".amount", req.getDouble("amount"));
            case "has_exp" -> convertHasExpFields(req, out, checkPath);
            case "has_item" -> convertHasItemFields(req, out, checkPath);
            case "string_equals", "string_equals_ignorecase", "string_contains" -> convertInputOutputFields(req, out, checkPath);
            case "regex_matches" -> convertRegexFields(req, out, checkPath);
            case "has_meta" -> convertHasMetaFields(req, out, checkPath);
            case "is_near" -> convertIsNearFields(req, out, checkPath);
            case "string_length" -> convertStringLengthFields(req, out, checkPath);
            case "javascript" -> {
                String expression = req.getString("expression");
                if (expression != null) {
                    out.set(checkPath + ".expression", expression);
                }
            }
            default -> convertComparatorFields(type, req, out, checkPath);
        }
    }

    private void convertHasExpFields(@NotNull ConfigurationSection req,
                                      @NotNull YamlConfiguration out, @NotNull String checkPath) {
        out.set(checkPath + ".amount", req.getInt("amount"));
        if (req.getBoolean("level", false)) {
            out.set(checkPath + ".level", true);
        }
    }

    private void convertHasItemFields(@NotNull ConfigurationSection req,
                                       @NotNull YamlConfiguration out, @NotNull String checkPath) {
        out.set(checkPath + ".material", req.getString("material"));
        out.set(checkPath + ".amount", req.getInt("amount", 1));
        if (req.contains("name")) {
            out.set(checkPath + ".name", req.getString("name"));
        }
        if (req.contains("lore")) {
            out.set(checkPath + ".lore", req.getString("lore"));
        }
    }

    private void convertInputOutputFields(@NotNull ConfigurationSection req,
                                            @NotNull YamlConfiguration out, @NotNull String checkPath) {
        out.set(checkPath + ".input", req.getString("input"));
        out.set(checkPath + ".output", req.getString("output"));
    }

    private void convertRegexFields(@NotNull ConfigurationSection req,
                                     @NotNull YamlConfiguration out, @NotNull String checkPath) {
        out.set(checkPath + ".input", req.getString("input"));
        out.set(checkPath + ".regex", req.getString("regex"));
    }

    private void convertHasMetaFields(@NotNull ConfigurationSection req,
                                       @NotNull YamlConfiguration out, @NotNull String checkPath) {
        out.set(checkPath + ".key", req.getString("key"));
        out.set(checkPath + ".meta_type", req.getString("meta_type"));
        out.set(checkPath + ".value", req.getString("value"));
    }

    private void convertIsNearFields(@NotNull ConfigurationSection req,
                                      @NotNull YamlConfiguration out, @NotNull String checkPath) {
        out.set(checkPath + ".location", req.getString("location"));
        out.set(checkPath + ".distance", req.getDouble("distance"));
    }

    private void convertStringLengthFields(@NotNull ConfigurationSection req,
                                            @NotNull YamlConfiguration out, @NotNull String checkPath) {
        out.set(checkPath + ".input", req.getString("input"));
        if (req.contains("min")) {
            out.set(checkPath + ".min", req.getInt("min"));
        }
        if (req.contains("max")) {
            out.set(checkPath + ".max", req.getInt("max"));
        }
    }

    private void convertComparatorFields(@NotNull String type, @NotNull ConfigurationSection req,
                                          @NotNull YamlConfiguration out, @NotNull String checkPath) {
        if (!type.startsWith("(") && !COMPARATOR_TYPES.contains(type)) {
            return;
        }
        out.set(checkPath + ".input", req.getString("input"));
        out.set(checkPath + ".output", req.getString("output"));
    }

    private @NotNull String convertRequirementType(@NotNull String dmType) {
        String cleaned = dmType.toLowerCase().replace(" ", "_");
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    private void replaceDmPlaceholders(@NotNull YamlConfiguration config) {
        String yaml = config.saveToString();
        if (!yaml.contains("deluxemenus_")) {
            return;
        }
        yaml = yaml.replace("%deluxemenus_meta_", "%aumenus_meta_");
        yaml = yaml.replace("{deluxemenus_meta_", "{aumenus_meta_");
        YamlConfiguration replaced = new YamlConfiguration();
        try {
            replaced.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            return;
        }
        for (String key : replaced.getKeys(true)) {
            if (!replaced.isConfigurationSection(key)) {
                config.set(key, replaced.get(key));
            }
        }
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

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern SECTION_HEX_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("§([0-9a-fk-orA-FK-OR])");

    public boolean convertToMiniMessage(@NotNull File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            convertStringKey(config, "title");
            convertStringKey(config, "args_usage");
            convertActionList(config, "on_open");
            convertActionList(config, "on_close");

            convertItemSection(config.getConfigurationSection("items"));
            convertItemSection(config.getConfigurationSection("page_items"));

            config.save(file);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert " + file.getName() + " to MiniMessage: " + e.getMessage());
            return false;
        }
    }

    private void convertItemSection(@Nullable ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String itemName : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(itemName);
            if (item == null) {
                continue;
            }
            convertStringKey(item, "name");
            convertStringList(item, "lore");
            convertActionList(item, "on_click");
            convertActionList(item, "on_left_click");
            convertActionList(item, "on_right_click");
            convertActionList(item, "on_shift_left_click");
            convertActionList(item, "on_shift_right_click");
            convertActionList(item, "on_middle_click");
            convertRequirementDeny(item, "click_require");
            convertRequirementDeny(item, "left_click_require");
            convertRequirementDeny(item, "right_click_require");
            convertRequirementDeny(item, "shift_left_click_require");
            convertRequirementDeny(item, "shift_right_click_require");
            convertRequirementDeny(item, "middle_click_require");
            convertRequirementDeny(item, "view_require");
        }
    }

    private void convertStringKey(@NotNull ConfigurationSection section, @NotNull String key) {
        String value = section.getString(key);
        if (value == null || !hasLegacyCodes(value)) {
            return;
        }
        section.set(key, legacyToMiniMessage(value));
    }

    private void convertStringList(@NotNull ConfigurationSection section, @NotNull String key) {
        List<String> list = section.getStringList(key);
        if (list.isEmpty()) {
            return;
        }
        boolean changed = false;
        List<String> converted = new ArrayList<>();
        for (String line : list) {
            if (hasLegacyCodes(line)) {
                converted.add(legacyToMiniMessage(line));
                changed = true;
            } else {
                converted.add(line);
            }
        }
        if (changed) {
            section.set(key, converted);
        }
    }

    private void convertActionList(@NotNull ConfigurationSection section, @NotNull String key) {
        List<?> list = section.getList(key);
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Object> converted = new ArrayList<>();
        boolean changed = false;
        for (Object entry : list) {
            if (entry instanceof String str && hasLegacyCodes(str)) {
                converted.add(legacyToMiniMessage(str));
                changed = true;
                continue;
            }
            if (entry instanceof Map<?, ?> map) {
                Object result = convertActionMapLegacy(map);
                converted.add(result);
                if (result != entry) {
                    changed = true;
                }
                continue;
            }
            converted.add(entry);
        }
        if (changed) {
            section.set(key, converted);
        }
    }

    private static final Set<String> LEGACY_CONVERTIBLE_KEYS = Set.of(
            "msg", "message", "broadcast", "minimessage", "minibroadcast"
    );

    private @NotNull Object convertActionMapLegacy(@NotNull Map<?, ?> map) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        boolean mapChanged = false;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String entryKey = entry.getKey().toString();
            Object entryValue = entry.getValue();
            if (entryValue instanceof String str && hasLegacyCodes(str)
                    && LEGACY_CONVERTIBLE_KEYS.contains(entryKey)) {
                newMap.put(entryKey, legacyToMiniMessage(str));
                mapChanged = true;
                continue;
            }
            newMap.put(entryKey, entryValue);
        }
        if (!mapChanged) {
            return map;
        }
        return newMap;
    }

    private void convertRequirementDeny(@NotNull ConfigurationSection parent, @NotNull String key) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) {
            return;
        }
        convertActionList(section, "deny");
        ConfigurationSection checks = section.getConfigurationSection("checks");
        if (checks == null) {
            return;
        }
        for (String checkName : checks.getKeys(false)) {
            ConfigurationSection check = checks.getConfigurationSection(checkName);
            if (check != null) {
                convertActionList(check, "deny");
            }
        }
    }

    private static boolean hasLegacyCodes(@NotNull String text) {
        return text.contains("&") || text.contains("§");
    }

    private static @NotNull String legacyToMiniMessage(@NotNull String input) {
        String prepared = input;

        Matcher sectionHexMatcher = SECTION_HEX_PATTERN.matcher(prepared);
        StringBuilder sectionHexBuilder = new StringBuilder();
        while (sectionHexMatcher.find()) {
            String hex = sectionHexMatcher.group().replaceAll("§[xX]|§", "");
            sectionHexMatcher.appendReplacement(sectionHexBuilder, "&#" + hex);
        }
        sectionHexMatcher.appendTail(sectionHexBuilder);
        prepared = sectionHexBuilder.toString();

        prepared = SECTION_CODE_PATTERN.matcher(prepared).replaceAll("&$1");

        Component component = LEGACY_SERIALIZER.deserialize(prepared);
        return MINI_MESSAGE.serialize(component);
    }

    private void log(@Nullable Player player, @NotNull String message) {
        if (player != null) {
            player.sendMessage(Util.parse(message));
        }
        plugin.getLogger().info(message.replaceAll("&#[0-9a-fA-F]{6}", "").replaceAll("&[0-9a-fk-or]", ""));
    }
}
