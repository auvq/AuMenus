# Actions

Actions are executed in response to clicks, menu open/close events, requirement denials, and input submissions. They are defined as lists in YAML.

## Action Syntax

### Simple string format

```yaml
on_click:
  - player: "spawn"
  - msg: "&aHello!"
  - close
```

### Map format (with modifiers)

```yaml
on_click:
  - msg: "&aDelayed message!"
    delay: 40
    chance: 50
```

## Modifiers

### delay

Delay in ticks before the action executes. 20 ticks = 1 second.

```yaml
- msg: "&aThis appears after 2 seconds"
  delay: 40
```

### chance

Percentage chance (0-100) that the action executes. When multiple consecutive actions have `chance` set, they form a chance group -- only one fires per group.

```yaml
- msg: "&a&lWIN!"
  chance: 50
- msg: "&c&lLOSE!"
  chance: 50
```

In a chance group, once one action fires, the remaining chance actions in the group are skipped. A non-chance action resets the group.

## Command Actions

### player / p

Runs a command as the player.

```yaml
- player: "spawn"
- p: "home"
```

### console / c

Runs a command from the server console. Placeholders like `%player%` are resolved.

```yaml
- console: "give %player% diamond 1"
- c: "eco give %player% 100"
```

### commandevent

Dispatches a command through the player chat processor, triggering PlayerCommandPreprocessEvent. Useful for commands that rely on event interception.

```yaml
- commandevent: "/some_command"
```

## Message Actions

### msg / message

Sends a message to the player. Supports MiniMessage and legacy codes.

```yaml
- msg: "&aHello, %player%!"
- message: "&#00FF7FSuccess!"
```

### minimessage

Sends a MiniMessage-formatted message.

```yaml
- minimessage: "<rainbow>Rainbow text!</rainbow>"
```

### broadcast

Sends a message to all online players.

```yaml
- broadcast: "&e%player% purchased a diamond!"
```

### minibroadcast

Broadcasts a MiniMessage-formatted message.

```yaml
- minibroadcast: "<bold>Server announcement!</bold>"
```

### chat

Makes the player send a chat message (not a command).

```yaml
- chat: "Hello everyone!"
```

### json

Sends a raw JSON text component to the player.

```yaml
- json: '{"text":"Click here","clickEvent":{"action":"open_url","value":"https://example.com"}}'
```

### jsonbroadcast

Broadcasts a raw JSON text component to all players.

```yaml
- jsonbroadcast: '{"text":"Server message"}'
```

## Menu Control Actions

### open / openguimenu

Opens another menu. Optionally pass arguments separated by spaces.

```yaml
- open: shop
- open: "profile Steve"
- open: "other_menu arg1 arg2"
```

### close

Closes the player's current menu.

```yaml
- close
```

### refresh

Re-renders the current menu without closing it. Useful after actions that change state.

```yaml
- refresh
```

## Economy Actions

Require Vault to be installed and an economy provider registered.

### take_money

Withdraws money from the player's balance.

```yaml
- take_money: 100
```

### give_money

Deposits money into the player's balance.

```yaml
- give_money: 50
```

## Experience Actions

### give_exp

Gives experience points, or levels if the value ends with `L`.

```yaml
- give_exp: 200       # 200 experience points
- give_exp: 10L       # 10 levels
```

### take_exp

Removes experience points or levels.

```yaml
- take_exp: 100       # 100 experience points
- take_exp: 5L        # 5 levels
```

## Permission Actions

Require Vault with a permissions provider.

### give_perm

Grants a permission to the player.

```yaml
- give_perm: "some.permission"
```

### take_perm

Removes a permission from the player.

```yaml
- take_perm: "some.permission"
```

## Sound Actions

### sound

Plays a sound for the player. Optionally specify volume and pitch (space-separated).

```yaml
- sound: entity.experience_orb.pickup
- sound: "entity.experience_orb.pickup 0.5 2.0"
```

Sound names use Minecraft sound keys. Underscores are automatically converted to dots if the name does not already contain dots.

### broadcast_sound

Plays a sound for all online players.

```yaml
- broadcast_sound: entity.ender_dragon.death
```

### broadcast_sound_world

Plays a sound for all players in the same world as the triggering player.

```yaml
- broadcast_sound_world: entity.lightning_bolt.thunder
```

## Network Actions

### connect

Sends the player to another BungeeCord/Velocity server.

```yaml
- connect: "lobby"
```

## Meta Actions

Manipulates persistent player data. See [Meta System](Meta-System.md).

```yaml
- meta: set coins INTEGER 100
- meta: add coins INTEGER 50
- meta: subtract coins INTEGER 10
- meta: switch vip BOOLEAN
- meta: remove coins
```

## Placeholder Action

### placeholder

Evaluates a PlaceholderAPI placeholder. Useful for triggering expansion side effects.

```yaml
- placeholder: "%some_expansion_trigger%"
```

## Pagination Actions

### prev_page

Navigates to the previous page in a paginated menu. Does nothing if already on page 1.

```yaml
- prev_page
```

### next_page

Navigates to the next page. Does nothing if already on the last page.

```yaml
- next_page
```

## Input Actions

### anvil_input

Opens an anvil GUI for text input. See [Input System](Input-System.md).

```yaml
- anvil_input:
    title: "Enter name"
    placeholder: "Type here..."
    on_submit:
      - msg: "&aYou typed: {input}"
    on_cancel:
      - msg: "&7Cancelled."
    require:
      type: string_length
      input: "{input}"
      min: 3
      max: 16
      deny: "&cMust be 3-16 characters!"
```

### chat_input

Closes the menu and waits for a chat message. See [Input System](Input-System.md).

```yaml
- close
- chat_input:
    prompt: "&eType your message. Type &ccancel &eto cancel."
    timeout: 30
    cancel_word: "cancel"
    on_submit:
      - msg: "&aYou typed: {input}"
    on_cancel:
      - msg: "&7Cancelled."
    on_timeout:
      - msg: "&cTimed out."
```

## Complete Example

```yaml
items:
  shop_item:
    material: DIAMOND
    slot: 13
    name: "&bDiamond"
    lore:
      - "&7Price: &a$100"
    on_click:
      - take_money: 100
      - console: "give %player% diamond 1"
      - msg: "&aPurchased!"
      - sound: entity.experience_orb.pickup
      - refresh
    click_require:
      money: 100
      deny: "&cYou need $100! Missing: ${remaining}"
```
