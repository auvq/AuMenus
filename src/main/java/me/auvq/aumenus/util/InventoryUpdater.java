package me.auvq.aumenus.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryUpdater {

    private static final int TITLE_CACHE_CAP = 512;
    private static final Map<String, Object> TITLE_CACHE = new ConcurrentHashMap<>();

    private static volatile boolean available;
    private static MethodHandle getHandleHandle;
    private static MethodHandle containerMenuGetter;
    private static MethodHandle containerIdGetter;
    private static MethodHandle connectionGetter;
    private static MethodHandle sendHandle;
    private static MethodHandle incrementStateIdHandle;
    private static MethodHandle openScreenConstructor;
    private static MethodHandle containerSetContentConstructor;
    private static MethodHandle bundleConstructor;
    private static MethodHandle asNmsCopyHandle;
    private static MethodHandle asVanillaHandle;
    private static Object[] menuTypes;
    private static Object emptyNmsItem;

    static {
        bootstrap();
    }

    private InventoryUpdater() {
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean sendSmoothUpdate(@NotNull Player viewer,
                                            @NotNull Inventory inventory,
                                            @Nullable Component title,
                                            @Nullable String titleRaw) {
        if (!available) {
            return false;
        }

        int size = inventory.getSize();
        if (size <= 0 || size % 9 != 0) {
            return false;
        }

        int menuIndex = (size / 9) - 1;
        if (menuIndex < 0 || menuIndex >= menuTypes.length) {
            return false;
        }

        boolean includeTitle = title != null && titleRaw != null;

        try {
            Object handle = getHandleHandle.invoke(viewer);
            Object containerMenu = containerMenuGetter.invoke(handle);
            int containerId = (int) containerIdGetter.invoke(containerMenu);
            int stateId = (int) incrementStateIdHandle.invoke(containerMenu);

            List<Object> nmsItems = buildNmsItemList(inventory, size);
            Object contentPacket = containerSetContentConstructor.invoke(
                    containerId, stateId, nmsItems, emptyNmsItem);

            List<Object> packets = new ArrayList<>(includeTitle ? 2 : 1);
            if (includeTitle) {
                Object nmsTitle = resolveNmsTitle(title, titleRaw);
                Object openScreenPacket = openScreenConstructor.invoke(
                        containerId, menuTypes[menuIndex], nmsTitle);
                packets.add(openScreenPacket);
            }
            packets.add(contentPacket);

            Object bundle = bundleConstructor.invoke((Iterable<Object>) packets);
            Object connection = connectionGetter.invoke(handle);
            sendHandle.invoke(connection, bundle);
            return true;
        } catch (Throwable failure) {
            Bukkit.getLogger().log(Level.WARNING,
                    "InventoryUpdater disabled after NMS failure; falling back to normal menu opens",
                    failure);
            available = false;
            return false;
        }
    }

    private static @NotNull List<Object> buildNmsItemList(@NotNull Inventory inventory, int size) throws Throwable {
        List<Object> items = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) {
                items.add(emptyNmsItem);
                continue;
            }
            items.add(asNmsCopyHandle.invoke(stack));
        }
        return items;
    }

    private static @NotNull Object resolveNmsTitle(@NotNull Component title,
                                                     @NotNull String titleRaw) throws Throwable {
        Object cached = TITLE_CACHE.get(titleRaw);
        if (cached != null) {
            return cached;
        }
        Object converted = asVanillaHandle.invoke(title);
        if (TITLE_CACHE.size() < TITLE_CACHE_CAP) {
            TITLE_CACHE.put(titleRaw, converted);
        }
        return converted;
    }

    private static void bootstrap() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            getHandleHandle = lookup.findVirtual(craftPlayerClass, "getHandle",
                    MethodType.methodType(serverPlayerClass));

            Field containerMenuField = serverPlayerClass.getField("containerMenu");
            Field connectionField = serverPlayerClass.getField("connection");
            containerMenuGetter = lookup.unreflectGetter(containerMenuField);
            connectionGetter = lookup.unreflectGetter(connectionField);

            Class<?> containerClass = Class.forName("net.minecraft.world.inventory.AbstractContainerMenu");
            containerIdGetter = lookup.unreflectGetter(containerClass.getField("containerId"));
            incrementStateIdHandle = lookup.findVirtual(containerClass, "incrementStateId",
                    MethodType.methodType(int.class));

            Class<?> menuTypeClass = Class.forName("net.minecraft.world.inventory.MenuType");
            menuTypes = new Object[6];
            for (int rowIndex = 0; rowIndex < 6; rowIndex++) {
                Field field = menuTypeClass.getField("GENERIC_9x" + (rowIndex + 1));
                menuTypes[rowIndex] = field.get(null);
            }

            Class<?> nmsComponentClass = Class.forName("net.minecraft.network.chat.Component");
            Class<?> openScreenClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundOpenScreenPacket");
            openScreenConstructor = lookup.findConstructor(openScreenClass,
                    MethodType.methodType(void.class, int.class, menuTypeClass, nmsComponentClass));

            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.Packet");
            sendHandle = findSendHandle(lookup, connectionField.getType(), packetClass);

            Class<?> bundleClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundBundlePacket");
            bundleConstructor = lookup.findConstructor(bundleClass,
                    MethodType.methodType(void.class, Iterable.class));

            Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            emptyNmsItem = nmsItemStackClass.getField("EMPTY").get(null);

            Class<?> setContentClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket");
            containerSetContentConstructor = lookup.findConstructor(setContentClass,
                    MethodType.methodType(void.class, int.class, int.class, List.class, nmsItemStackClass));

            Class<?> craftItemStackClass = Class.forName(
                    "org.bukkit.craftbukkit.inventory.CraftItemStack");
            asNmsCopyHandle = lookup.findStatic(craftItemStackClass, "asNMSCopy",
                    MethodType.methodType(nmsItemStackClass, ItemStack.class));

            Class<?> paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            asVanillaHandle = lookup.findStatic(paperAdventureClass, "asVanilla",
                    MethodType.methodType(nmsComponentClass, Component.class));

            available = true;
        } catch (ReflectiveOperationException failure) {
            available = false;
        }
    }

    private static @NotNull MethodHandle findSendHandle(@NotNull MethodHandles.Lookup lookup,
                                                          @NotNull Class<?> connectionClass,
                                                          @NotNull Class<?> packetClass)
            throws ReflectiveOperationException {
        Class<?> current = connectionClass;
        while (current != null) {
            try {
                return lookup.findVirtual(current, "send",
                        MethodType.methodType(void.class, packetClass));
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException("send(Packet) not found in " + connectionClass.getName());
    }
}
