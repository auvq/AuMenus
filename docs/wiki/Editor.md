# In-Game Editor

AuMenus includes a basic in-game editor for visually arranging items in a menu.

## Creating a New Menu

```
/am create <name> <size>
```

This creates an empty YAML file in `plugins/AuMenus/menus/` and opens the editor for the new menu. Size must be a multiple of 9 (9, 18, 27, 36, 45, or 54).

## Editing an Existing Menu

```
/am editor <menu>
```

Opens the editor for an existing menu. The editor loads the current items as rendered in-game.

## How It Works

1. The editor opens as a normal inventory with the menu's current items displayed.
2. You can move items between slots by clicking and dragging, or place new items from your inventory.
3. When you close the editor inventory, changes are saved to the menu's YAML file automatically.
4. The menu is reloaded after saving.

### What gets saved

- **Existing items** (items loaded from the config) retain all their configuration (actions, requirements, lore, etc.). Only their slot positions are updated.
- **New items** (placed from your inventory) are saved with their basic properties: material, display name, lore, amount, and slot.
- **Removed items** (slots that were cleared) are removed from the config.

### Item identification

The editor tags each loaded item with an internal PDC marker linking it to its config key name. When you move a tagged item to a different slot, the editor updates only the `slot`/`slots` field in the config -- all other properties (actions, requirements, etc.) are preserved.

## Limitations

- The editor only handles slot positioning. To configure actions, requirements, lore formatting, enchantments, and other properties, edit the YAML file directly and use `/am reload`.
- New items placed from your inventory only capture basic properties (material, plain text name, plain text lore, amount).
- The editor does not support editing page items for paginated menus.
- Non-chest inventory types (HOPPER, DISPENSER, etc.) are supported but the editor uses the appropriate inventory type.
- Only one player can edit a menu at a time.

## Workflow

A typical workflow for creating a new menu:

1. `/am create shop 54` -- Creates the file and opens the editor.
2. Place items from your inventory into the desired slots.
3. Close the editor -- items are saved to the YAML file.
4. Edit `plugins/AuMenus/menus/shop.yml` to add actions, requirements, custom names, and lore.
5. `/am reload` to apply changes.
