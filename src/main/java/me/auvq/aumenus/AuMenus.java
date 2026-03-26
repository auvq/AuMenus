package me.auvq.aumenus;

import lombok.Getter;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.action.ActionRegistry;
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
import me.auvq.aumenus.meta.MetaStore;
import me.auvq.aumenus.requirement.RequirementList;
import me.auvq.aumenus.requirement.RequirementRegistry;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

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
        for (Player player : Bukkit.getOnlinePlayers()) {
            menuRegistry.getOpenMenu(player.getUniqueId()).ifPresent(MenuHolder::stopUpdateTask);
            player.closeInventory();
        }

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("AuMenus disabled.");
    }

    public void openMenu(@NotNull Player player, @NotNull Menu menu, @NotNull Map<String, String> args) {
        openMenu(player, null, menu, args);
    }

    public void openMenu(@NotNull Player player, @Nullable OfflinePlayer target, @NotNull Menu menu,
                          @NotNull Map<String, String> args) {
        if (!player.isOnline()) {
            return;
        }

        MenuOpenEvent openEvent = new MenuOpenEvent(player, menu);
        openEvent.callEvent();
        if (openEvent.isCancelled()) {
            return;
        }

        if (menu.getOpenRequire() != null
                && !player.hasPermission("aumenus.bypass.openrequirement")
                && !menu.getOpenRequire().evaluate(player, requirementRegistry)) {
            if (!menu.getOpenRequire().getDenyActions().isEmpty()) {
                actionRegistry.executeActions(player, menu.getOpenRequire().getDenyActions());
            }
            return;
        }

        Map<String, String> resolvedArgs = resolveArguments(player, menu, args);
        if (resolvedArgs == null) {
            return;
        }

        String current = lastOpenedMenus.get(player.getUniqueId());
        String prev = previousMenus.get(player.getUniqueId());
        if (current != null && !menu.getName().equals(prev)) {
            previousMenus.put(player.getUniqueId(), current);
        }
        lastOpenedMenus.put(player.getUniqueId(), menu.getName());

        MenuHolder holder = new MenuHolder(menu, player, target, resolvedArgs);
        menuRenderer.render(holder);
        menuRegistry.trackOpen(player.getUniqueId(), holder);

        player.getScheduler().run(this, task -> {
            player.openInventory(holder.getInventory());
            holder.startUpdateTask(this);

            if (!menu.getOnOpen().isEmpty()) {
                actionRegistry.executeActions(player, menu.getOnOpen());
            }
        }, null);
    }

    public void reloadMenus() {
        Map<UUID, MenuHolder> snapshot = new HashMap<>(menuRegistry.getOpenMenus());
        for (Map.Entry<UUID, MenuHolder> entry : snapshot.entrySet()) {
            entry.getValue().stopUpdateTask();
            entry.getValue().setReloading(true);
        }

        menuRegistry.clear();
        HeadProvider.clearCache();
        reloadConfig();
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
            MenuHolder newHolder = new MenuHolder(newMenu, player, holder.getArguments());
            menuRenderer.render(newHolder);
            menuRegistry.trackOpen(player.getUniqueId(), newHolder);
            player.openInventory(newHolder.getInventory());
            newHolder.startUpdateTask(this);
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
        String[] defaults = {"basic_menu.yml", "paginated_menu.yml", "test_actions.yml",
                "test_requirements.yml", "test_items.yml", "test_inventory_types.yml", "test_dispenser.yml"};
        for (String name : defaults) {
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
                        sender.sendMessage(Util.parse("&cOnly players can use this command."));
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
                    if (targetMenu.isAllowTargetPlayer() && targetMenu.isTargetPlayerArg() && args.length == 1) {
                        String prefix = args[0].toLowerCase();
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(prefix))
                                .toList();
                    }
                    return List.of();
                }
            };
            commandMap.register("aumenus", dynamicCmd);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(this, task -> player.updateCommands(), null);
        }
    }

}
