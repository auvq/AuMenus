# Migration

AuMenus includes a migration tool that converts menu files from other plugins into AuMenus format.

## Commands

```
/am migrate           # Migrate all files
/am migrate <name>    # Migrate a single file
```

Permission: `aumenus.admin`

## Source Folders

The migrator checks these locations in order:

1. `plugins/AuMenus/migration/`
2. Other common menu plugin folders on the server

Copy your source menu files into `plugins/AuMenus/migration/` if the migrator does not detect them automatically.

## What Gets Converted

Menu-level keys are mapped to AuMenus equivalents: title, command, size, inventory type, update interval, args, open requirements, and open/close actions.

Item properties are mapped: material, display name, lore, slots, enchantments, model data, and all click/view requirements and actions.

Action prefixes (e.g., `[player]`, `[console]`, `[message]`) are converted to AuMenus format (`player:`, `console:`, `msg:`). Delay and chance modifiers are converted to `delay` and `chance` keys.

Requirement structures (with `deny_commands` and `success_commands`) are converted to AuMenus `checks` format with `deny` and `success` lists.

## Output

Converted files are saved to `plugins/AuMenus/menus/`. Existing files with the same name are skipped. Run `/am reload` after migration.

## After Migration

- **Color codes:** Legacy codes (`&a`, `&#RRGGBB`) carry over and work. Consider updating to MiniMessage.
- **Actions:** Verify conversions, especially actions with delays and chances.
- **Requirements:** Review complex requirement structures.
- **Placeholders:** PAPI placeholders carry over unchanged.
