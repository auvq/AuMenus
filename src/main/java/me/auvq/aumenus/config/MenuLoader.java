package me.auvq.aumenus.config;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.action.ActionRegistry;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.menu.MenuRegistry;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.requirement.RequirementRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            if (menu.hasErrors()) {
                for (String error : menu.getLoadErrors()) {
                    plugin.getLogger().warning(error);
                }
            }
        }
        return loaded;
    }

    public @Nullable Menu loadMenu(@NotNull File file) {
        String fileName = file.getName();
        String menuName = fileName.replace(".yml", "").replace(".yaml", "");

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigValidator validator = new ConfigValidator(fileName);

            String title = config.getString("title", "&8" + menuName);
            String typeStr = config.getString("type", "CHEST");
            validator.validateInventoryType(typeStr);
            InventoryType invType = parseInventoryType(typeStr);
            int size = config.getInt("size", 54);
            if (invType == InventoryType.CHEST) {
                validator.validateSize(size);
            }

            String command = config.getString("command");
            boolean registerCommand = config.getBoolean("register_command", true);
            int updateInterval = config.getInt("update_interval",
                    plugin.getConfig().getInt("default_update_interval", 20));

            List<String> args = config.getStringList("args");
            String argsUsage = config.getString("args_usage");

            Map<String, RequirementList> argRequirements = parseArgRequirements(config);
            RequirementList openRequire = parseRequirementSection(config, "open_require");
            List<Action> onOpen = actionRegistry.parseActions(config.getList("on_open"));
            List<Action> onClose = actionRegistry.parseActions(config.getList("on_close"));

            Map<Integer, List<MenuItem>> items = parseItems(config, size, validator);
            List<Integer> pageSlots = parseSlotList(config.getString("page_slots", ""), size);
            List<MenuItem> pageItems = parsePageItems(config, size, validator);

            return Menu.builder()
                    .name(menuName)
                    .title(title)
                    .size(size)
                    .inventoryType(invType)
                    .command(command)
                    .registerCommand(registerCommand)
                    .updateInterval(updateInterval)
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

        for (String itemName : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemName);
            if (itemSection == null) {
                continue;
            }

            MenuItem menuItem = parseMenuItem(itemName, itemSection, menuSize, validator);
            for (int slot : menuItem.getSlots()) {
                items.computeIfAbsent(slot, k -> new ArrayList<>()).add(menuItem);
            }
        }

        for (List<MenuItem> slotItems : items.values()) {
            slotItems.sort(Comparator.comparingInt(MenuItem::getPriority));
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
            pageItems.add(parseMenuItem(itemName, itemSection, menuSize, validator));
        }

        return pageItems;
    }

    private @NotNull MenuItem parseMenuItem(@NotNull String name,
                                             @NotNull ConfigurationSection section,
                                             int menuSize,
                                             @NotNull ConfigValidator validator) {
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
            if (reqSection != null) {
                result.put(argName, requirementRegistry.parseRequirementConfig(sectionToMap(reqSection)));
            }
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
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = Math.max(0, start); i <= end && i < maxSize; i++) {
                    slots.add(i);
                }
            } else {
                int slot = Integer.parseInt(part);
                if (slot >= 0 && slot < maxSize) {
                    slots.add(slot);
                }
            }
        }
        return slots;
    }

    public static @NotNull List<Integer> parseSlotListFromYamlList(@NotNull List<?> list, int maxSize) {
        List<Integer> slots = new ArrayList<>();
        for (Object item : list) {
            String str = item.toString().trim();
            if (str.contains("-")) {
                String[] range = str.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = Math.max(0, start); i <= end && i < maxSize; i++) {
                    slots.add(i);
                }
            } else {
                int slot = Integer.parseInt(str);
                if (slot >= 0 && slot < maxSize) {
                    slots.add(slot);
                }
            }
        }
        return slots;
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
