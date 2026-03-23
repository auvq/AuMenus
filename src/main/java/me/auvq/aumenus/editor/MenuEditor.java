package me.auvq.aumenus.editor;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.config.MenuLoader;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.menu.MenuHolder;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuEditor {

    private static final Set<UUID> EDITING = ConcurrentHashMap.newKeySet();
    private static final NamespacedKey ITEM_KEY = new NamespacedKey("aumenus", "editor_item");
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
            public void onClick(InventoryClickEvent event) {
                if (event.getInventory() != editor) {
                    return;
                }
            }

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

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        Map<String, List<Integer>> itemSlots = new LinkedHashMap<>();

        for (int slot = 0; slot < editor.getSize(); slot++) {
            ItemStack item = editor.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            String itemName = getEditorTag(item);
            if (itemName != null) {
                itemSlots.computeIfAbsent(itemName, k -> new ArrayList<>()).add(slot);
                continue;
            }

            String newName = "item_" + slot;
            saveNewItem(config, newName, item, slot);
        }

        for (Map.Entry<String, List<Integer>> entry : itemSlots.entrySet()) {
            updateItemSlots(config, entry.getKey(), entry.getValue());
        }

        if (itemsSection != null) {
            for (String itemName : itemsSection.getKeys(false)) {
                if (itemSlots.containsKey(itemName)) {
                    continue;
                }
                config.set("items." + itemName, null);
            }
        }

        menuFile.getParentFile().mkdirs();
        try {
            config.save(menuFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save editor menu: " + e.getMessage());
        }
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
        config.set(path + ".material", item.getType().name());
        config.set(path + ".slot", slot);
        config.set(path + ".amount", item.getAmount());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        if (meta.hasDisplayName()) {
            config.set(path + ".name",
                    PlainTextComponentSerializer.plainText().serialize(meta.displayName()));
        }

        if (meta.hasLore() && meta.lore() != null) {
            List<String> lore = meta.lore().stream()
                    .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                    .toList();
            config.set(path + ".lore", lore);
        }
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

    private String getEditorTag(@NotNull ItemStack item) {
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
