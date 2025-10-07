# ForceXaeroFairPlay for Velocity

This plugin automatically switches Xaero's Minimap mode (FairPlay/Disabled) when players move between servers in Velocity proxy.

## Features

- **Automatic switching**: Plugin automatically sends commands to change minimap mode when switching between servers
- **Server-specific configuration**: Configure different modes for different servers in your network
- **Permission system**: Players with `forcexaerofairplay.bypass` permission can bypass automatic switching
- **Velocity support**: Fully compatible with Velocity proxy

## Installation

1. Download the latest version of the plugin
2. Place the `.jar` file in your Velocity server's `plugins/` folder
3. Restart Velocity
4. Configure settings in `plugins/forcexaerofairplay/config.yml`

## Configuration

### config.yml

```yaml
# Default mode for all players
# Options: none, fairplay, fairplay_nether, disabled, fairplay_disabled, fairplay_nether_disabled
# none: Only reset (clear all modes)
# fairplay: Disable cave mode everywhere
# fairplay_nether: Disable cave mode everywhere except nether
# disabled: Only disable minimap
# fairplay_disabled: Fairplay + disabled minimap
# fairplay_nether_disabled: Fairplay nether + disabled minimap
defaultMode: fairplay

# Enable debug logging
debug: false

# Server-specific modes
# Only add servers if you want to override default setting
serverModes:
  lobby: none
  survival: fairplay
  creative: disabled
  pvp: fairplay_disabled
```

### Minimap Modes

- **none**: Only resets all modes (clears previous settings)
- **fairplay**: Disables cave mode everywhere (standard FairPlay)
- **fairplay_nether**: Disables cave mode everywhere except nether
- **disabled**: Only disables the minimap completely
- **fairplay_disabled**: Combines FairPlay mode + disabled minimap
- **fairplay_nether_disabled**: Combines FairPlay nether mode + disabled minimap

## Permissions

- `forcexaerofairplay.bypass` - Allows player to bypass automatic minimap mode switching

## Debug Mode

The plugin supports detailed logging for debugging. Set `debug: true` in configuration to enable detailed logging:

- Player transitions between servers
- Minimap mode changes
- Sent commands
- Permission bypasses

Example logs with debug mode enabled:
```
[INFO] Player PlayerName switching from server 'lobby' (mode: none) to server 'survival' (mode: fairplay)
[INFO] Mode changed for player PlayerName, sending reset command
[INFO] Setting fairplay mode for player PlayerName
[INFO] Sent minimap command to player PlayerName: §r§e§s§e§t§x§a§e§r§o §f§a§i§r§x§a§e§r§o
```

To disable debug mode, set `debug: false` in configuration.

## How it works

1. When a player connects to a server or switches between servers, the plugin checks the configuration
2. If the mode for the new server differs from the previous one, the plugin sends a regular message to the player
3. The message contains special characters (alternating ampersands and letters) that Xaero's Minimap interprets as mode switching commands

### Special character sequences:

Each mode sends a single message with reset + mode command:

- **reset**: `§r§e§s§e§t§x§a§e§r§o`
- **fairplay**: `§f§a§i§r§x§a§e§r§o`
- **fairplay_nether**: `§f§a§i§r§x§a§e§r§o §x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r`
- **disabled**: `§n§o§m§i§n§i§m§a§p`
- **fairplay_disabled**: `§f§a§i§r§x§a§e§r§o §n§o§m§i§n§i§m§a§p`
- **fairplay_nether_disabled**: `§f§a§i§r§x§a§e§r§o §x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r §n§o§m§i§n§i§m§a§p`

## Compatibility

- **Velocity**: 3.0.0+
- **Java**: 21+
- **Xaero's Minimap**: Any version with FairPlay mode support

## Development

### Building from source

```bash
git clone <repository-url>
cd ForceXaeroFairPlay-Velocity
mvn clean package
```

The built file will be located in the `target/` folder.

### Project structure

```
src/
├── main/
│   ├── java/
│   │   └── com/alfie51m/forceXaeroFairPlay/
│   │       └── ForceXaeroFairPlay.java
│   └── resources/
│       ├── config.yml
│       └── velocity-plugin.json
```

## Differences from Bukkit/Spigot version

- Uses Velocity events instead of Bukkit events
- Works with servers instead of worlds
- Uses `velocity-plugin.json` instead of `plugin.yml`
- Supports Velocity API instead of Spigot API

## Support

If you encounter any issues or have questions, create an issue in the project repository.

## License

This project is distributed under the same license as the original plugin.