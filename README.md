<div align="center">

<!-- Logo will be added here -->

# AuMenus
**Lightweight, open-source GUI menu plugin for Paper & Folia 1.21.1+**

[![Build](https://img.shields.io/github/actions/workflow/status/auvq/AuMenus/build.yml?branch=main&label=build)](https://github.com/auvq/AuMenus/actions)
[![Servers](https://img.shields.io/bstats/servers/30368?label=servers&color=brightgreen)](https://bstats.org/plugin/bukkit/AuMenus/30368)
[![Discord](https://img.shields.io/discord/1485295113566421033?label=chat&logo=discord&color=5865F2)](https://discord.gg/B7ArGXP6)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Wiki](https://img.shields.io/badge/docs-Wiki-blue?logo=github)](https://github.com/auvq/AuMenus/wiki)

</div>

---

## Information

**AuMenus** is a lightweight, open-source alternative to DeluxeMenus for Paper and Folia servers. Create fully configurable GUI menus that open with custom commands, display player-specific content, and perform actions based on requirements.

- Full **MiniMessage** support (gradients, hover, click events) alongside legacy color codes
- Native **Folia** support out of the box
- **Pagination**, anvil/chat text input, persistent player metadata
- In-game visual **menu editor** and DM **migration tool**
- Hooks into Vault, PlaceholderAPI, HeadDatabase, ItemsAdder, Oraxen, Nexo

AuMenus has **no required dependencies**. Vault and PlaceholderAPI are optional.

## Getting Started

1. Drop the jar into your `plugins/` folder
2. Restart the server
3. Edit menus in `plugins/AuMenus/menus/`
4. Use `/am reload` to apply changes

**Migrating from DeluxeMenus?** Run `/am migrate deluxemenus` to convert your existing menus automatically.

## Developer API

Add AuMenus as a dependency to register custom actions, requirements, and listen to menu events.

### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.auvq</groupId>
    <artifactId>AuMenus</artifactId>
    <version>v1.0.2</version>
    <scope>provided</scope>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.auvq:AuMenus:v1.0.0")
}
```

Replace `v1.0.0` with the latest release tag.

## Contribute

Contributions are welcome! Fork the repo, make your changes, and submit a pull request.

```bash
git clone https://github.com/auvq/AuMenus.git
cd AuMenus
./gradlew build
```

## Support

- [Discord](https://discord.gg/B7ArGXP6)
- [Issue Tracker](https://github.com/auvq/AuMenus/issues)

## Quick Links

- [bStats](https://bstats.org/plugin/bukkit/AuMenus/30368)
- [JitPack](https://jitpack.io/#auvq/AuMenus)
