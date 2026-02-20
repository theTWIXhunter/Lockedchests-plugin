# LockedChests Plugin

A lightweight chest locking plugin for Paper 1.21 built by theTWIXhunter

## Features

- **Key-Based Locking**: Lock chests with customizable keys that can be crafted, given, and duplicated
- **Password Protection**: Set passwords on locked chests for additional security
- **Blank Keys**: Craft blank keys to lock chests, then create specific keys for each locked chest
- **Key Duplication**: Duplicate existing keys with blank keys to share access
- **World-Specific**: Configure whitelist or blacklist mode for specific worlds
- **Flexible Access Control**: Multiple authentication methods (owner, key in hand, key in inventory, password)
- **Customizable Keys**: Fully customize key appearance, name, and lore
- **Admin Tools**: Give keys to players with `/givekey` command

## Commands

- `/lockedchests` (aliases: `/lc`, `/chest`) - Main plugin command with reload functionality
- `/setpassword <password>` - Set a password for a locked chest
- `/unlock` - Unlock a chest you own
- `/givekey <player> <ID/blank> <amount>` - Give keys to players (requires `lockedchests.admin`)

## Permissions

- `lockedchests.use` - Allows using locked chests (default: true)
- `lockedchests.admin` - Admin permissions for commands like `/givekey` (default: op)
- `lockedchests.bypass` - Bypass all chest locks (default: op)

## Configuration

Edit `config.yml` to customize the plugin:

**World Filtering**
-worlds-mode: `false` for blacklist (works everywhere except listed), `true` for whitelist (works only in listed worlds)
- worlds: List of worlds to enable/disable the plugin

**Key Customization**
- key material: Item type for keys (default: TRIPWIRE_HOOK)
- key name: Display name for keys
- key.lore: Lore text for keys (use `%id%` for chest ID)

**Crafting**
- crafting-recipe enabled?: Enable/disable blank key crafting
- crafting-recipe shape: Shapeless or shaped recipe
- crafting-recipe ingredients: Define crafting materials

**Accesscontrol**
- allow-owner: Allow chest owner to open without needing a key
- allow-key-click: Allow opening with key in hand
- allow-key-inventory: Allow opening with key in inventory, not just in the hand
- allow-password: Enable password authentication to be able to share a chest with players without a key.
- password-timeout: Timeout for password entry (seconds)

### Locking Behavior
- ask-on-place: Prompt to lock when placing chest
- locking.allow-key-click-lock: Lock by clicking with blank key
- locking.require-just-placed: Only allow locking recently placed chests
- locking.place-time-window: Time window for locking after placement (seconds)

### Key Duplication
- key-duplication.enabled: Allow duplicating keys by combining them with blank keys in a crafting table

## Building

Run `mvn clean package` to build the plugin. The compiled JAR will be in the `target` folder.

## Installation

1. Build the plugin or download the JAR
2. Place the JAR in your server's `plugins` folder
3. Restart the server
4. Configure the plugin in `plugins/LockedChests/config.yml`
5. Reload or restart the server

## How It Works:
1. **Locking a Chest with a new key**: 
   - Place the chest.
   - Craft a blank key using the configured recipe
   - Right-click the chest with the blank key to lock it
   - A specific key for that chest will be created

1. **Locking a Chest with a new key**:
   - Place the chest.
   - Right-click the chest with the key to lock it
   - This key will now be able to open that chest

2. **Opening a Locked Chest**:
   - Have the correct key in your inventory or hand
   - Enter the password if one is set
   - Be the chest owner

3. **Sharing Access**:
   - Duplicate keys using blank keys in crafting
   - Set a password with `/setpassword` and share it
   - Admins can give keys with `/givekey`

## Author

me.thetwixhunter
