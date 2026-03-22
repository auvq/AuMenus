# Developer API

Other plugins can open menus, register custom actions and requirements, and listen to menu events.

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

Add AuMenus as a soft dependency in `plugin.yml` or `paper-plugin.yml`:

```yaml
softdepend:
  - AuMenus
```

## AuMenusAPI

```java
import me.auvq.aumenus.api.AuMenusAPI;

// Open a menu
AuMenusAPI.openMenu(player, "shop");

// Open with arguments
AuMenusAPI.openMenu(player, "profile", Map.of("player_name", "Steve"));

// Query menus
Optional<Menu> menu = AuMenusAPI.getMenu("shop");
Collection<Menu> allMenus = AuMenusAPI.getAllMenus();
boolean inMenu = AuMenusAPI.isInMenu(player);
```

## Custom Actions

```java
AuMenusAPI.registerAction("heal", (player, value) -> {
    double amount = Double.parseDouble(value);
    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
});
```

Config usage:

```yaml
on_click:
  - heal: 10
```

The `ActionExecutor` interface:

```java
@FunctionalInterface
public interface ActionExecutor {
    void execute(@NotNull Player player, @NotNull String value);
}
```

## Custom Requirements

```java
AuMenusAPI.registerRequirement("has_playtime", (player, config) -> {
    int requiredHours = ((Number) config.get("hours")).intValue();
    int actualHours = getPlaytimeHours(player);
    return actualHours >= requiredHours;
});
```

Config usage:

```yaml
click_require:
  checks:
    playtime:
      type: has_playtime
      hours: 10
      deny:
        - msg: "&cYou need 10 hours of playtime!"
```

The `RequirementEvaluator` interface:

```java
@FunctionalInterface
public interface RequirementEvaluator {
    boolean evaluate(@NotNull Player player, @NotNull Map<String, Object> config);
}
```

The `config` map contains all keys from the check's YAML section (excluding `type`, `deny`, `success`, `optional`).

## Events

### MenuOpenEvent

Fired before a menu opens. Cancellable.

```java
@EventHandler
public void onMenuOpen(MenuOpenEvent event) {
    Player player = event.getPlayer();
    Menu menu = event.getMenu();

    if (menu.getName().equals("vip_shop") && !player.hasPermission("vip")) {
        event.setCancelled(true);
    }
}
```

### MenuCloseEvent

Fired when a menu closes. Not cancellable. Not fired during reload or menu transitions.

```java
@EventHandler
public void onMenuClose(MenuCloseEvent event) {
    Player player = event.getPlayer();
    Menu menu = event.getMenu();
}
```

## Plugin Internals

For advanced use:

```java
AuMenus plugin = AuMenus.getInstance();
plugin.getMenuRegistry();
plugin.getActionRegistry();
plugin.getRequirementRegistry();
plugin.getMetaStore();
plugin.getHookProvider();
```

Internal APIs may change between versions. The `AuMenusAPI` static methods are the stable surface.
