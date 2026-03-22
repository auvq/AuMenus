# Developer API

Other plugins can open menus, register custom actions and requirements, and listen to menu events through the AuMenus API.

## Dependency Setup

AuMenus is published via JitPack.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.auvq:AuMenus:VERSION")
}
```

### Maven

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.auvq</groupId>
    <artifactId>AuMenus</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```

Replace `VERSION` with the release tag or commit hash.

Add AuMenus as a soft dependency in your `plugin.yml` or `paper-plugin.yml`:

```yaml
softdepend:
  - AuMenus
```

## AuMenusAPI

The `AuMenusAPI` class provides static methods for common operations.

### Opening menus

```java
import me.auvq.aumenus.api.AuMenusAPI;

// Open a menu for a player
AuMenusAPI.openMenu(player, "shop");

// Open a menu with arguments
AuMenusAPI.openMenu(player, "profile", Map.of("player_name", "Steve"));
```

### Querying menus

```java
// Get a menu by name
Optional<Menu> menu = AuMenusAPI.getMenu("shop");

// Get all loaded menus
Collection<Menu> allMenus = AuMenusAPI.getAllMenus();

// Check if a player has an AuMenus menu open
boolean inMenu = AuMenusAPI.isInMenu(player);
```

## Custom Actions

Register a custom action type that can be used in menu configs.

```java
import me.auvq.aumenus.api.AuMenusAPI;

AuMenusAPI.registerAction("heal", (player, value) -> {
    double amount = Double.parseDouble(value);
    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
});
```

Usage in a menu config:

```yaml
on_click:
  - heal: 10
```

The `ActionExecutor` functional interface:

```java
@FunctionalInterface
public interface ActionExecutor {
    void execute(@NotNull Player player, @NotNull String value);
}
```

## Custom Requirements

Register a custom requirement type.

```java
import me.auvq.aumenus.api.AuMenusAPI;

AuMenusAPI.registerRequirement("has_playtime", (player, config) -> {
    int requiredHours = ((Number) config.get("hours")).intValue();
    int actualHours = getPlaytimeHours(player); // your logic
    return actualHours >= requiredHours;
});
```

Usage in a menu config:

```yaml
click_require:
  checks:
    playtime:
      type: has_playtime
      hours: 10
      deny:
        - msg: "&cYou need 10 hours of playtime!"
```

The `RequirementEvaluator` functional interface:

```java
@FunctionalInterface
public interface RequirementEvaluator {
    boolean evaluate(@NotNull Player player, @NotNull Map<String, Object> config);
}
```

The `config` map contains all keys from the check's YAML section (excluding `type`, `deny`, `success`, and `optional`).

## Events

AuMenus fires custom events that other plugins can listen to.

### MenuOpenEvent

Fired when a menu is about to open. Cancellable.

```java
import me.auvq.aumenus.api.event.MenuOpenEvent;

@EventHandler
public void onMenuOpen(MenuOpenEvent event) {
    Player player = event.getPlayer();
    Menu menu = event.getMenu();
    String menuName = menu.getName();

    if (menuName.equals("vip_shop") && !player.hasPermission("vip")) {
        event.setCancelled(true);
        player.sendMessage("VIP only!");
    }
}
```

### MenuCloseEvent

Fired when a menu is closed. Not cancellable. Not fired during plugin reload or when transitioning between menus.

```java
import me.auvq.aumenus.api.event.MenuCloseEvent;

@EventHandler
public void onMenuClose(MenuCloseEvent event) {
    Player player = event.getPlayer();
    Menu menu = event.getMenu();
    // Handle close
}
```

## Accessing Plugin Internals

For advanced use cases, you can access the plugin instance directly:

```java
import me.auvq.aumenus.AuMenus;

AuMenus plugin = AuMenus.getInstance();

// Access registries
plugin.getMenuRegistry();
plugin.getActionRegistry();
plugin.getRequirementRegistry();
plugin.getMetaStore();
plugin.getHookProvider();
```

Note that internal APIs may change between versions. The `AuMenusAPI` static methods are the stable public API surface.
