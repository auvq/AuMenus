# FAQ

## My menu command shows up as red / "Unknown command"

When a menu registers a command (via the `command` option), the server's command map is updated dynamically. On some Paper versions, newly registered commands may appear red in tab completion until the server restarts. The commands still work when typed.

**Solution:** Restart the server after adding new menu commands. Existing commands work after `/am reload` without a restart -- only brand-new commands need the restart for clean tab completion.

## MiniMessage or legacy color codes?

AuMenus supports both:

- **Legacy codes:** `&a`, `&l`, `&#RRGGBB` (hex)
- **MiniMessage:** `<red>`, `<bold>`, `<gradient:#FF0000:#00FF00>`, `<rainbow>`, etc.

You can mix them in the same string. Legacy codes are internally converted to MiniMessage before rendering. MiniMessage is recommended for new menus as it supports gradients, hover events, and more.

Note: all item text has `<!italic>` prepended automatically to prevent the default italic styling that Minecraft applies to custom item names and lore.

## Does AuMenus support Folia?

Yes. AuMenus is fully compatible with Folia. It uses the correct scheduler APIs:

- Player-related operations (menus, clicks, rendering) use the player scheduler.
- Console commands and reload operations use the global region scheduler.
- Head texture fetching and placeholder resolution use the async scheduler.

No additional configuration is needed. The same jar works on both Paper and Folia.

## Is PlaceholderAPI required?

No. AuMenus includes built-in placeholders for common values (player name, health, level, world, coordinates, etc.). See [Placeholders](Placeholders.md).

PlaceholderAPI is optional and adds:
- Access to thousands of external placeholder expansions (Vault, LuckPerms, etc.)
- AuMenus-specific PAPI placeholders (`%aumenus_meta_...%`, `%aumenus_is_in_menu%`, etc.)

## Is Vault required?

No. Vault is optional and enables:
- Economy actions: `take_money`, `give_money`
- Economy requirements: `has_money`
- Permission actions: `give_perm`, `take_perm`

Without Vault, these features are silently disabled.

## Items show as red stained glass with "Config Error"

This means the item failed to load or build. The error message is shown in the lore, and details are logged to the console. Common causes:

- Invalid material name
- Slot out of range
- Invalid enchantment format (should be `name;level`)
- Invalid RGB format (should be `R,G,B`)

The rest of the menu still loads -- only the broken item is replaced with the error indicator.

## How do I make an item show only for certain players?

Use `view_require` with `priority` to create conditional items. Multiple items can share the same slot. The first item (lowest priority number) whose view requirement passes is displayed.

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

See [Requirements](Requirements.md) for the full `view_require` syntax.

## How do I update items with live data?

1. Set `update_interval` on the menu (in ticks, 20 = 1 second).
2. Set `update: true` on items that should refresh.
3. Use placeholders in the item's name or lore.

```yaml
update_interval: 20

items:
  stats:
    material: CLOCK
    slot: 13
    name: "&6Live Stats"
    lore:
      - "&7Level: &f%player_level%"
      - "&7Health: &c%player_health%"
    update: true
```

## How do I open a menu from another plugin?

Use the AuMenus API:

```java
AuMenusAPI.openMenu(player, "shop");
```

Or use the command: `/am open shop PlayerName`

See [API](API.md) for details.

## What inventory types are supported?

`CHEST`, `BARREL`, `ENDER_CHEST`, `SHULKER_BOX`, `HOPPER`, `DISPENSER`, `DROPPER`, `FURNACE`, `BLAST_FURNACE`, `SMOKER`, `BREWING`, `WORKBENCH`, `ANVIL`.

For chest-type inventories, set `size` to a multiple of 9. For other types, the size is determined by the inventory type.

## Can I use AuMenus menus on signs or NPCs?

AuMenus does not handle sign clicks or NPC interactions directly. Use another plugin (such as Citizens or interaction plugins) to run the command `/am open <menu>` when a player interacts with the sign or NPC.
