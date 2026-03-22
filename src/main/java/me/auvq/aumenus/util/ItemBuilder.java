package me.auvq.aumenus.util;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.item.HeadProvider;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.menu.MenuHolder;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ItemBuilder {

    private static final Method SET_ITEM_MODEL;
    private static final Method SET_TOOLTIP_STYLE;

    static {
        Method itemModel = null;
        Method tooltipStyle = null;
        try {
            itemModel = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            tooltipStyle = ItemMeta.class.getMethod("setTooltipStyle", NamespacedKey.class);
        } catch (NoSuchMethodException ignored) {
        }
        SET_ITEM_MODEL = itemModel;
        SET_TOOLTIP_STYLE = tooltipStyle;
    }

    private final AuMenus plugin;

    public ItemBuilder(@NotNull AuMenus plugin) {
        this.plugin = plugin;
    }

    public @NotNull ItemStack buildItemStack(@NotNull Player player,
                                              @NotNull MenuItem item,
                                              @NotNull MenuHolder holder) {
        if (item.hasError()) {
            return Util.buildErrorItem(item.getName(), item.getErrorMessage());
        }

        try {
            return buildItemInternal(player, item, holder);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to build item '" + item.getName() + "': " + e.getMessage());
            return Util.buildErrorItem(item.getName(), e.getMessage());
        }
    }

    private @NotNull ItemStack buildItemInternal(@NotNull Player player,
                                                  @NotNull MenuItem item,
                                                  @NotNull MenuHolder holder) {
        String materialStr = resolve(player, item.getMaterial(), holder);
        ItemStack stack = resolveMaterial(materialStr, player);

        int amount = item.getAmount();
        if (item.getDynamicAmount() != null) {
            String resolved = resolve(player, item.getDynamicAmount(), holder);
            try {
                amount = Integer.parseInt(resolved);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid dynamic amount '" + resolved + "' for item '" + item.getName() + "'");
            }
        }
        stack.setAmount(Math.max(1, Math.min(64, amount)));

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        applyDisplayProperties(meta, item, player, holder);
        applyEnchantments(meta, item);
        applyItemFlags(meta, item);
        applyMiscProperties(meta, item);
        applyColorProperties(meta, item);
        applyBannerProperties(meta, item);
        applyPotionProperties(meta, item);
        applyArmorTrimProperties(meta, item);

        stack.setItemMeta(meta);
        return stack;
    }

    private void applyDisplayProperties(@NotNull ItemMeta meta, @NotNull MenuItem item,
                                         @NotNull Player player, @NotNull MenuHolder holder) {
        if (item.getDisplayName() != null) {
            String name = resolve(player, item.getDisplayName(), holder);
            meta.displayName(Util.parse(name));
        }

        if (item.getLore() == null) {
            return;
        }

        String appendMode = item.getLoreAppendMode() != null
                ? item.getLoreAppendMode().toUpperCase() : "OVERRIDE";

        if (appendMode.equals("IGNORE")) {
            return;
        }

        List<Component> newLore = new ArrayList<>();
        for (String line : item.getLore()) {
            String resolved = resolve(player, line, holder);
            newLore.add(Util.parse(resolved));
        }

        List<Component> existingLore = meta.lore();
        if (existingLore == null || existingLore.isEmpty() || appendMode.equals("OVERRIDE")) {
            meta.lore(newLore);
            return;
        }

        if (appendMode.equals("BOTTOM")) {
            List<Component> combined = new ArrayList<>(existingLore);
            combined.addAll(newLore);
            meta.lore(combined);
            return;
        }

        if (appendMode.equals("TOP")) {
            List<Component> combined = new ArrayList<>(newLore);
            combined.addAll(existingLore);
            meta.lore(combined);
            return;
        }

        meta.lore(newLore);
    }

    private void applyEnchantments(@NotNull ItemMeta meta, @NotNull MenuItem item) {
        if (item.getEnchantments() == null) {
            return;
        }
        for (String enchStr : item.getEnchantments()) {
            String[] parts = enchStr.split(";");
            if (parts.length < 2) {
                plugin.getLogger().warning("Invalid enchantment format: " + enchStr);
                continue;
            }
            Enchantment enchant = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.minecraft(parts[0].toLowerCase()));
            if (enchant == null) {
                plugin.getLogger().warning("Invalid enchantment format: " + enchStr);
                continue;
            }
            try {
                meta.addEnchant(enchant, Integer.parseInt(parts[1]), true);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid enchantment format: " + enchStr);
            }
        }
    }

    private void applyItemFlags(@NotNull ItemMeta meta, @NotNull MenuItem item) {
        if (item.getItemFlags() == null) {
            return;
        }
        for (String flagStr : item.getItemFlags()) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid item flag: " + flagStr);
            }
        }
    }

    private void applyMiscProperties(@NotNull ItemMeta meta, @NotNull MenuItem item) {
        if (item.getEnchantmentGlintOverride() != null) {
            meta.setEnchantmentGlintOverride(item.getEnchantmentGlintOverride());
        }
        if (item.getHideTooltip() != null) {
            meta.setHideTooltip(item.getHideTooltip());
        }
        if (item.isUnbreakable()) {
            meta.setUnbreakable(true);
        }
        if (item.getModelData() != null) {
            meta.setCustomModelData(item.getModelData());
        }
        if (item.getRarity() != null) {
            try {
                meta.setRarity(ItemRarity.valueOf(item.getRarity().toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid item rarity: " + item.getRarity());
            }
        }
        if (item.getItemModel() != null && SET_ITEM_MODEL != null) {
            try {
                SET_ITEM_MODEL.invoke(meta, NamespacedKey.fromString(item.getItemModel()));
            } catch (Exception ignored) {
            }
        }
        if (item.getTooltipStyle() != null && SET_TOOLTIP_STYLE != null) {
            try {
                SET_TOOLTIP_STYLE.invoke(meta, NamespacedKey.fromString(item.getTooltipStyle()));
            } catch (Exception ignored) {
            }
        }
        if (item.getDamage() != null && meta instanceof Damageable damageable) {
            damageable.setDamage(item.getDamage());
        }
    }

    private void applyColorProperties(@NotNull ItemMeta meta, @NotNull MenuItem item) {
        if (item.getRgb() == null) {
            return;
        }
        String[] parts = item.getRgb().split(",");
        if (parts.length != 3) {
            plugin.getLogger().warning("Invalid RGB format: " + item.getRgb());
            return;
        }
        try {
            Color color = Color.fromRGB(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()));
            if (meta instanceof LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(color);
            } else if (meta instanceof PotionMeta potionMeta) {
                potionMeta.setColor(color);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid RGB format: " + item.getRgb());
        }
    }

    private void applyBannerProperties(@NotNull ItemMeta meta, @NotNull MenuItem item) {
        if (!(meta instanceof BannerMeta bannerMeta) || item.getBannerMeta() == null) {
            return;
        }
        if (item.getBaseColor() != null) {
            try {
                bannerMeta.addPattern(new Pattern(DyeColor.valueOf(item.getBaseColor().toUpperCase()),
                        PatternType.BASE));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid banner base color: " + item.getBaseColor());
            }
        }
        for (String patternStr : item.getBannerMeta()) {
            String[] parts = patternStr.split(";");
            if (parts.length < 2) {
                plugin.getLogger().warning("Invalid banner pattern format: " + patternStr);
                continue;
            }
            try {
                DyeColor dyeColor = DyeColor.valueOf(parts[0].toUpperCase());
                PatternType patternType = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.BANNER_PATTERN)
                        .get(NamespacedKey.minecraft(parts[1].toLowerCase()));
                if (patternType != null) {
                    bannerMeta.addPattern(new Pattern(dyeColor, patternType));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid banner pattern format: " + patternStr);
            }
        }
    }

    private void applyPotionProperties(@NotNull ItemMeta meta, @NotNull MenuItem item) {
        if (!(meta instanceof PotionMeta potionMeta) || item.getPotionEffects() == null) {
            return;
        }
        for (String effectStr : item.getPotionEffects()) {
            String[] parts = effectStr.split(";");
            if (parts.length < 3) {
                plugin.getLogger().warning("Invalid potion effect format: " + effectStr);
                continue;
            }
            PotionEffectType effectType = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.MOB_EFFECT)
                    .get(NamespacedKey.minecraft(parts[0].toLowerCase()));
            if (effectType == null) {
                plugin.getLogger().warning("Invalid potion effect format: " + effectStr);
                continue;
            }
            try {
                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);
                potionMeta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid potion effect format: " + effectStr);
            }
        }
    }

    private void applyArmorTrimProperties(@NotNull ItemMeta meta, @NotNull MenuItem item) {
        if (item.getTrimMaterial() == null || item.getTrimPattern() == null) {
            return;
        }
        if (!(meta instanceof ArmorMeta armorMeta)) {
            return;
        }
        TrimMaterial trimMaterial = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_MATERIAL)
                .get(NamespacedKey.minecraft(item.getTrimMaterial().toLowerCase()));
        TrimPattern trimPattern = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_PATTERN)
                .get(NamespacedKey.minecraft(item.getTrimPattern().toLowerCase()));
        if (trimMaterial != null && trimPattern != null) {
            armorMeta.setTrim(new ArmorTrim(trimMaterial, trimPattern));
        }
    }

    private @NotNull ItemStack resolveMaterial(@NotNull String materialStr, @NotNull Player player) {
        if (materialStr.startsWith("head-")) {
            return HeadProvider.createPlayerHead(materialStr.substring(5)).clone();
        }
        if (materialStr.startsWith("basehead-")) {
            return HeadProvider.createBase64Head(materialStr.substring(9)).clone();
        }
        if (materialStr.startsWith("texture-")) {
            return HeadProvider.createTextureHead(materialStr.substring(8)).clone();
        }
        if (materialStr.equals("main_hand")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            return item.getType() == Material.AIR ? new ItemStack(Material.AIR) : item.clone();
        }
        if (materialStr.equals("off_hand")) {
            ItemStack item = player.getInventory().getItemInOffHand();
            return item.getType() == Material.AIR ? new ItemStack(Material.AIR) : item.clone();
        }
        if (materialStr.startsWith("armor_")) {
            return resolveArmorMaterial(materialStr, player);
        }
        if (materialStr.equals("air")) {
            return new ItemStack(Material.AIR);
        }
        if (materialStr.equalsIgnoreCase("water_bottle")) {
            ItemStack potion = new ItemStack(Material.POTION);
            PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
            if (potionMeta != null) {
                potionMeta.setBasePotionType(org.bukkit.potion.PotionType.WATER);
                potion.setItemMeta(potionMeta);
            }
            return potion;
        }
        if (materialStr.startsWith("placeholder-")) {
            String placeholder = materialStr.substring(12);
            String resolved = Util.resolveBuiltInPlaceholders(player, placeholder);
            if (plugin.getHookProvider().isPapiEnabled() && resolved.contains("%")) {
                resolved = plugin.getHookProvider().papi().setPlaceholders(player, resolved);
            }
            Material mat = Material.matchMaterial(resolved);
            return new ItemStack(Objects.requireNonNullElse(mat, Material.STONE));
        }

        Material material = Material.matchMaterial(materialStr);
        if (material == null) {
            material = Material.STONE;
        }
        return new ItemStack(material);
    }

    private @NotNull ItemStack resolveArmorMaterial(@NotNull String type, @NotNull Player player) {
        ItemStack item = switch (type) {
            case "armor_helmet" -> player.getInventory().getHelmet();
            case "armor_chestplate" -> player.getInventory().getChestplate();
            case "armor_leggings" -> player.getInventory().getLeggings();
            case "armor_boots" -> player.getInventory().getBoots();
            default -> null;
        };
        return item != null ? item.clone() : new ItemStack(Material.AIR);
    }

    private @NotNull String resolve(@NotNull Player player, @NotNull String text, @NotNull MenuHolder holder) {
        return Util.resolvePlaceholders(player, text, holder, plugin.getHookProvider());
    }
}
