package me.auvq.aumenus;

import lombok.Getter;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.action.ActionRegistry;
import me.auvq.aumenus.api.event.MenuCloseEvent;
import me.auvq.aumenus.api.event.MenuOpenEvent;
import me.auvq.aumenus.command.MenuCommand;
import me.auvq.aumenus.config.MenuLoader;
import me.auvq.aumenus.config.MenuMigrator;
import me.auvq.aumenus.editor.MenuEditor;
import me.auvq.aumenus.hook.HookProvider;
import me.auvq.aumenus.input.AnvilInput;
import me.auvq.aumenus.input.ChatInput;
import me.auvq.aumenus.listener.MenuListener;
import me.auvq.aumenus.listener.PlayerListener;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.menu.MenuHolder;
import me.auvq.aumenus.item.HeadProvider;
import me.auvq.aumenus.menu.MenuRegistry;
import me.auvq.aumenus.menu.MenuRenderer;
import me.auvq.aumenus.requirement.Requirement;
import me.auvq.aumenus.meta.MetaStore;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.requirement.RequirementRegistry;
import me.auvq.aumenus.requirement.RequirementType;
import me.auvq.aumenus.util.InventoryUpdater;
import me.auvq.aumenus.util.UpdateChecker;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class AuMenus extends JavaPlugin {

    @Getter
    private static AuMenus instance;

    @Getter
    private HookProvider hookProvider;
    @Getter
    private ActionRegistry actionRegistry;
    @Getter
    private RequirementRegistry requirementRegistry;
    @Getter
    private MenuRegistry menuRegistry;
    @Getter
    private MenuRenderer menuRenderer;
    @Getter
    private MenuLoader menuLoader;
    @Getter
    private MetaStore metaStore;
    @Getter
    private MenuEditor menuEditor;
    @Getter
    private AnvilInput anvilInput;
    @Getter
    private MenuMigrator menuMigrator;
    @Getter
    private ChatInput chatInput;
    @Getter
    private final Map<UUID, String> lastOpenedMenus = new ConcurrentHashMap<>();
    @Getter
    private final Map<UUID, String> previousMenus = new ConcurrentHashMap<>();

    private ScheduledTask globalUpdateTask;
    private ScheduledTask globalAnimationTask;

    @Getter
    private boolean smoothTransitions;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.smoothTransitions = getConfig().getBoolean("smooth_transitions", true);
        this.hookProvider = new HookProvider(this);
        this.metaStore = new MetaStore(this);
        this.requirementRegistry = new RequirementRegistry();
        this.actionRegistry = new ActionRegistry(this);
        this.menuRegistry = new MenuRegistry();
        this.menuRenderer = new MenuRenderer(this, requirementRegistry);
        this.menuLoader = new MenuLoader(this, actionRegistry, requirementRegistry, menuRegistry);

        this.menuEditor = new MenuEditor(this);
        this.anvilInput = new AnvilInput(this);
        this.chatInput = new ChatInput(this);
        this.menuMigrator = new MenuMigrator(this);
        new File(getDataFolder(), "migration").mkdirs();
        new File(getDataFolder(), "templates").mkdirs();
        saveDefaultMenus();

        int loaded = menuLoader.loadAll();
        getLogger().info("Loaded " + loaded + " menu(s).");
        registerMenuCommands();

        if (hookProvider.isPapiEnabled()) {
            hookProvider.papi().registerAuMenusExpansion(this);
        }

        Bukkit.getPluginManager().registerEvents(
                new MenuListener(this, actionRegistry, requirementRegistry), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        if (getConfig().getBoolean("check_updates", true)) {
            UpdateChecker updateChecker = new UpdateChecker(this);
            updateChecker.check();
            Bukkit.getPluginManager().registerEvents(updateChecker, this);
        }

        startGlobalTasks();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        //noinspection UnstableApiUsage
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            MenuCommand.register(event.registrar(), this);
            MenuCommand.registerMenuCommands(event.registrar(), this);
        });

        new Metrics(this, 30368);

        getLogger().info("AuMenus v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        closeOpenMenusOnDisable();

        if (globalUpdateTask != null) {
            globalUpdateTask.cancel();
        }
        if (globalAnimationTask != null) {
            globalAnimationTask.cancel();
        }

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("AuMenus disabled.");
    }

    private void closeOpenMenusOnDisable() {
        for (Map.Entry<UUID, MenuHolder> entry : menuRegistry.getOpenMenus().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            MenuHolder holder = entry.getValue();
            holder.setReloading(true);
            try {
                player.closeInventory();
            } catch (RuntimeException failure) {
                getLogger().warning("Failed to close menu for " + player.getName()
                        + " during disable: " + failure.getMessage());
            }
        }
    }

    public void openMenu(@NotNull Player player, @NotNull Menu menu, @NotNull Map<String, String> args) {
        openMenu(player, null, menu, args, 0);
    }

    public void openMenu(@NotNull Player player, @Nullable OfflinePlayer target, @NotNull Menu menu,
                          @NotNull Map<String, String> args) {
        openMenu(player, target, menu, args, 0);
    }

    private static final int MAX_PREDICATE_DEPTH = 5;

    private static final List<String> DEFAULT_MENU_FILES = List.of(
            "basic_menu.yml", "paginated_menu.yml", "test_actions.yml",
            "test_requirements.yml", "test_items.yml", "test_inventory_types.yml",
            "test_dispenser.yml", "test_smooth_pages.yml",
            "shop_main.yml", "shop_weapons.yml", "shop_armor.yml",
            "shop_tools.yml", "shop_blocks.yml", "shop_food.yml", "shop_misc.yml"
    );

    private void openMenu(@NotNull Player player, @Nullable OfflinePlayer target, @NotNull Menu menu,
                           @NotNull Map<String, String> args, int predicateDepth) {
        if (!player.isOnline()) {
            return;
        }

        if (isPredicateMenu(menu)) {
            handlePredicateMenu(player, target, menu, args, predicateDepth);
            return;
        }

        MenuOpenEvent openEvent = new MenuOpenEvent(player, menu);
        openEvent.callEvent();
        if (openEvent.isCancelled()) {
            return;
        }

        if (!passesOpenRequire(player, menu)) {
            return;
        }

        Map<String, String> resolvedArgs = resolveArguments(player, menu, args);
        if (resolvedArgs == null) {
            return;
        }

        String current = lastOpenedMenus.get(player.getUniqueId());
        if (current != null && !current.equals(menu.getName())) {
            previousMenus.put(player.getUniqueId(), current);
        }
        lastOpenedMenus.put(player.getUniqueId(), menu.getName());

        MenuHolder currentHolder = menuRegistry.getOpenMenu(player.getUniqueId()).orElse(null);
        if (canSmoothSwap(currentHolder, menu)) {
            smoothSwapMenu(player, target, currentHolder, menu, resolvedArgs);
            return;
        }

        MenuHolder holder = new MenuHolder(menu, player, target, resolvedArgs);
        menuRenderer.render(holder);
        menuRegistry.trackOpen(player.getUniqueId(), holder);

        player.getScheduler().run(this, task -> {
            player.openInventory(holder.getInventory());

            if (!menu.getOnOpen().isEmpty()) {
                actionRegistry.executeActions(player, menu.getOnOpen());
            }
        }, null);
    }

    private @NotNull List<String> suggestOnlinePlayerNames(@NotNull String partial) {
        String prefix = partial.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (Player online : Util.snapshotOnlinePlayers()) {
            String name = online.getName();
            if (name.toLowerCase().startsWith(prefix)) {
                matches.add(name);
            }
        }
        return matches;
    }

    private boolean isPredicateMenu(@NotNull Menu menu) {
        return menu.getPredicateType() != null
                && menu.getPredicatePass() != null
                && menu.getPredicateFail() != null;
    }

    private void handlePredicateMenu(@NotNull Player player, @Nullable OfflinePlayer target,
                                       @NotNull Menu menu, @NotNull Map<String, String> args,
                                       int predicateDepth) {
        if (predicateDepth >= MAX_PREDICATE_DEPTH) {
            getLogger().warning("Predicate menu chain exceeded max depth for menu '" + menu.getName() + "'");
            return;
        }

        Requirement predReq = Requirement.builder()
                .name("predicate")
                .type(menu.getPredicateType())
                .config(menu.getPredicateConfig() != null ? menu.getPredicateConfig() : Map.of())
                .denyActions(List.of())
                .successActions(List.of())
                .build();

        boolean passed = requirementRegistry.evaluate(player, predReq);
        String targetMenuName = passed ? menu.getPredicatePass() : menu.getPredicateFail();
        Menu targetMenu = menuRegistry.findByName(targetMenuName).orElse(null);
        if (targetMenu == null) {
            getLogger().warning("Predicate menu '" + menu.getName() + "' references unknown menu '" + targetMenuName + "'");
            return;
        }
        openMenu(player, target, targetMenu, args, predicateDepth + 1);
    }

    private boolean passesOpenRequire(@NotNull Player player, @NotNull Menu menu) {
        RequirementList openRequire = menu.getOpenRequire();
        if (openRequire == null) {
            return true;
        }
        if (player.hasPermission("aumenus.bypass.openrequirement")) {
            return true;
        }
        if (openRequire.evaluate(player, requirementRegistry)) {
            return true;
        }
        if (!openRequire.getDenyActions().isEmpty()) {
            actionRegistry.executeActions(player, openRequire.getDenyActions());
        }
        return false;
    }

    private boolean canSmoothSwap(@Nullable MenuHolder holder, @NotNull Menu newMenu) {
        if (!smoothTransitions) {
            return false;
        }
        if (holder == null) {
            return false;
        }
        if (!InventoryUpdater.isAvailable()) {
            return false;
        }
        if (newMenu.getInventoryType() != InventoryType.CHEST) {
            return false;
        }
        Menu oldMenu = holder.getMenu();
        if (oldMenu.getInventoryType() != InventoryType.CHEST) {
            return false;
        }
        return oldMenu.getSize() == newMenu.getSize();
    }

    private void smoothSwapMenu(@NotNull Player player, @Nullable OfflinePlayer target,
                                  @NotNull MenuHolder holder, @NotNull Menu newMenu,
                                  @NotNull Map<String, String> resolvedArgs) {
        player.getScheduler().run(this,
                task -> performSmoothSwap(player, target, holder, newMenu, resolvedArgs),
                null);
    }

    private void performSmoothSwap(@NotNull Player player, @Nullable OfflinePlayer target,
                                     @NotNull MenuHolder holder, @NotNull Menu newMenu,
                                     @NotNull Map<String, String> resolvedArgs) {
        if (!player.isOnline()) {
            return;
        }

        Menu oldMenu = holder.getMenu();
        boolean suppressOldClose = holder.isReloading();
        holder.setReloading(false);

        if (!suppressOldClose) {
            List<Action> onClose = oldMenu.getOnClose();
            if (!onClose.isEmpty()) {
                actionRegistry.executeActions(player, onClose);
            }
            new MenuCloseEvent(player, oldMenu).callEvent();
        }

        List<Integer> oldActiveSlots = new ArrayList<>(holder.getActiveItems().keySet());

        holder.resetForSwap();
        holder.setMenu(newMenu);
        holder.setTarget(target);
        holder.setArguments(resolvedArgs);

        Inventory inventory = holder.getInventory();
        menuRenderer.render(holder);

        for (int slot : oldActiveSlots) {
            if (holder.getActiveItems().containsKey(slot)) {
                continue;
            }
            inventory.setItem(slot, null);
        }

        String resolvedTitle = holder.resolveTitle(player);
        Component titleComponent = Util.parse(resolvedTitle);
        holder.setLastRenderedTitle(resolvedTitle);

        boolean sent = InventoryUpdater.sendSmoothUpdate(
                player, inventory, titleComponent, resolvedTitle);
        if (!sent) {
            holder.setReloading(true);
            player.openInventory(inventory);
            holder.setReloading(false);
        }

        if (!newMenu.getOnOpen().isEmpty()) {
            actionRegistry.executeActions(player, newMenu.getOnOpen());
        }
    }

    public void reloadMenus() {
        Map<UUID, MenuHolder> snapshot = new HashMap<>(menuRegistry.getOpenMenus());
        for (Map.Entry<UUID, MenuHolder> entry : snapshot.entrySet()) {
            entry.getValue().setReloading(true);
        }

        menuRegistry.clear();
        menuRenderer.clearItemCache();
        HeadProvider.clearCache();
        RequirementType.clearCache();
        reloadConfig();
        this.smoothTransitions = getConfig().getBoolean("smooth_transitions", true);
        int loaded = menuLoader.loadAll();
        getLogger().info("Reloaded " + loaded + " menu(s).");
        registerMenuCommands();

        for (Map.Entry<UUID, MenuHolder> entry : snapshot.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            reopenMenuAfterReload(player, entry.getValue());
        }
    }

    private void reopenMenuAfterReload(@NotNull Player player, @NotNull MenuHolder holder) {
        Menu newMenu = menuRegistry.findByName(holder.getMenu().getName()).orElse(null);

        player.getScheduler().run(this, task -> {
            if (newMenu == null) {
                player.closeInventory();
                return;
            }

            if (newMenu.getSize() != holder.getMenu().getSize()
                    || newMenu.getInventoryType() != holder.getMenu().getInventoryType()) {
                player.closeInventory();
            }
            MenuHolder newHolder = new MenuHolder(newMenu, player, holder.getTarget(), holder.getArguments());
            menuRenderer.render(newHolder);
            menuRegistry.trackOpen(player.getUniqueId(), newHolder);
            player.openInventory(newHolder.getInventory());
        }, null);
    }

    private Map<String, String> resolveArguments(@NotNull Player player,
                                                  @NotNull Menu menu,
                                                  @NotNull Map<String, String> providedArgs) {
        List<String> argNames = menu.getArgs();
        if (argNames.isEmpty()) {
            return new HashMap<>(providedArgs);
        }

        Map<String, String> resolved = new HashMap<>(providedArgs);

        for (String argName : argNames) {
            if (resolved.containsKey(argName)) {
                continue;
            }
            if (menu.getArgsUsage() != null) {
                player.sendMessage(Util.parse(menu.getArgsUsage()));
            }
            return null;
        }

        if (menu.getArgRequirements() == null) {
            return resolved;
        }

        for (Map.Entry<String, RequirementList> entry : menu.getArgRequirements().entrySet()) {
            if (entry.getValue().evaluate(player, requirementRegistry)) {
                continue;
            }
            executeArgDenyActions(player, entry.getValue().getDenyActions(), resolved);
            return null;
        }

        return resolved;
    }

    private void executeArgDenyActions(@NotNull Player player,
                                        @NotNull List<Action> denyActions,
                                        @NotNull Map<String, String> resolved) {
        if (denyActions.isEmpty()) {
            return;
        }

        List<Action> resolvedDeny = denyActions.stream()
                .map(action -> resolveActionArgs(action, resolved))
                .toList();
        actionRegistry.executeActions(player, resolvedDeny);
    }

    private @NotNull Action resolveActionArgs(@NotNull Action action,
                                               @NotNull Map<String, String> args) {
        String val = action.getValue();
        for (Map.Entry<String, String> arg : args.entrySet()) {
            val = val.replace("{" + arg.getKey() + "}", arg.getValue());
        }
        return new Action(action.getType(), val, action.getDelay(), action.getChance());
    }

    private void saveDefaultMenus() {
        File menusDir = new File(getDataFolder(), "menus");
        File[] existing = menusDir.listFiles();
        if (existing != null && existing.length > 0) {
            return;
        }
        menusDir.mkdirs();
        for (String name : DEFAULT_MENU_FILES) {
            saveResource("menus/" + name, false);
        }
    }

    private @Nullable OfflinePlayer resolveTarget(@NotNull String name, boolean allowOffline) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            return online;
        }
        if (allowOffline) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            if (offline.hasPlayedBefore()) {
                return offline;
            }
        }
        return null;
    }

    private void startGlobalTasks() {
        globalUpdateTask = Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, MenuHolder> entry : menuRegistry.getOpenMenus().entrySet()) {
                MenuHolder holder = entry.getValue();
                int updateInterval = holder.getMenu().getUpdateInterval();
                if (updateInterval <= 0) {
                    continue;
                }
                long intervalMs = Math.max(50L, updateInterval * 50L);
                if (now - holder.getLastUpdateTime() < intervalMs) {
                    continue;
                }
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    continue;
                }
                holder.setLastUpdateTime(now);
                player.getScheduler().run(this, t -> {
                    menuRenderer.refreshUpdatableItems(holder);
                    if (holder.getMenu().isDynamicTitle()) {
                        holder.updateTitle(player);
                    }
                }, null);
            }
        }, 50, 50, TimeUnit.MILLISECONDS);

        globalAnimationTask = Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            for (Map.Entry<UUID, MenuHolder> entry : menuRegistry.getOpenMenus().entrySet()) {
                MenuHolder holder = entry.getValue();
                if (!holder.hasAnimatedItems()) {
                    continue;
                }
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    continue;
                }
                player.getScheduler().run(this, t ->
                        menuRenderer.refreshAnimatedItems(holder), null);
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void registerMenuCommands() {
        CommandMap commandMap = Bukkit.getCommandMap();

        Map<String, Command> knownCommands = commandMap.getKnownCommands();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            if (entry.getValue().getDescription().startsWith("Opens the ")
                    && entry.getValue().getDescription().endsWith(" menu")) {
                toRemove.add(entry.getKey());
            }
        }
        for (String key : toRemove) {
            knownCommands.remove(key);
        }

        for (Menu menu : menuRegistry.all()) {
            if (menu.getCommand() == null || menu.getCommand().isEmpty() || !menu.isRegisterCommand()) {
                continue;
            }

            String cmd = menu.getCommand().toLowerCase();
            if (commandMap.getCommand(cmd) != null) {
                continue;
            }

            String menuName = menu.getName();
            List<String> aliases = menu.getCommandAliases().stream()
                    .map(String::toLowerCase).toList();

            Command dynamicCmd = new Command(cmd, "Opens the " + menuName + " menu", "/" + cmd, aliases) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Util.parse("<red>Only players can use this command."));
                        return true;
                    }
                    Menu targetMenu = menuRegistry.findByName(menuName).orElse(null);
                    if (targetMenu == null) {
                        return true;
                    }

                    OfflinePlayer target = null;
                    Map<String, String> menuArgs = new HashMap<>();
                    int argIndex = 0;

                    if (targetMenu.isAllowTargetPlayer() && targetMenu.isTargetPlayerArg() && args.length > 0) {
                        target = resolveTarget(args[0], targetMenu.isAllowOfflineTarget());
                        if (target == null) {
                            player.sendMessage(Util.playerNotFound(args[0]));
                            return true;
                        }
                    }

                    int startIndex = (target != null && targetMenu.isTargetPlayerArg()) ? 1 : 0;
                    for (int i = startIndex; i < args.length; i++) {
                        if (targetMenu.isAllowTargetPlayer() && args[i].toLowerCase().startsWith("-p:")) {
                            String targetName = args[i].substring(3);
                            target = resolveTarget(targetName, targetMenu.isAllowOfflineTarget());
                            if (target == null) {
                                player.sendMessage(Util.playerNotFound(targetName));
                                return true;
                            }
                            continue;
                        }
                        if (argIndex < targetMenu.getArgs().size()) {
                            String sanitized = MiniMessage.miniMessage().escapeTags(args[i]);
                            menuArgs.put(targetMenu.getArgs().get(argIndex), sanitized);
                            argIndex++;
                        }
                    }
                    openMenu(player, target, targetMenu, menuArgs);
                    return true;
                }

                @Override
                public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args) {
                    Menu targetMenu = menuRegistry.findByName(menuName).orElse(null);
                    if (targetMenu == null) {
                        return List.of();
                    }
                    if (!targetMenu.isAllowTargetPlayer() || !targetMenu.isTargetPlayerArg() || args.length != 1) {
                        return List.of();
                    }
                    return suggestOnlinePlayerNames(args[0]);
                }
            };
            commandMap.register("aumenus", dynamicCmd);
        }

        for (Player player : Util.snapshotOnlinePlayers()) {
            player.getScheduler().run(this, task -> player.updateCommands(), null);
        }
    }

}
