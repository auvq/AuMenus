package me.auvq.aumenus.listener;

import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.action.ActionRegistry;
import me.auvq.aumenus.api.event.MenuCloseEvent;
import me.auvq.aumenus.item.MenuItem;
import me.auvq.aumenus.menu.MenuHolder;
import me.auvq.aumenus.requirement.Requirement;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.requirement.RequirementRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class MenuListener implements Listener {

    private final AuMenus plugin;
    private final ActionRegistry actionRegistry;
    private final RequirementRegistry requirementRegistry;
    private final long cooldownMs;

    private record DenyContext(String needed, String current, String remaining, String input, String output) {}

    public MenuListener(@NotNull AuMenus plugin,
                        @NotNull ActionRegistry actionRegistry,
                        @NotNull RequirementRegistry requirementRegistry) {
        this.plugin = plugin;
        this.actionRegistry = actionRegistry;
        this.requirementRegistry = requirementRegistry;
        this.cooldownMs = plugin.getConfig().getInt("click_cooldown", 2) * 50L;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (holder.isUpdating()) {
            return;
        }
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }

        int slot = event.getRawSlot();
        MenuItem item = holder.getActiveItem(slot);
        if (item == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long effectiveCooldown = holder.getMenu().getClickCooldown() >= 0
                ? holder.getMenu().getClickCooldown() * 50L : this.cooldownMs;
        if (now - holder.getLastClickTime() < effectiveCooldown) {
            return;
        }
        holder.setLastClickTime(now);

        List<Action> actions = resolveActions(item, event.getClick());
        RequirementList requirements = resolveRequirements(item, event.getClick());

        if (requirements != null) {
            RequirementList resolved = plugin.getMenuRenderer()
                    .resolveRequirementPlaceholders(requirements, player, holder);
            RequirementList.EvaluationResult result = resolved.evaluateDetailed(player, requirementRegistry);
            if (!result.passed()) {
                handleDenyActions(player, resolved, result.failed());
                handlePerCheckDeny(player, result.failedOptional());
                return;
            }
            handlePerCheckDeny(player, result.failedOptional());
            handleSuccessActions(player, resolved, result.passed_list());
        }

        if (!actions.isEmpty()) {
            actionRegistry.executeActions(player, actions);
        }
    }

    @EventHandler
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        holder.stopUpdateTask();
        MenuHolder current = plugin.getMenuRegistry().getOpenMenu(player.getUniqueId()).orElse(null);
        if (current == holder) {
            plugin.getMenuRegistry().trackClose(player.getUniqueId());
        }

        if (holder.isReloading()) {
            return;
        }

        if (!holder.getMenu().getOnClose().isEmpty()) {
            actionRegistry.executeActions(player, holder.getMenu().getOnClose());
        }

        new MenuCloseEvent(player, holder.getMenu()).callEvent();
    }

    private @NotNull List<Action> resolveActions(@NotNull MenuItem item,
                                                  @NotNull ClickType clickType) {
        List<Action> specific = switch (clickType) {
            case SHIFT_LEFT -> item.getShiftLeftClickActions();
            case SHIFT_RIGHT -> item.getShiftRightClickActions();
            case LEFT -> item.getLeftClickActions();
            case RIGHT -> item.getRightClickActions();
            case MIDDLE -> item.getMiddleClickActions();
            default -> List.of();
        };
        return specific.isEmpty() ? item.getClickActions() : specific;
    }

    private RequirementList resolveRequirements(@NotNull MenuItem item, @NotNull ClickType clickType) {
        RequirementList specific = switch (clickType) {
            case SHIFT_LEFT -> item.getShiftLeftClickRequire();
            case SHIFT_RIGHT -> item.getShiftRightClickRequire();
            case LEFT -> item.getLeftClickRequire();
            case RIGHT -> item.getRightClickRequire();
            case MIDDLE -> item.getMiddleClickRequire();
            default -> null;
        };
        return specific != null ? specific : item.getClickRequire();
    }

    private void handleSuccessActions(@NotNull Player player,
                                       @NotNull RequirementList requirements,
                                       @NotNull List<Requirement> passed) {
        for (Requirement req : passed) {
            if (!req.getSuccessActions().isEmpty()) {
                actionRegistry.executeActions(player, req.getSuccessActions());
            }
        }
        if (!requirements.getSuccessActions().isEmpty()) {
            actionRegistry.executeActions(player, requirements.getSuccessActions());
        }
    }

    private void handlePerCheckDeny(@NotNull Player player,
                                      @NotNull List<Requirement> failedOptional) {
        for (Requirement req : failedOptional) {
            if (!req.getDenyActions().isEmpty()) {
                DenyContext context = computeDenyContext(player, req);
                actionRegistry.executeActions(player, applyDenyReplacements(req.getDenyActions(), context));
            }
        }
    }

    private void handleDenyActions(@NotNull Player player,
                                    @NotNull RequirementList requirements,
                                    @NotNull List<Requirement> failed) {
        for (Requirement req : failed) {
            if (req.getDenyActions().isEmpty()) {
                continue;
            }
            DenyContext context = computeDenyContext(player, req);
            actionRegistry.executeActions(player, applyDenyReplacements(req.getDenyActions(), context));
            return;
        }

        if (requirements.getDenyActions().isEmpty()) {
            return;
        }

        if (failed.isEmpty()) {
            actionRegistry.executeActions(player, requirements.getDenyActions());
            return;
        }

        Requirement firstFailed = failed.getFirst();
        DenyContext context = computeDenyContext(player, firstFailed);
        actionRegistry.executeActions(player, applyDenyReplacements(requirements.getDenyActions(), context));
    }

    private @NotNull DenyContext computeDenyContext(@NotNull Player player, @NotNull Requirement req) {
        String type = req.getType().toLowerCase();
        Map<String, Object> config = req.getConfig();
        String configInput = config.containsKey("input") ? config.get("input").toString() : "";
        String configOutput = config.containsKey("output") ? config.get("output").toString() : "";

        try {
            return switch (type) {
                case "has_money", "has money" -> {
                    double amount = parseNumber(config.get("amount"), 0).doubleValue();
                    double balance = plugin.getHookProvider().isVaultEnabled()
                            ? plugin.getHookProvider().vault().getBalance(player) : 0;
                    yield new DenyContext(formatMoney(amount), formatMoney(balance),
                            formatMoney(Math.max(0, amount - balance)), configInput, configOutput);
                }
                case "has_exp", "has exp" -> {
                    int amount = parseNumber(config.get("amount"), 0).intValue();
                    boolean levels = Boolean.TRUE.equals(config.get("level"));
                    int current = levels ? player.getLevel() : player.getTotalExperience();
                    yield new DenyContext(String.valueOf(amount), String.valueOf(current),
                            String.valueOf(Math.max(0, amount - current)), configInput, configOutput);
                }
                case "has_item", "has item" -> {
                    int amount = parseNumber(config.get("amount"), 1).intValue();
                    yield new DenyContext(String.valueOf(amount), "", String.valueOf(amount), configInput, configOutput);
                }
                default -> new DenyContext("", "", "", configInput, configOutput);
            };
        } catch (RuntimeException e) {
            return new DenyContext("", "", "", configInput, configOutput);
        }
    }

    private static @NotNull Number parseNumber(@Nullable Object value, double fallback) {
        if (value instanceof Number num) {
            return num;
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private @NotNull List<Action> applyDenyReplacements(@NotNull List<Action> actions, @NotNull DenyContext context) {
        return actions.stream()
                .map(action -> new Action(
                        action.getType(),
                        action.getValue()
                                .replace("{needed}", context.needed())
                                .replace("{has}", context.current())
                                .replace("{remaining}", context.remaining())
                                .replace("{input}", context.input())
                                .replace("{output}", context.output()),
                        action.getDelay(),
                        action.getChance()))
                .toList();
    }

    private static @NotNull String formatMoney(double amount) {
        if (amount == Math.floor(amount)) {
            return String.format("%,.0f", amount);
        }
        return String.format("%,.2f", amount);
    }
}
