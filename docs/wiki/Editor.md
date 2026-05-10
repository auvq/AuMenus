# In-Game Editor

AuMenus includes a basic visual editor for arranging items in menus.

## Usage

Create a new menu and open the editor:

```
/am create <name> <size>
```

Edit an existing menu:

```
/am editor <menu>
```

## How It Works

1. The editor opens as a normal inventory showing current items.
2. Move items between slots by clicking and dragging, or place items from your inventory.
3. Closing the editor saves changes to the YAML file and reloads the menu.

Existing items (loaded from config) keep all their configuration, only slot positions update. New items placed from your inventory are captured with their full visible metadata (see below). Cleared slots are removed from config.

## What Gets Captured

When you drop a new item into the editor, these properties are read off the ItemStack and written to the YAML on close:

- **Material**, including resource-pack items (see below)
- **Amount**, if not 1
- **Name**: uses `custom_name` if set, falls back to `item_name` (1.21+ non-italic override). Serialized with MiniMessage so colors, gradients, fonts, decorations, click events and hover events survive the round-trip.
- **Lore**: each line serialized with MiniMessage (same as name)
- **Custom model data** as `model_data: N`
- **Item model** (1.21.2+ namespaced key) as `item_model: "namespace:key"`
- **Tooltip style** (1.21.2+ namespaced key) as `tooltip_style: "namespace:key"`
- **Enchantments** in `name;level` format
- **Item flags** as a list
- **Unbreakable**, **rarity**, **hide_tooltip**, **damage** if set
- **RGB color** for leather armor and potions

Not captured (AuMenus config only, not stored on the ItemStack): click actions, click requirements, view requirements, and slot priority. Add these in the YAML afterwards.

## Custom Items From Other Plugins

The editor detects and preserves items from supported plugins via their reverse-lookup APIs:

- **Nexo**: saved as `nexo-<id>`
- **ItemsAdder**: saved as `itemsadder-<namespaced:id>`
- **Oraxen**: saved as `oraxen-<id>`
- **HeadDatabase**: player heads from HDB are saved as `hdb-<id>` (takes priority over `basehead-<texture>` for HDB heads)

The plugin has to be loaded at save time for its detection to run.

## Player Heads

Heads that aren't HDB-managed preserve their texture via:

- Custom-textured heads (base64 profile texture from any source) saved as `basehead-<texture>`
- Owner-named vanilla heads saved as `head-<ownerName>`
- Plain untextured heads saved as `PLAYER_HEAD`

## Limitations

- Does not capture AuMenus-specific properties: click actions, click requirements, view requirements, slot priority. Configure those in the YAML.
- Does not support editing page items for paginated menus.
- One editor per menu at a time.
