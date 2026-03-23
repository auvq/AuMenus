# Actions

Actions execute in response to clicks, menu open/close, requirement denials, and input submissions.

## Syntax

String format:

```yaml
on_click:
  - player: "spawn"
  - msg: "&aHello!"
  - close
```

Map format with modifiers:

```yaml
on_click:
  - msg: "&aDelayed message!"
    delay: 40
    chance: 50
```

## Modifiers

| Key      | Description |
|----------|-------------|
| `delay`  | Ticks before execution. 20 ticks = 1 second. |
| `chance` | Percentage (0-100) that the action fires. Consecutive `chance` actions form a group -only one fires per group. A non-chance action resets the group. |

## Action Types

| Type | Aliases | Description |
|------|---------|-------------|
| `player` | `p` | Run command as player |
| `console` | `c` | Run command from console. Placeholders resolved. |
| `commandevent` | | Dispatch command through chat processor (triggers PlayerCommandPreprocessEvent) |
| `msg` | `message` | Send message to player |
| `minimessage` | | Send MiniMessage-formatted message |
| `broadcast` | | Send message to all online players |
| `minibroadcast` | | Broadcast MiniMessage-formatted message |
| `chat` | | Make player send a chat message |
| `json` | | Send raw JSON text component to player |
| `jsonbroadcast` | | Broadcast raw JSON text component |
| `open` | `openguimenu`, `openmenu` | Open another menu. Pass args separated by spaces. |
| `close` | | Close current menu |
| `refresh` | | Re-render current menu without closing. Use with a delay when placed after commands (see below). |
| `take_money` | `takemoney` | Withdraw from player balance (Vault) |
| `give_money` | `givemoney` | Deposit to player balance (Vault) |
| `give_exp` | `giveexp` | Give exp points, or levels with `L` suffix |
| `take_exp` | `takeexp` | Remove exp points, or levels with `L` suffix |
| `give_perm` | `givepermission` | Grant permission (Vault) |
| `take_perm` | `takepermission` | Remove permission (Vault) |
| `sound` | | Play sound. Optional volume and pitch space-separated. |
| `broadcast_sound` | `broadcastsound` | Play sound for all players |
| `broadcast_sound_world` | `broadcastsoundworld` | Play sound for players in same world |
| `connect` | | Send player to BungeeCord/Velocity server |
| `meta` | | Manipulate persistent player data |
| `placeholder` | | Evaluate a PAPI placeholder (triggers expansion side effects) |
| `prev_page` | | Go to previous page in paginated menu |
| `next_page` | | Go to next page in paginated menu |
| `anvil_input` | | Open anvil GUI for text input |
| `chat_input` | | Close menu and wait for chat message |

## Refresh After Commands

When using `refresh` after a `player` or `console` action, the command may not have finished processing by the time the menu re-renders. Add a delay to give the command time to complete:

```yaml
on_click:
  - player: sethome myhome
  - sound: entity.experience_orb.pickup
  - refresh                        # too fast - placeholders show old values
```

```yaml
on_click:
  - player: sethome myhome
  - sound: entity.experience_orb.pickup
  - refresh: ''
    delay: 5                       # waits 5 ticks before refreshing
```

A delay of 5-10 ticks is usually enough. Increase if the command triggers async operations.

## Sound Format

Sound names use Minecraft sound keys. Underscores convert to dots automatically if no dots are present.

```yaml
- sound: entity.experience_orb.pickup
- sound: "entity.experience_orb.pickup 0.5 2.0"   # volume pitch
```

## Experience Format

Append `L` for levels instead of points:

```yaml
- give_exp: 200       # 200 experience points
- give_exp: 10L       # 10 levels
```

## Open Menu with Arguments

```yaml
- open: "profile Steve"
- open: "other_menu arg1 arg2"
```

## Meta Actions

```yaml
- meta: set coins INTEGER 100
- meta: add coins INTEGER 50
- meta: subtract coins INTEGER 10
- meta: switch vip BOOLEAN
- meta: remove coins
```

See [Placeholders](Placeholders.md) for the meta system details.

## Anvil Input

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

| Key           | Description |
|---------------|-------------|
| `title`       | Anvil GUI title. Default: `"Input"`. |
| `placeholder` | Default text in rename field. |
| `on_submit`   | Actions when player clicks result slot. `{input}` = typed text. |
| `on_cancel`   | Actions when player closes without submitting. |
| `require`     | Requirement to validate input. Anvil stays open on failure. |

## Chat Input

Close the menu first, then use `chat_input`:

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

| Key           | Description |
|---------------|-------------|
| `prompt`      | Message sent to player. Default: `"Enter input:"`. |
| `timeout`     | Seconds before timeout. 0 = no timeout. Default: 0. |
| `cancel_word` | Word to cancel. Default: `"cancel"`. Case-insensitive. |
| `on_submit`   | Actions on input. `{input}` = typed message. |
| `on_cancel`   | Actions on cancel word. |
| `on_timeout`  | Actions on timeout. |

Only one chat input can be pending per player at a time.
