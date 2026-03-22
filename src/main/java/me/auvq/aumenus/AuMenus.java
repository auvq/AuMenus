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
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuMenus extends JavaPlugin {

    @Getter
    private static AuMenus instance;

    public static final int MINECRAFT_VERSION = parseMinecraftVersion();

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
        });

        new Metrics(this, 30368);

        getLogger().info("AuMenus v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            MenuHolder holder = menuRegistry.getOpenMenu(player.getUniqueId()).orElse(null);
            if (holder != null) {
                holder.stopUpdateTask();
            }
            player.getScheduler().run(this, task -> player.closeInventory(), null);
        }

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("AuMenus disabled.");
        instance = null;
    }

    public void openMenu(@NotNull Player player, @NotNull Menu menu, @NotNull Map<String, String> args) {
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

        lastOpenedMenus.put(player.getUniqueId(), menu.getName());

        MenuHolder holder = new MenuHolder(menu, player, resolvedArgs);
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
                openMenu(player, newMenu, holder.getArguments());
            } else {
                MenuHolder newHolder = new MenuHolder(newMenu, player, holder.getArguments());
                menuRenderer.render(newHolder);
                player.openInventory(newHolder.getInventory());
                newHolder.startUpdateTask(this);
            }
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
            if (!resolved.containsKey(argName)) {
                if (menu.getArgsUsage() != null) {
                    player.sendMessage(Util.parse(menu.getArgsUsage()));
                }
                return null;
            }
        }

        if (menu.getArgRequirements() != null) {
            for (Map.Entry<String, RequirementList> entry
                    : menu.getArgRequirements().entrySet()) {
                if (!entry.getValue().evaluate(player, requirementRegistry)) {
                    List<Action> denyActions = entry.getValue().getDenyActions();
                    if (!denyActions.isEmpty()) {
                        List<Action> resolvedDeny = denyActions.stream()
                                .map(action -> {
                                    String val = action.getValue();
                                    for (Map.Entry<String, String> arg : resolved.entrySet()) {
                                        val = val.replace("{" + arg.getKey() + "}", arg.getValue());
                                    }
                                    return new Action(action.getType(), val, action.getDelay(), action.getChance());
                                })
                                .toList();
                        actionRegistry.executeActions(player, resolvedDeny);
                    }
                    return null;
                }
            }
        }

        return resolved;
    }

    private void saveDefaultMenus() {
        File menusDir = new File(getDataFolder(), "menus");
        if (menusDir.exists() && menusDir.listFiles() != null && menusDir.listFiles().length > 0) {
            return;
        }
        menusDir.mkdirs();
        String[] defaults = {"basic_menu.yml", "paginated_menu.yml", "test_actions.yml",
                "test_requirements.yml", "test_items.yml", "test_inventory_types.yml", "test_dispenser.yml"};
        for (String name : defaults) {
            saveResource("menus/" + name, false);
        }
    }

    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        return MINECRAFT_VERSION >= (major * 10000 + minor * 100 + patch);
    }

    public void registerMenuCommands() {
        CommandMap commandMap = Bukkit.getCommandMap();
        for (Menu menu : menuRegistry.all()) {
            if (menu.getCommand() == null || menu.getCommand().isEmpty() || !menu.isRegisterCommand()) {
                continue;
            }
            String cmd = menu.getCommand().toLowerCase();
            if (commandMap.getCommand(cmd) != null) {
                continue;
            }
            commandMap.register("aumenus", new Command(cmd) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Util.parse("<red>Only players can use this command.</red>"));
                        return true;
                    }
                    Map<String, String> menuArgs = new HashMap<>();
                    for (int i = 0; i < Math.min(menu.getArgs().size(), args.length); i++) {
                        menuArgs.put(menu.getArgs().get(i), args[i]);
                    }
                    openMenu(player, menu, menuArgs);
                    return true;
                }
            });
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(this, task -> player.updateCommands(), null);
        }
    }

    private static int parseMinecraftVersion() {
        String version = Bukkit.getMinecraftVersion();
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
            return major * 10000 + minor * 100 + patch;
        }
        return 12006;
    }
}
