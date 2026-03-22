# AuMenus

AuMenus is a lightweight, open-source GUI menu plugin for Paper and Folia 1.20.6+. Config-driven. MiniMessage and legacy color support. Folia-native.

## Features

- Config-driven menus with YAML files
- MiniMessage and legacy color code support
- Full Folia compatibility out of the box
- Pagination system for large item lists
- Conditional items with view requirements
- Per-click-type actions (left, right, shift+left, shift+right, middle)
- Anvil and chat input systems
- Persistent player metadata (PDC-backed)
- Shorthand and full-form requirement syntax
- Built-in placeholders (no PAPI required for basics)
- PlaceholderAPI and Vault integration
- DeluxeMenus migration tool
- In-game menu editor
- Developer API with custom action and requirement registration
- BungeeCord server switching

## Requirements

- Paper or Folia 1.20.6+
- Java 21+
- Optional: Vault (for economy/permissions), PlaceholderAPI (for external placeholders)

## Installation

1. Download the AuMenus jar file.
2. Place it in your server's `plugins/` folder.
3. Start or restart your server.
4. Default example menus are generated in `plugins/AuMenus/menus/`.

## Quick Start

After installation, try these commands:

- `/shop` -- Opens the example shop menu
- `/warps` -- Opens the example paginated warps menu
- `/testact` -- Opens the action test menu
- `/testreq` -- Opens the requirement test menu
- `/testitems` -- Opens the item properties test menu

All menus are defined as YAML files in `plugins/AuMenus/menus/`. Edit any file and run `/am reload` to apply changes.

## Wiki Pages

- [Menu Options](Menu-Options.md) -- Menu-level configuration (title, size, commands, args, events)
- [Item Properties](Item-Properties.md) -- All item configuration options (material, lore, enchantments, etc.)
- [Actions](Actions.md) -- All action types (commands, messages, economy, sounds, etc.)
- [Requirements](Requirements.md) -- Requirement system (permissions, money, items, comparators, etc.)
- [Pagination](Pagination.md) -- Paginated menus with page_slots and page_items
- [Input System](Input-System.md) -- Anvil input and chat input
- [Commands](Commands.md) -- All plugin commands and permissions
- [Meta System](Meta-System.md) -- Persistent player data storage and retrieval
- [Placeholders](Placeholders.md) -- Built-in and PAPI placeholders
- [Editor](Editor.md) -- In-game menu editor
- [Migration](Migration.md) -- Migrating from DeluxeMenus
- [API](API.md) -- Developer API for other plugins
- [FAQ](FAQ.md) -- Frequently asked questions
