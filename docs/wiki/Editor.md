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

Existing items (loaded from config) keep all their configuration -only slot positions update. New items placed from your inventory are saved with basic properties only (material, name, lore, amount, slot). Cleared slots are removed from config.

## Limitations

- Only handles slot positioning. Configure actions, requirements, and other properties in the YAML file.
- New items from inventory capture only basic properties (material, plain text name/lore, amount).
- Does not support editing page items for paginated menus.
- One editor per menu at a time.
