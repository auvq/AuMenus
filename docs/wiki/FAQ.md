# FAQ

## My menu command shows up as red / "Unknown command"

Dynamically registered commands may appear red in tab completion until the server restarts. The commands still work when typed. Restart the server after adding new menu commands for clean tab completion.

## MiniMessage or legacy color codes?

Both are supported and can be mixed:

- Legacy: `&a`, `&l`, `&#RRGGBB`
- MiniMessage: `<red>`, `<bold>`, `<gradient:#FF0000:#00FF00>`, `<rainbow>`

Legacy codes are converted to MiniMessage internally. MiniMessage is recommended for new menus. All item text has `<!italic>` prepended automatically to prevent Minecraft's default italic on custom names and lore.

## Does AuMenus support Folia?

Yes. The same jar works on both Paper and Folia. Player operations use the player scheduler, console commands use the global scheduler, and async tasks (head fetching, placeholder resolution) use the async scheduler.

## Is PlaceholderAPI required?

No. Built-in placeholders cover common values (name, health, level, world, etc.). PAPI is optional and adds external expansion support plus AuMenus-specific placeholders. See [Placeholders](Placeholders.md).

## Is Vault required?

No. Vault is optional. Without it, economy actions (`take_money`, `give_money`), economy requirements (`has_money`), and permission actions (`give_perm`, `take_perm`) are disabled silently.

## Items show as red stained glass with "Config Error"

The item failed to load. Error details are in the lore and console log. Common causes: invalid material name, slot out of range, bad enchantment format (`name;level`), bad RGB format (`R,G,B`). The rest of the menu still loads.

## How do I make an item show only for certain players?

Use `view_require` with `priority`. See [Requirements](Requirements.md).

```yaml
vip_item:
  material: NETHER_STAR
  slot: 13
  priority: 0
  view_require:
    perm: group.vip

default_item:
  material: PAPER
  slot: 13
  priority: 1
```

## How do I update items with live data?

Set `update_interval` on the menu, `update: true` on the item, and use placeholders in name/lore.

## How do I open a menu from another plugin?

```java
AuMenusAPI.openMenu(player, "shop");
```

Or: `/am open shop PlayerName`. See [API](API.md).

## What inventory types are supported?

`CHEST`, `BARREL`, `ENDER_CHEST`, `SHULKER_BOX`, `HOPPER`, `DISPENSER`, `DROPPER`, `FURNACE`, `BLAST_FURNACE`, `SMOKER`, `BREWING`, `WORKBENCH`, `ANVIL`.

## Can I use AuMenus menus on signs or NPCs?

AuMenus does not handle sign/NPC interactions. Use another plugin to run `/am open <menu>` on interaction.
