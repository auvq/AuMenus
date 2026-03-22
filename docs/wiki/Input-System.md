# Input System

AuMenus provides two ways to collect text input from players: anvil input and chat input.

## Anvil Input

Opens an anvil GUI where the player types in the rename field and clicks the result slot to submit.

### Configuration

```yaml
on_click:
  - anvil_input:
      title: "Enter pet name"
      placeholder: "Type name here..."
      on_submit:
        - console: "pet rename %player% {input}"
        - msg: "&aPet renamed to {input}!"
      on_cancel:
        - msg: "&7Rename cancelled."
      require:
        type: string_length
        input: "{input}"
        min: 3
        max: 16
        deny: "&cName must be 3-16 characters!"
```

### Options

| Key           | Description                                          |
|---------------|------------------------------------------------------|
| `title`       | Title of the anvil GUI. Defaults to `"Input"`.       |
| `placeholder` | Default text shown in the rename field.              |
| `on_submit`   | Actions executed when the player clicks the result slot (slot 2). |
| `on_cancel`   | Actions executed when the player closes the anvil without submitting. |
| `require`     | Optional requirement block to validate the input before accepting. Uses standard requirement syntax. |

### The {input} placeholder

In `on_submit` actions and within the `require` block, `{input}` is replaced with the text the player typed.

### Input Validation

The `require` block uses the same syntax as other requirements. The `{input}` placeholder is resolved within the requirement config before evaluation.

If the requirement fails, the deny actions fire but the anvil stays open, allowing the player to try again.

```yaml
require:
  type: string_length
  input: "{input}"
  min: 3
  max: 16
  deny: "&cName must be 3-16 characters!"
```

```yaml
require:
  type: regex_matches
  input: "{input}"
  regex: "^[a-zA-Z0-9_]+$"
  deny: "&cOnly letters, numbers, and underscores!"
```

### Behavior

- The anvil opens with a Paper item in slot 0 displaying the placeholder text.
- Clicking the result slot (right slot) submits the input.
- Closing the anvil without clicking the result slot triggers `on_cancel`.
- If a player disconnects, the listener is cleaned up automatically.

## Chat Input

Closes the menu and waits for the player to type a message in chat.

### Configuration

```yaml
on_click:
  - close
  - chat_input:
      prompt: "&eType your nickname. Type &ccancel &eto cancel."
      timeout: 30
      cancel_word: "cancel"
      on_submit:
        - console: "nick %player% {input}"
        - msg: "&aNickname set to {input}!"
      on_cancel:
        - msg: "&7Cancelled."
      on_timeout:
        - msg: "&cTimed out."
```

### Options

| Key           | Description                                                |
|---------------|------------------------------------------------------------|
| `prompt`      | Message sent to the player when input is requested. Defaults to `"Enter input:"`. |
| `timeout`     | Timeout in seconds. 0 means no timeout. Defaults to 0.    |
| `cancel_word` | Word the player types to cancel. Defaults to `"cancel"`.  |
| `on_submit`   | Actions executed when the player types a message.          |
| `on_cancel`   | Actions executed when the player types the cancel word.    |
| `on_timeout`  | Actions executed when the timeout expires.                 |

### The {input} placeholder

In `on_submit` actions, `{input}` is replaced with the message the player typed.

### Behavior

- The menu should be closed before using `chat_input` (add a `close` action before it).
- The player's next chat message is intercepted and cancelled from public chat.
- If the message matches `cancel_word` (case-insensitive), `on_cancel` fires.
- If `timeout` seconds pass without input, `on_timeout` fires.
- Only one chat input can be pending per player at a time.
- If a player disconnects, the pending input is cleaned up.

## Complete Example: Settings Menu

```yaml
title: "&8Player Settings"
command: settings
size: 27

items:
  border:
    material: GRAY_STAINED_GLASS_PANE
    name: " "
    slots: [0-8, 18-26]

  rename_pet:
    material: NAME_TAG
    slot: 11
    name: "&eRename Pet"
    lore:
      - "&7Click to rename your pet."
    on_click:
      - anvil_input:
          title: "Enter pet name"
          placeholder: "Type name here..."
          on_submit:
            - console: "pet rename %player% {input}"
            - msg: "&aPet renamed to {input}!"
          on_cancel:
            - msg: "&7Rename cancelled."
          require:
            type: string_length
            input: "{input}"
            min: 3
            max: 16
            deny: "&cName must be 3-16 characters!"

  set_nickname:
    material: PAPER
    slot: 13
    name: "&eSet Nickname"
    lore:
      - "&7Type your new nickname in chat."
    on_click:
      - close
      - chat_input:
          prompt: "&eType your new nickname. Type &ccancel &eto cancel."
          timeout: 30
          cancel_word: "cancel"
          on_submit:
            - console: "nick %player% {input}"
            - msg: "&aNickname set to {input}!"
          on_cancel:
            - msg: "&7Cancelled."
          on_timeout:
            - msg: "&cTimed out."
```
