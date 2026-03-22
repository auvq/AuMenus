# AuMenus

AuMenus is a lightweight, open-source GUI menu plugin for Paper and Folia 1.20.6+. Config-driven. MiniMessage and legacy color support. Folia-native.

## Features

- YAML-based menu configuration
- MiniMessage and legacy color codes (`&a`, `&#RRGGBB`)
- Folia compatible out of the box
- Pagination for large item lists
- Conditional items with view requirements
- Per-click-type actions (left, right, shift+left, shift+right, middle)
- Anvil and chat input collection
- Persistent player metadata (PDC-backed)
- Shorthand and full-form requirement syntax
- Built-in placeholders (no PAPI required for basics)
- PlaceholderAPI and Vault integration
- In-game menu editor
- Migration tool for existing menu files
- Developer API with custom action/requirement registration
- BungeeCord/Velocity server switching

## Requirements

- Paper or Folia 1.20.6+
- Java 21+
- Optional: Vault (economy/permissions), PlaceholderAPI (external placeholders)

## Installation

Drop the AuMenus jar into your server's `plugins/` folder and restart. Example menus are generated in `plugins/AuMenus/menus/`.

## Quick Start

After installation:

- `/shop` -- Opens the example shop menu
- `/warps` -- Opens the example paginated warps menu
- `/testact` -- Opens the action test menu
- `/testreq` -- Opens the requirement test menu
- `/testitems` -- Opens the item properties test menu

All menus are YAML files in `plugins/AuMenus/menus/`. Edit any file and run `/am reload` to apply changes.

## Wiki Pages

- [Configuration](Configuration.md) -- Menu options and item properties
- [Actions](Actions.md) -- All action types
- [Requirements](Requirements.md) -- Requirement system
- [Pagination](Pagination.md) -- Paginated menus
- [Commands](Commands.md) -- Commands and permissions
- [Placeholders](Placeholders.md) -- Placeholders and meta system
- [Editor](Editor.md) -- In-game menu editor
- [Migration](Migration.md) -- Migrating from other menu plugins
- [API](API.md) -- Developer API
- [FAQ](FAQ.md) -- Frequently asked questions
