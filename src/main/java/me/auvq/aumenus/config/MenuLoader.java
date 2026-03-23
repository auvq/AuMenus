package me.auvq.aumenus.config;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.action.ActionRegistry;
import me.auvq.aumenus.hook.HookProvider;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.menu.MenuRegistry;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.requirement.RequirementRegistry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class MenuLoader {

    private final AuMenus plugin;
    private final ActionRegistry actionRegistry;
    private final RequirementRegistry requirementRegistry;
    private final MenuRegistry menuRegistry;

    public MenuLoader(@NotNull AuMenus plugin,
                      @NotNull ActionRegistry actionRegistry,
                      @NotNull RequirementRegistry requirementRegistry,
                      @NotNull MenuRegistry menuRegistry) {
        this.plugin = plugin;
        this.actionRegistry = actionRegistry;
        this.requirementRegistry = requirementRegistry;
        this.menuRegistry = menuRegistry;
    }

    public int loadAll() {
        File menusDir = new File(plugin.getDataFolder(), "menus");
        if (!menusDir.exists()) {
            menusDir.mkdirs();
            return 0;
        }

        File[] files = menusDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) {
            return 0;
        }

        int loaded = 0;
        for (File file : files) {
            Menu menu = loadMenu(file);
            if (menu == null) {
                continue;
            }
            menuRegistry.register(menu);
            loaded++;
            logMenuErrors(menu);
        }
        return loaded;
    }

    private void logMenuErrors(@NotNull Menu menu) {
        if (!menu.hasErrors()) {
            return;
        }
        for (String error : menu.getLoadErrors()) {
            plugin.getLogger().warning(error);
        }
    }

    public @Nullable Menu loadMenu(@NotNull File file) {
        String fileName = file.getName();
        String menuName = fileName.replace(".yml", "").replace(".yaml", "");

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigValidator validator = new ConfigValidator(fileName);
            checkDuplicateKeys(file, validator);

            String title = config.getString("title", "&8" + menuName);
            String typeStr = config.getString("type", "CHEST");
            validator.validateInventoryType(typeStr);
            InventoryType invType = parseInventoryType(typeStr);
            int size = config.getInt("size", 54);
            if (invType == InventoryType.CHEST) {
                validator.validateSize(size);
            }

            String command = config.getString("command");
            List<String> commandAliases = config.getStringList("command_aliases");
            boolean registerCommand = config.getBoolean("register_command", true);
            int updateInterval = config.getInt("update_interval",
                    plugin.getConfig().getInt("default_update_interval", 20));
            int clickCooldown = config.getInt("click_cooldown", -1);
            boolean allowTargetPlayer = config.getBoolean("allow_target_player",
                    plugin.getConfig().getBoolean("default_allow_target_player", false));
            boolean allowOfflineTarget = config.getBoolean("allow_offline_target",
                    plugin.getConfig().getBoolean("default_allow_offline_target", false));

            List<String> args = config.getStringList("args");
            String argsUsage = config.getString("args_usage");

            Map<String, RequirementList> argRequirements = parseArgRequirements(config);
            RequirementList openRequire = parseRequirementSection(config, "open_require");
            List<Action> onOpen = actionRegistry.parseActions(config.getList("on_open"));
            List<Action> onClose = actionRegistry.parseActions(config.getList("on_close"));

            Map<Integer, List<MenuItem>> items = parseItems(config, size, validator);
            List<Integer> pageSlots = parseSlotList(config.getString("page_slots", ""), size);
            List<MenuItem> pageItems = parsePageItems(config, size, validator);

            Menu menu = Menu.builder()
                    .name(menuName)
                    .title(title)
                    .size(size)
                    .inventoryType(invType)
                    .command(command)
                    .commandAliases(commandAliases)
                    .registerCommand(registerCommand)
                    .updateInterval(updateInterval)
                    .clickCooldown(clickCooldown)
                    .allowTargetPlayer(allowTargetPlayer)
                    .allowOfflineTarget(allowOfflineTarget)
                    .args(args)
                    .argsUsage(argsUsage)
                    .argRequirements(argRequirements.isEmpty() ? null : argRequirements)
                    .openRequire(openRequire)
                    .onOpen(onOpen)
                    .onClose(onClose)
                    .items(items)
                    .paginated(!pageSlots.isEmpty())
                    .pageSlots(pageSlots)
                    .pageItems(pageItems)
                    .sourceFile(file.getAbsolutePath())
                    .loadErrors(validator.getErrors())
                    .build();

            checkHookDependencies(menu, fileName);
            return menu;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load menu file " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private @NotNull Map<Integer, List<MenuItem>> parseItems(@NotNull YamlConfiguration config,
                                                              int menuSize,
                                                              @NotNull ConfigValidator validator) {
        Map<Integer, List<MenuItem>> items = new TreeMap<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return items;
        }

        int order = 0;
        for (String itemName : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemName);
            if (itemSection == null) {
                continue;
            }

            MenuItem menuItem = parseMenuItem(itemName, itemSection, menuSize, validator, order++);
            for (int slot : menuItem.getSlots()) {
                items.computeIfAbsent(slot, k -> new ArrayList<>()).add(menuItem);
            }
        }

        for (List<MenuItem> slotItems : items.values()) {
            slotItems.sort(Comparator.comparingInt(MenuItem::getPriority)
                    .thenComparing(Comparator.comparingInt(MenuItem::getConfigOrder).reversed()));
        }

        return items;
    }

    private @NotNull List<MenuItem> parsePageItems(@NotNull YamlConfiguration config,
                                                    int menuSize,
                                                    @NotNull ConfigValidator validator) {
        List<MenuItem> pageItems = new ArrayList<>();
        ConfigurationSection pageItemsSection = config.getConfigurationSection("page_items");
        if (pageItemsSection == null) {
            return pageItems;
        }

        for (String itemName : pageItemsSection.getKeys(false)) {
            ConfigurationSection itemSection = pageItemsSection.getConfigurationSection(itemName);
            if (itemSection == null) {
                continue;
            }
            pageItems.add(parseMenuItem(itemName, itemSection, menuSize, validator, 0));
        }

        return pageItems;
    }

    private @NotNull MenuItem parseMenuItem(@NotNull String name,
                                             @NotNull ConfigurationSection section,
                                             int menuSize,
                                             @NotNull ConfigValidator validator,
                                             int configOrder) {
        String material = section.getString("material", "STONE");
        validator.validateMaterial(material, name);

        List<Integer> slots;
        if (section.contains("slot")) {
            int slot = section.getInt("slot");
            validator.validateSlot(slot, menuSize, name);
            slots = List.of(slot);
        } else if (section.contains("slots")) {
            Object slotsObj = section.get("slots");
            if (slotsObj instanceof String str) {
                slots = parseSlotList(str, menuSize);
            } else if (slotsObj instanceof List<?> list) {
                slots = parseSlotListFromYamlList(list, menuSize);
            } else {
                slots = List.of();
            }
        } else {
            slots = List.of();
        }

        return MenuItem.builder()
                .name(name)
                .material(material)
                .displayName(section.getString("name"))
                .lore(section.contains("lore") ? section.getStringList("lore") : null)
                .amount(section.getInt("amount", 1))
                .dynamicAmount(section.getString("dynamic_amount"))
                .slots(slots)
                .priority(section.getInt("priority", 0))
                .configOrder(configOrder)
                .update(section.getBoolean("update", false))
                .enchantments(section.contains("enchantments") ? section.getStringList("enchantments") : null)
                .enchantmentGlintOverride(section.contains("enchantment_glint_override")
                        ? section.getBoolean("enchantment_glint_override") : null)
                .hideTooltip(section.contains("hide_tooltip") ? section.getBoolean("hide_tooltip") : null)
                .rarity(section.getString("rarity"))
                .itemFlags(section.contains("item_flags") ? section.getStringList("item_flags") : null)
                .unbreakable(section.getBoolean("unbreakable", false))
                .modelData(section.contains("model_data") ? section.getInt("model_data") : null)
                .itemModel(section.getString("item_model"))
                .tooltipStyle(section.getString("tooltip_style"))
                .bannerMeta(section.contains("banner_meta") ? section.getStringList("banner_meta") : null)
                .baseColor(section.getString("base_color"))
                .lightLevel(section.contains("light_level") ? section.getInt("light_level") : null)
                .trimMaterial(section.getString("trim_material"))
                .trimPattern(section.getString("trim_pattern"))
                .potionEffects(section.contains("potion_effects") ? section.getStringList("potion_effects") : null)
                .rgb(section.getString("rgb"))
                .damage(section.contains("damage") ? section.getInt("damage") : null)
                .loreAppendMode(section.getString("lore_append_mode"))
                .clickActions(actionRegistry.parseActions(section.getList("on_click")))
                .leftClickActions(actionRegistry.parseActions(section.getList("on_left_click")))
                .rightClickActions(actionRegistry.parseActions(section.getList("on_right_click")))
                .shiftLeftClickActions(actionRegistry.parseActions(section.getList("on_shift_left_click")))
                .shiftRightClickActions(actionRegistry.parseActions(section.getList("on_shift_right_click")))
                .middleClickActions(actionRegistry.parseActions(section.getList("on_middle_click")))
                .clickRequire(parseRequirementSection(section, "click_require"))
                .leftClickRequire(parseRequirementSection(section, "left_click_require"))
                .rightClickRequire(parseRequirementSection(section, "right_click_require"))
                .shiftLeftClickRequire(parseRequirementSection(section, "shift_left_click_require"))
                .shiftRightClickRequire(parseRequirementSection(section, "shift_right_click_require"))
                .middleClickRequire(parseRequirementSection(section, "middle_click_require"))
                .viewRequire(parseRequirementSection(section, "view_require"))
                .errorMessage(null)
                .build();
    }

    private @Nullable RequirementList parseRequirementSection(@NotNull ConfigurationSection parent,
                                                               @NotNull String key) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) {
            return null;
        }
        return requirementRegistry.parseRequirementConfig(sectionToMap(section));
    }

    private @NotNull Map<String, RequirementList> parseArgRequirements(@NotNull YamlConfiguration config) {
        Map<String, RequirementList> result = new HashMap<>();
        ConfigurationSection argReqSection = config.getConfigurationSection("arg_require");
        if (argReqSection == null) {
            return result;
        }

        for (String argName : argReqSection.getKeys(false)) {
            ConfigurationSection reqSection = argReqSection.getConfigurationSection(argName);
            if (reqSection == null) {
                continue;
            }
            result.put(argName, requirementRegistry.parseRequirementConfig(sectionToMap(reqSection)));
        }
        return result;
    }

    public static @NotNull List<Integer> parseSlotList(@Nullable String input, int maxSize) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        String cleaned = input.replaceAll("[\\[\\]]", "").trim();
        if (cleaned.isEmpty()) {
            return List.of();
        }

        List<Integer> slots = new ArrayList<>();
        for (String part : cleaned.split(",")) {
            parseSlotPart(part.trim(), maxSize, slots);
        }
        return slots;
    }

    public static @NotNull List<Integer> parseSlotListFromYamlList(@NotNull List<?> list, int maxSize) {
        List<Integer> slots = new ArrayList<>();
        for (Object item : list) {
            parseSlotPart(item.toString().trim(), maxSize, slots);
        }
        return slots;
    }

    private static void parseSlotPart(@NotNull String part, int maxSize, @NotNull List<Integer> slots) {
        try {
            if (part.contains("-")) {
                parseSlotRange(part, maxSize, slots);
            } else {
                parseSingleSlot(part, maxSize, slots);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static void parseSlotRange(@NotNull String part, int maxSize, @NotNull List<Integer> slots) {
        String[] range = part.split("-");
        int start = Integer.parseInt(range[0].trim());
        int end = Integer.parseInt(range[1].trim());
        for (int i = Math.max(0, start); i <= end && i < maxSize; i++) {
            slots.add(i);
        }
    }

    private static void parseSingleSlot(@NotNull String part, int maxSize, @NotNull List<Integer> slots) {
        int slot = Integer.parseInt(part);
        if (slot >= 0 && slot < maxSize) {
            slots.add(slot);
        }
    }

    private static @NotNull Map<String, Object> sectionToMap(@NotNull ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection sub) {
                map.put(key, sectionToMap(sub));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    private static void checkDuplicateKeys(@NotNull File file, @NotNull ConfigValidator validator) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            return;
        }

        checkSectionDuplicates(lines, "items", validator);
        checkSectionDuplicates(lines, "page_items", validator);
    }

    private static void checkSectionDuplicates(@NotNull List<String> lines, @NotNull String sectionName,
                                                 @NotNull ConfigValidator validator) {
        int sectionIndent = -1;
        int childIndent = -1;
        Map<String, Integer> seen = new HashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int indent = line.length() - line.stripLeading().length();

            if (sectionIndent == -1) {
                if (trimmed.equals(sectionName + ":")) {
                    sectionIndent = indent;
                }
                continue;
            }

            if (indent <= sectionIndent) {
                break;
            }

            if (childIndent == -1) {
                childIndent = indent;
            }

            if (indent != childIndent || !trimmed.endsWith(":")) {
                continue;
            }

            String key = trimmed.substring(0, trimmed.length() - 1).trim();
            if (seen.containsKey(key)) {
                validator.addError("Duplicate item name '" + key + "' under '" + sectionName
                        + "' at line " + (i + 1) + " (first at line " + seen.get(key)
                        + "). YAML only keeps the last one.");
            } else {
                seen.put(key, i + 1);
            }
        }
    }

    private static final Map<String, String> THIRD_PARTY_MATERIAL_PREFIXES = Map.of(
            "hdb-", "HeadDatabase",
            "itemsadder-", "ItemsAdder",
            "oraxen-", "Oraxen",
            "nexo-", "Nexo"
    );

    private void checkHookDependencies(@NotNull Menu menu, @NotNull String fileName) {
        HookProvider hookProvider = plugin.getHookProvider();

        if (!hookProvider.isVaultEnabled() && usesVaultActions(menu)) {
            plugin.getLogger().warning("Menu '" + menu.getName()
                    + "' uses economy/permission actions but Vault is not installed.");
        }

        if (!hookProvider.isPapiEnabled() && usesExternalPlaceholders(menu)) {
            plugin.getLogger().warning("Menu '" + menu.getName()
                    + "' uses PlaceholderAPI placeholders but PlaceholderAPI is not installed.");
        }

        for (Map.Entry<String, String> entry : THIRD_PARTY_MATERIAL_PREFIXES.entrySet()) {
            if (usesThirdPartyMaterial(menu, entry.getKey()) && !isPluginEnabled(entry.getValue())) {
                plugin.getLogger().warning("Menu '" + menu.getName()
                        + "' uses " + entry.getValue() + " items but " + entry.getValue() + " is not installed.");
            }
        }
    }

    private boolean usesThirdPartyMaterial(@NotNull Menu menu, @NotNull String prefix) {
        for (MenuItem item : collectAllMenuItems(menu)) {
            if (item.getMaterial().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPluginEnabled(@NotNull String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    private static final Set<String> VAULT_ACTION_TYPES = Set.of(
            "take_money", "takemoney", "give_money", "givemoney",
            "give_perm", "givepermission", "take_perm", "takepermission"
    );

    private boolean usesVaultActions(@NotNull Menu menu) {
        List<Action> allActions = collectAllActions(menu);
        for (Action action : allActions) {
            if (VAULT_ACTION_TYPES.contains(action.getType().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static final Set<String> BUILTIN_PLACEHOLDERS = Set.of(
            "%player%", "%player_name%"
    );

    private boolean usesExternalPlaceholders(@NotNull Menu menu) {
        if (containsExternalPlaceholder(menu.getTitle())) {
            return true;
        }

        for (MenuItem item : collectAllMenuItems(menu)) {
            if (menuItemUsesExternalPlaceholders(item)) {
                return true;
            }
        }

        return false;
    }

    private boolean menuItemUsesExternalPlaceholders(@NotNull MenuItem item) {
        if (item.getDisplayName() != null && containsExternalPlaceholder(item.getDisplayName())) {
            return true;
        }
        if (item.getLore() != null) {
            for (String line : item.getLore()) {
                if (containsExternalPlaceholder(line)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsExternalPlaceholder(@NotNull String text) {
        int index = 0;
        while ((index = text.indexOf('%', index)) != -1) {
            int end = text.indexOf('%', index + 1);
            if (end == -1) {
                break;
            }
            String placeholder = text.substring(index, end + 1);
            if (!BUILTIN_PLACEHOLDERS.contains(placeholder.toLowerCase())) {
                return true;
            }
            index = end + 1;
        }
        return false;
    }

    private static @NotNull List<MenuItem> collectAllMenuItems(@NotNull Menu menu) {
        List<MenuItem> allItems = new ArrayList<>();
        for (List<MenuItem> slotItems : menu.getItems().values()) {
            allItems.addAll(slotItems);
        }
        allItems.addAll(menu.getPageItems());
        return allItems;
    }

    private static @NotNull List<Action> collectAllActions(@NotNull Menu menu) {
        List<Action> actions = new ArrayList<>();
        actions.addAll(menu.getOnOpen());
        actions.addAll(menu.getOnClose());

        for (MenuItem item : collectAllMenuItems(menu)) {
            collectItemActions(item, actions);
        }

        return actions;
    }

    private static void collectItemActions(@NotNull MenuItem item, @NotNull List<Action> actions) {
        actions.addAll(item.getClickActions());
        actions.addAll(item.getLeftClickActions());
        actions.addAll(item.getRightClickActions());
        actions.addAll(item.getShiftLeftClickActions());
        actions.addAll(item.getShiftRightClickActions());
        actions.addAll(item.getMiddleClickActions());
    }

    private static @NotNull InventoryType parseInventoryType(@Nullable String type) {
        if (type == null || type.equalsIgnoreCase("CHEST")) {
            return InventoryType.CHEST;
        }
        if (type.equalsIgnoreCase("WORKBENCH")) {
            return InventoryType.WORKBENCH;
        }
        try {
            return InventoryType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return InventoryType.CHEST;
        }
    }
}
