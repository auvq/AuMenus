package me.auvq.aumenus.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.auvq.aumenus.AuMenus;
import me.auvq.aumenus.action.Action;
import me.auvq.aumenus.menu.Menu;
import me.auvq.aumenus.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class MenuCommand {

    private MenuCommand() {}

    private static final Set<String> META_OPERATIONS = Set.of("set", "remove", "add", "subtract", "switch", "get", "list");
    private static final Set<String> META_TYPES = Set.of("STRING", "INTEGER", "LONG", "DOUBLE", "BOOLEAN");

    public static void register(@NotNull Commands registrar, @NotNull AuMenus plugin) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("aumenus")
                .then(openSubcommand(plugin))
                .then(listSubcommand(plugin))
                .then(reloadSubcommand(plugin))
                .then(executeSubcommand(plugin))
                .then(createSubcommand(plugin))
                .then(metaSubcommand(plugin))
                .then(editorSubcommand(plugin))
                .then(migrateSubcommand(plugin));

        registrar.register(root.build(), "AuMenus main command", List.of("am"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> openSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("open")
                .requires(source -> source.getSender().hasPermission("aumenus.open"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("menu", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            plugin.getMenuRegistry().all().forEach(menu -> builder.suggest(menu.getName()));
                            return builder.buildFuture();
                        })
                        .executes(context -> handleOpen(context, plugin, null))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
                                .requires(source -> source.getSender().hasPermission("aumenus.open.others"))
                                .suggests((context, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    return handleOpen(context, plugin, playerName);
                                })
                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("args", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String playerName = StringArgumentType.getString(context, "player");
                                            return handleOpen(context, plugin, playerName);
                                        })
                                )));
    }

    private static int handleOpen(@NotNull CommandContext<CommandSourceStack> context,
                                   @NotNull AuMenus plugin,
                                   String targetName) {
        CommandSender sender = context.getSource().getSender();
        String menuName = StringArgumentType.getString(context, "menu");

        Menu menu = plugin.getMenuRegistry().findByName(menuName).orElse(null);
        if (menu == null) {
            sender.sendMessage(Util.parse("&cMenu '" + menuName + "' not found."));
            return 0;
        }

        Player target;
        if (targetName != null) {
            target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(Util.parse("&cPlayer '" + targetName + "' not found."));
                return 0;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Util.parse("&cSpecify a player when running from console."));
            return 0;
        }

        Map<String, String> args = new HashMap<>();
        try {
            String argsStr = StringArgumentType.getString(context, "args");
            String[] argValues = argsStr.split("\\s+");
            for (int i = 0; i < Math.min(menu.getArgs().size(), argValues.length); i++) {
                String sanitized = MiniMessage.miniMessage().escapeTags(argValues[i]);
                args.put(menu.getArgs().get(i), sanitized);
            }
        } catch (IllegalArgumentException ignored) {
        }

        plugin.openMenu(target, menu, args);
        if (sender != target) {
            sender.sendMessage(Util.parse("&aOpened menu '" + menuName + "' for " + target.getName() + "."));
        }
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("list")
                .requires(source -> source.getSender().hasPermission("aumenus.list"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    if (plugin.getMenuRegistry().size() == 0) {
                        sender.sendMessage(Util.parse("&7No menus loaded."));
                        return 1;
                    }
                    sender.sendMessage(Util.parse("&6AuMenus &7- &fLoaded menus:"));
                    for (Menu menu : plugin.getMenuRegistry().all()) {
                        String cmd = menu.getCommand() != null ? " &7(/" + menu.getCommand() + ")" : "";
                        String errors = menu.hasErrors() ? " &c[" + menu.getLoadErrors().size() + " errors]" : "";
                        sender.sendMessage(Util.parse("&7 - &e" + menu.getName() + cmd + errors));
                    }
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reloadSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("aumenus.reload"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
                        plugin.reloadMenus();
                        sender.sendMessage(Util.parse("&aAuMenus reloaded. "
                                + plugin.getMenuRegistry().size() + " menu(s) loaded."));
                    });
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> executeSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("execute")
                .requires(source -> source.getSender().hasPermission("aumenus.execute"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("action", StringArgumentType.greedyString())
                                .executes(context -> handleExecute(context, plugin))));
    }

    private static int handleExecute(@NotNull CommandContext<CommandSourceStack> context,
                                     @NotNull AuMenus plugin) {
        CommandSender sender = context.getSource().getSender();
        String playerName = StringArgumentType.getString(context, "player");
        String actionStr = StringArgumentType.getString(context, "action");

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Util.parse("&cPlayer '" + playerName + "' not found."));
            return 0;
        }

        String actionType = actionStr.split("\\s+")[0];
        String actionValue = actionStr.contains(" ") ? actionStr.substring(actionStr.indexOf(' ') + 1) : "";
        Action action = new Action(actionType, actionValue);
        target.getScheduler().run(plugin, task -> {
            plugin.getActionRegistry().executeSingle(target, action);
        }, null);
        sender.sendMessage(Util.parse("&aExecuted action for " + target.getName() + "."));
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("create")
                .requires(source -> source.getSender().hasPermission("aumenus.admin"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("name", StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("size", IntegerArgumentType.integer(9, 54))
                                .executes(context -> handleCreate(context, plugin))));
    }

    private static int handleCreate(@NotNull CommandContext<CommandSourceStack> context,
                                    @NotNull AuMenus plugin) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "name");
        int size = IntegerArgumentType.getInteger(context, "size");

        if (size % 9 != 0) {
            sender.sendMessage(Util.parse("&cSize must be a multiple of 9 (e.g. 9, 18, 27, 36, 45, 54)."));
            return 0;
        }

        if (plugin.getMenuRegistry().findByName(name).isPresent()) {
            sender.sendMessage(Util.parse("&cMenu '" + name + "' already exists."));
            return 0;
        }

        File menuFile = new File(plugin.getDataFolder(), "menus/" + name + ".yml");
        if (menuFile.exists()) {
            sender.sendMessage(Util.parse("&cFile '" + name + ".yml' already exists."));
            return 0;
        }

        menuFile.getParentFile().mkdirs();
        YamlConfiguration config = new YamlConfiguration();
        config.set("title", "&8" + name);
        config.set("size", size);

        try {
            config.save(menuFile);
        } catch (IOException e) {
            sender.sendMessage(Util.parse("&cFailed to create menu file: " + e.getMessage()));
            return 0;
        }

        Menu menu = plugin.getMenuLoader().loadMenu(menuFile);
        if (menu == null) {
            sender.sendMessage(Util.parse("&cFailed to load menu file after creation."));
            return 0;
        }
        plugin.getMenuRegistry().register(menu);

        sender.sendMessage(Util.parse("&aCreated menu '" + name + "' (" + size + " slots)."));

        if (sender instanceof Player player) {
            plugin.getMenuEditor().openEditor(player, name, size);
        }

        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> metaSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("meta")
                .requires(source -> source.getSender().hasPermission("aumenus.admin"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("operation", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    META_OPERATIONS.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> handleMetaList(context, plugin))
                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("key", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String playerName = StringArgumentType.getString(context, "player");
                                            Player target = Bukkit.getPlayer(playerName);
                                            if (target != null) {
                                                plugin.getMetaStore().getKeys(target).forEach(builder::suggest);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("type", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    META_TYPES.forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> handleMeta(context, plugin, null))
                                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("value", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            String value = StringArgumentType.getString(context, "value");
                                                            return handleMeta(context, plugin, value);
                                                        }))))));
    }

    private static int handleMetaList(@NotNull CommandContext<CommandSourceStack> context,
                                       @NotNull AuMenus plugin) {
        CommandSender sender = context.getSource().getSender();
        String playerName = StringArgumentType.getString(context, "player");
        String operation = StringArgumentType.getString(context, "operation");

        if (!operation.equalsIgnoreCase("list")) {
            sender.sendMessage(Util.parse("&cUsage: /aumenus meta <player> <operation> <key> <type> [value]"));
            return 0;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Util.parse("&cPlayer '" + playerName + "' not found."));
            return 0;
        }

        sender.sendMessage(Util.parse("&6Meta keys for &e" + target.getName() + "&6:"));
        target.getPersistentDataContainer().getKeys().stream()
                .filter(key -> key.getNamespace().equals(plugin.getName().toLowerCase()))
                .forEach(key -> sender.sendMessage(Util.parse("&7 - &e" + key.getKey())));
        return 1;
    }

    private static int handleMeta(@NotNull CommandContext<CommandSourceStack> context,
                                   @NotNull AuMenus plugin,
                                   String value) {
        CommandSender sender = context.getSource().getSender();
        String playerName = StringArgumentType.getString(context, "player");
        String operation = StringArgumentType.getString(context, "operation").toLowerCase();
        String key = StringArgumentType.getString(context, "key");
        String type = StringArgumentType.getString(context, "type").toUpperCase();

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Util.parse("&cPlayer '" + playerName + "' not found."));
            return 0;
        }

        if (!META_TYPES.contains(type)) {
            sender.sendMessage(Util.parse("&cUnknown type '" + type + "'. Valid: " + String.join(", ", META_TYPES)));
            return 0;
        }

        switch (operation) {
            case "remove" -> {
                plugin.getMetaStore().remove(target, key);
                sender.sendMessage(Util.parse("&aRemoved meta key '" + key + "' from " + target.getName() + "."));
            }
            case "switch" -> {
                plugin.getMetaStore().executeMetaAction(target, "switch " + key);
                sender.sendMessage(Util.parse("&aSwitched boolean meta '" + key + "' for " + target.getName() + "."));
            }
            case "get" -> {
                String result = plugin.getMetaStore().get(target, key, type, "&cnot set");
                sender.sendMessage(Util.parse("&6" + target.getName() + " &7[" + key + "] &f= &e" + result));
            }
            case "set", "add", "subtract" -> {
                if (value == null) {
                    sender.sendMessage(Util.parse("&cUsage: /aumenus meta <player> " + operation + " <key> <type> <value>"));
                    return 0;
                }
                plugin.getMetaStore().executeMetaAction(target, operation + " " + key + " " + type + " " + value);
                sender.sendMessage(Util.parse("&aExecuted meta " + operation + " on '" + key + "' for " + target.getName() + "."));
            }
            default -> {
                sender.sendMessage(Util.parse("&cUnknown operation '" + operation + "'. Valid: set, remove, add, subtract, switch, get, list"));
                return 0;
            }
        }

        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> editorSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("editor")
                .requires(source -> source.getSender().hasPermission("aumenus.admin"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("menu", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            plugin.getMenuRegistry().all().forEach(menu -> builder.suggest(menu.getName()));
                            return builder.buildFuture();
                        })
                        .executes(context -> handleEditor(context, plugin)));
    }

    private static int handleEditor(@NotNull CommandContext<CommandSourceStack> context,
                                     @NotNull AuMenus plugin) {
        CommandSender sender = context.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Util.parse("&cThis command can only be run by a player."));
            return 0;
        }

        String menuName = StringArgumentType.getString(context, "menu");
        Menu menu = plugin.getMenuRegistry().findByName(menuName).orElse(null);

        if (menu == null) {
            sender.sendMessage(Util.parse("&cMenu '" + menuName + "' not found."));
            return 0;
        }

        plugin.getMenuEditor().openEditor(player, menuName, menu.getSize());
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> migrateSubcommand(@NotNull AuMenus plugin) {
        return Commands.literal("migrate")
                .requires(source -> source.getSender().hasPermission("aumenus.admin"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    return handleMigrateAll(sender, plugin);
                })
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("menu", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            plugin.getMenuMigrator().listAvailableFiles().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            String name = StringArgumentType.getString(context, "menu");
                            return handleMigrateSingle(sender, plugin, name);
                        }));
    }

    private static int handleMigrateAll(@NotNull CommandSender sender, @NotNull AuMenus plugin) {
        File source = plugin.getMenuMigrator().findSourceFolder();
        if (source == null) {
            sender.sendMessage(Util.parse("&#FF474DNo DeluxeMenus files found. Place DM menu files in:"));
            sender.sendMessage(Util.parse("&#B6D0E2  plugins/DeluxeMenus/gui_menus/"));
            sender.sendMessage(Util.parse("&#B6D0E2  plugins/AuMenus/migration/"));
            return 0;
        }

        Player notifier = sender instanceof Player p ? p : null;
        sender.sendMessage(Util.parse("&#B6D0E2Migrating from: &f" + source.getPath()));
        int count = plugin.getMenuMigrator().migrateAll(notifier);
        sender.sendMessage(Util.parse("&#00FF7FMigrated &f" + count + " &#00FF7Fmenu(s). Run &f/am reload &#00FF7Fto load them."));
        return 1;
    }

    private static int handleMigrateSingle(@NotNull CommandSender sender, @NotNull AuMenus plugin,
                                            @NotNull String name) {
        File source = plugin.getMenuMigrator().findSourceFolder();
        if (source == null) {
            sender.sendMessage(Util.parse("&#FF474DNo DeluxeMenus files found."));
            return 0;
        }

        Player notifier = sender instanceof Player p ? p : null;
        if (plugin.getMenuMigrator().migrateSingle(name, notifier)) {
            sender.sendMessage(Util.parse("&#00FF7FRun &f/am reload &#00FF7Fto load the migrated menu."));
        } else {
            sender.sendMessage(Util.parse("&#FF474DCould not migrate '" + name + "'."));
        }
        return 1;
    }
}
