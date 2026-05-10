package me.auvq.aumenus.editor;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.config.MenuLoader;
import me.auvq.aumenus.hook.HeadDatabaseHook;
import me.auvq.aumenus.hook.ItemsAdderHook;
import me.auvq.aumenus.hook.NexoHook;
import me.auvq.aumenus.hook.OraxenHook;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.menu.MenuHolder;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MenuEditor {

    private static final Set<UUID> EDITING = ConcurrentHashMap.newKeySet();
    private static final NamespacedKey ITEM_KEY = new NamespacedKey("aumenus", "editor_item");

    private static final Method GET_ITEM_MODEL;
    private static final Method GET_TOOLTIP_STYLE;
    private static final Method HAS_ITEM_NAME;
    private static final Method GET_ITEM_NAME;

    static {
        GET_ITEM_MODEL = findMethod("getItemModel");
        GET_TOOLTIP_STYLE = findMethod("getTooltipStyle");
        HAS_ITEM_NAME = findMethod("hasItemName");
        GET_ITEM_NAME = findMethod("itemName");
    }

    private static @Nullable Method findMethod(@NotNull String name) {
        try {
            return ItemMeta.class.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private final AuMenus plugin;

    public MenuEditor(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    public boolean isEditing(@NotNull Player player) {
        return EDITING.contains(player.getUniqueId());
    }

    public void openEditor(@NotNull Player player, @NotNull String menuName, int size) {
        if (EDITING.contains(player.getUniqueId())) {
            player.sendMessage(Util.parse("&cYou're already editing a menu."));
            return;
        }

        File menuFile = new File(plugin.getDataFolder(), "menus/" + menuName + ".yml");
        InventoryType invType = loadInventoryType(menuFile);

        Inventory editor;
        if (invType == InventoryType.CHEST) {
            editor = Bukkit.createInventory(null, size, Util.parse("&8Editor: " + menuName));
        } else {
            editor = Bukkit.createInventory(null, invType, Util.parse("&8Editor: " + menuName));
        }

        if (menuFile.exists()) {
            loadItemsIntoEditor(editor, menuFile, player);
        }

        EDITING.add(player.getUniqueId());
        Listener listener = createEditorListener(player, editor, menuName, menuFile);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        player.getScheduler().run(plugin, task -> {
            plugin.getMenuRegistry().getOpenMenu(player.getUniqueId())
                    .ifPresent(h -> h.setReloading(true));
            player.openInventory(editor);
        }, null);
    }

    private @NotNull InventoryType loadInventoryType(@NotNull File menuFile) {
        if (!menuFile.exists()) {
            return InventoryType.CHEST;
        }

        YamlConfiguration tempConfig = YamlConfiguration.loadConfiguration(menuFile);
        String typeStr = tempConfig.getString("type");
        if (typeStr == null) {
            return InventoryType.CHEST;
        }

        try {
            return InventoryType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown inventory type: " + typeStr);
            return InventoryType.CHEST;
        }
    }

    private Listener createEditorListener(@NotNull Player player,
                                           @NotNull Inventory editor,
                                           @NotNull String menuName,
                                           @NotNull File menuFile) {
        return new Listener() {
            @EventHandler
            public void onClose(InventoryCloseEvent event) {
                if (event.getInventory() != editor) {
                    return;
                }
                if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    return;
                }

                EDITING.remove(player.getUniqueId());
                HandlerList.unregisterAll(this);

                saveEditorChanges(editor, menuName, menuFile);
                player.sendMessage(Util.parse("&aMenu '" + menuName + "' saved."));

                Menu menu = plugin.getMenuLoader().loadMenu(menuFile);
                if (menu == null) {
                    return;
                }
                plugin.getMenuRegistry().register(menu);
            }
        };
    }

    private void saveEditorChanges(@NotNull Inventory editor,
                                    @NotNull String menuName,
                                    @NotNull File menuFile) {
        YamlConfiguration config = menuFile.exists()
                ? YamlConfiguration.loadConfiguration(menuFile)
                : new YamlConfiguration();

        if (!config.contains("title")) {
            config.set("title", "&8" + menuName);
        }
        config.set("size", editor.getSize());

        Set<String> existingNames = collectExistingItemNames(config);
        Map<String, List<Integer>> itemSlots = new LinkedHashMap<>();
        Set<String> writtenNames = new LinkedHashSet<>();

        for (int slot = 0; slot < editor.getSize(); slot++) {
            ItemStack item = editor.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            String itemName = getEditorTag(item);
            if (itemName != null && existingNames.contains(itemName)) {
                itemSlots.computeIfAbsent(itemName, k -> new ArrayList<>()).add(slot);
                writtenNames.add(itemName);
                continue;
            }

            String newName = "item_" + slot;
            saveNewItem(config, newName, item, slot);
            writtenNames.add(newName);
        }

        for (Map.Entry<String, List<Integer>> entry : itemSlots.entrySet()) {
            updateItemSlots(config, entry.getKey(), entry.getValue());
        }

        for (String itemName : existingNames) {
            if (writtenNames.contains(itemName)) {
                continue;
            }
            config.set("items." + itemName, null);
        }

        menuFile.getParentFile().mkdirs();
        try {
            config.save(menuFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save editor menu: " + e.getMessage());
        }
    }

    private @NotNull Set<String> collectExistingItemNames(@NotNull YamlConfiguration config) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(itemsSection.getKeys(false));
    }

    private void updateItemSlots(@NotNull YamlConfiguration config, @NotNull String itemName,
                                  @NotNull List<Integer> newSlots) {
        String path = "items." + itemName;
        if (newSlots.size() == 1) {
            config.set(path + ".slot", newSlots.getFirst());
            config.set(path + ".slots", null);
        } else {
            config.set(path + ".slot", null);
            config.set(path + ".slots", newSlots);
        }
    }

    private void saveNewItem(@NotNull YamlConfiguration config, @NotNull String name,
                              @NotNull ItemStack item, int slot) {
        String path = "items." + name;
        config.set(path + ".material", resolveMaterialString(item));
        config.set(path + ".slot", slot);
        if (item.getAmount() != 1) {
            config.set(path + ".amount", item.getAmount());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        captureMetaSafely(config, path, meta, name, "display text", () -> saveDisplayText(config, path, meta));
        captureMetaSafely(config, path, meta, name, "model properties", () -> saveModelProperties(config, path, meta));
        captureMetaSafely(config, path, meta, name, "enchantments", () -> saveEnchantments(config, path, meta));
        captureMetaSafely(config, path, meta, name, "item flags", () -> saveItemFlags(config, path, meta));
        captureMetaSafely(config, path, meta, name, "misc properties", () -> saveMiscProperties(config, path, meta));
        captureMetaSafely(config, path, meta, name, "color properties", () -> saveColorProperties(config, path, meta));
    }

    private void captureMetaSafely(@NotNull YamlConfiguration config, @NotNull String path,
                                     @NotNull ItemMeta meta, @NotNull String itemName,
                                     @NotNull String section, @NotNull Runnable capture) {
        try {
            capture.run();
        } catch (RuntimeException | LinkageError e) {
            plugin.getLogger().warning("Editor: skipped " + section + " for '" + itemName
                    + "' (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        }
    }

    private void saveDisplayText(@NotNull YamlConfiguration config, @NotNull String path,
                                   @NotNull ItemMeta meta) {
        MiniMessage mm = MiniMessage.miniMessage();

        if (meta.hasDisplayName() && meta.displayName() != null) {
            config.set(path + ".name", mm.serialize(meta.displayName()));
        } else if (HAS_ITEM_NAME != null && GET_ITEM_NAME != null) {
            try {
                if (Boolean.TRUE.equals(HAS_ITEM_NAME.invoke(meta))) {
                    Object itemName = GET_ITEM_NAME.invoke(meta);
                    if (itemName instanceof net.kyori.adventure.text.Component component) {
                        config.set(path + ".name", mm.serialize(component));
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        if (meta.hasLore() && meta.lore() != null) {
            List<String> lore = meta.lore().stream()
                    .map(mm::serialize)
                    .toList();
            config.set(path + ".lore", lore);
        }
    }

    private void saveModelProperties(@NotNull YamlConfiguration config, @NotNull String path,
                                       @NotNull ItemMeta meta) {
        if (meta.hasCustomModelData()) {
            config.set(path + ".model_data", meta.getCustomModelData());
        }

        NamespacedKey itemModel = invokeNamespacedKey(GET_ITEM_MODEL, meta);
        if (itemModel != null) {
            config.set(path + ".item_model", itemModel.toString());
        }

        NamespacedKey tooltipStyle = invokeNamespacedKey(GET_TOOLTIP_STYLE, meta);
        if (tooltipStyle != null) {
            config.set(path + ".tooltip_style", tooltipStyle.toString());
        }
    }

    private @Nullable NamespacedKey invokeNamespacedKey(@Nullable Method method, @NotNull ItemMeta meta) {
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(meta);
            return result instanceof NamespacedKey key ? key : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private void saveEnchantments(@NotNull YamlConfiguration config, @NotNull String path,
                                    @NotNull ItemMeta meta) {
        if (!meta.hasEnchants()) {
            return;
        }
        List<String> enchants = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            enchants.add(entry.getKey().getKey().getKey() + ";" + entry.getValue());
        }
        config.set(path + ".enchantments", enchants);
    }

    private void saveItemFlags(@NotNull YamlConfiguration config, @NotNull String path,
                                 @NotNull ItemMeta meta) {
        Set<ItemFlag> flags = meta.getItemFlags();
        if (flags.isEmpty()) {
            return;
        }
        List<String> flagNames = new ArrayList<>(flags.size());
        for (ItemFlag flag : flags) {
            flagNames.add(flag.name());
        }
        config.set(path + ".item_flags", flagNames);
    }

    private void saveMiscProperties(@NotNull YamlConfiguration config, @NotNull String path,
                                      @NotNull ItemMeta meta) {
        if (meta.isUnbreakable()) {
            config.set(path + ".unbreakable", true);
        }
        if (meta.hasRarity()) {
            config.set(path + ".rarity", meta.getRarity().name());
        }
        if (meta.isHideTooltip()) {
            config.set(path + ".hide_tooltip", true);
        }
        if (meta instanceof Damageable damageable && damageable.hasDamage()) {
            config.set(path + ".damage", damageable.getDamage());
        }
    }

    private void saveColorProperties(@NotNull YamlConfiguration config, @NotNull String path,
                                       @NotNull ItemMeta meta) {
        Color color = null;
        if (meta instanceof LeatherArmorMeta leather) {
            color = leather.getColor();
        } else if (meta instanceof PotionMeta potion) {
            color = potion.getColor();
        }
        if (color == null) {
            return;
        }
        config.set(path + ".rgb", color.getRed() + "," + color.getGreen() + "," + color.getBlue());
    }

    private @NotNull String resolveMaterialString(@NotNull ItemStack item) {
        String resolved = doResolveMaterial(item);
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("[Editor] " + item.getType().name() + " saved as material '" + resolved + "'");
        }
        return resolved;
    }

    private @NotNull String doResolveMaterial(@NotNull ItemStack item) {
        String pluginId = resolvePluginItemId(item);
        if (pluginId != null) {
            return pluginId;
        }
        if (item.getType() != Material.PLAYER_HEAD) {
            return item.getType().name();
        }
        return resolveHeadMaterial(item);
    }

    private @Nullable String resolvePluginItemId(@NotNull ItemStack item) {
        if (isPluginLoaded("Nexo")) {
            String id = safeIdLookup(() -> NexoHook.idFromItem(item));
            if (id != null) {
                return "nexo-" + id;
            }
        }
        if (isPluginLoaded("ItemsAdder")) {
            String id = safeIdLookup(() -> ItemsAdderHook.idFromItem(item));
            if (id != null) {
                return "itemsadder-" + id;
            }
        }
        if (isPluginLoaded("Oraxen")) {
            String id = safeIdLookup(() -> OraxenHook.idFromItem(item));
            if (id != null) {
                return "oraxen-" + id;
            }
        }
        if (item.getType() == Material.PLAYER_HEAD && isPluginLoaded("HeadDatabase")) {
            String id = safeIdLookup(() -> HeadDatabaseHook.idFromItem(item));
            if (id != null && !id.isEmpty()) {
                return "hdb-" + id;
            }
        }
        return null;
    }

    private boolean isPluginLoaded(@NotNull String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }

    private @Nullable String safeIdLookup(@NotNull Supplier<String> lookup) {
        try {
            String id = lookup.get();
            return id != null && !id.isEmpty() ? id : null;
        } catch (LinkageError e) {
            return null;
        }
    }

    private @NotNull String resolveHeadMaterial(@NotNull ItemStack item) {
        if (!(item.getItemMeta() instanceof SkullMeta skullMeta)) {
            return Material.PLAYER_HEAD.name();
        }

        PlayerProfile profile = skullMeta.getPlayerProfile();
        if (profile == null) {
            return Material.PLAYER_HEAD.name();
        }

        for (ProfileProperty property : profile.getProperties()) {
            if (!"textures".equals(property.getName())) {
                continue;
            }
            String value = property.getValue();
            if (value != null && !value.isEmpty()) {
                return "basehead-" + value;
            }
        }

        String ownerName = profile.getName();
        if (ownerName != null && !ownerName.isEmpty()) {
            return "head-" + ownerName;
        }

        return Material.PLAYER_HEAD.name();
    }

    private void loadItemsIntoEditor(@NotNull Inventory editor, @NotNull File menuFile,
                                      @NotNull Player player) {
        Menu menu = plugin.getMenuLoader().loadMenu(menuFile);
        if (menu == null) {
            return;
        }

        MenuHolder tempHolder = new MenuHolder(menu, player, Map.of());
        plugin.getMenuRenderer().render(tempHolder);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(menuFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }

        for (String itemName : itemsSection.getKeys(false)) {
            loadSingleEditorItem(itemsSection, itemName, tempHolder, editor);
        }
    }

    private void loadSingleEditorItem(@NotNull ConfigurationSection itemsSection,
                                       @NotNull String itemName,
                                       @NotNull MenuHolder tempHolder,
                                       @NotNull Inventory editor) {
        ConfigurationSection section = itemsSection.getConfigurationSection(itemName);
        if (section == null) {
            return;
        }

        List<Integer> slots = parseItemSlots(section, editor.getSize());
        for (int slot : slots) {
            placeRenderedItem(tempHolder, editor, slot, itemName);
        }
    }

    private void placeRenderedItem(@NotNull MenuHolder tempHolder, @NotNull Inventory editor,
                                    int slot, @NotNull String itemName) {
        ItemStack rendered = tempHolder.getInventory().getItem(slot);
        if (rendered == null || rendered.getType() == Material.AIR) {
            return;
        }

        ItemStack tagged = rendered.clone();
        tagEditorItem(tagged, itemName);
        editor.setItem(slot, tagged);
    }

    private void tagEditorItem(@NotNull ItemStack item, @NotNull String itemName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, itemName);
        item.setItemMeta(meta);
    }

    private @Nullable String getEditorTag(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }

    private @NotNull List<Integer> parseItemSlots(@NotNull ConfigurationSection section, int maxSize) {
        if (section.contains("slot")) {
            int slot = section.getInt("slot", -1);
            if (slot >= 0 && slot < maxSize) {
                return List.of(slot);
            }
            return List.of();
        }

        if (!section.contains("slots")) {
            return List.of();
        }

        Object slotsObj = section.get("slots");
        if (slotsObj instanceof String str) {
            return MenuLoader.parseSlotList(str, maxSize);
        }
        if (slotsObj instanceof List<?> list) {
            return MenuLoader.parseSlotListFromYamlList(list, maxSize);
        }
        return List.of();
    }
}
