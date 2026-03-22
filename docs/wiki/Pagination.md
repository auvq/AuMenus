# Pagination

AuMenus supports paginated menus for displaying large item lists across multiple pages.

## Configuration

### page_slots

Inventory slots used for paginated items. Same range/list format as item `slots`.

```yaml
page_slots: [10-16, 19-25, 28-34]
```

### page_items

Items that fill the page slots. Defined like regular items but without `slot` or `slots`. Position is determined by `page_slots` and the current page.

### Placeholders

| Placeholder  | Description         |
|--------------|---------------------|
| `{page}`     | Current page number |
| `{max_page}` | Total page count    |

If the title contains `{page}` or `{max_page}`, changing pages re-creates the inventory to update the title.

### Navigation

Use `prev_page` and `next_page` actions on regular items (not page items).

`prev_page` does nothing on page 1. `next_page` does nothing on the last page.

## Complete Example

```yaml
title: "<dark_gray>Warps {page}/{max_page}</dark_gray>"
command: warps
size: 54

page_slots: [10-16, 19-25, 28-34]

page_items:
  spawn:
    material: GRASS_BLOCK
    name: "&a&lSpawn"
    lore:
      - "&7Teleport to spawn."
    on_click:
      - player: "warp spawn"
      - close

  nether:
    material: NETHERRACK
    name: "&c&lNether"
    on_click:
      - player: "warp nether"
      - close

  end:
    material: END_STONE
    name: "&5&lThe End"
    on_click:
      - player: "warp end"
      - close

items:
  border:
    material: GRAY_STAINED_GLASS_PANE
    name: " "
    hide_tooltip: true
    slots: [0-9, 17-18, 26-27, 35-44, 45-47, 51-53]

  previous_page:
    material: ARROW
    slot: 48
    name: "&e&l<- Previous Page"
    lore:
      - "&7Page {page}/{max_page}"
    on_click:
      - prev_page

  next_page:
    material: ARROW
    slot: 50
    name: "&e&lNext Page ->"
    lore:
      - "&7Page {page}/{max_page}"
    on_click:
      - next_page

  close:
    material: FLOWER_BANNER_PATTERN
    slot: 49
    name: "&eExit"
    on_click:
      - close
```

Regular `items` (border, navigation, close button) stay fixed on every page. Only the `page_slots` area changes.
