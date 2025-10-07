package com.alfie51m.forceXaeroFairPlay;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
    id = "forcexaerofairplay",
    name = "ForceXaeroFairPlay",
    version = "2.2.0",
    description = "Force Xaero's Minimap FairPlay mode on Velocity servers",
    authors = {"alfie51m"}
)
public class ForceXaeroFairPlay {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final Map<Player, String> playerLastServer = new ConcurrentHashMap<>();

    @Inject
    public ForceXaeroFairPlay(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.configManager = new ConfigManager();
    }

    @Subscribe
    public void onProxyInitialization(com.velocitypowered.api.event.proxy.ProxyInitializeEvent event) {
        configManager.loadConfig();
        logger.info("ForceXaeroFairPlay has been enabled!");
        logger.info("Default mode: {}", configManager.getDefaultMode());
        logger.info("Server modes: {}", configManager.getServerModes());
    }

    @Subscribe
    public void onProxyShutdown(com.velocitypowered.api.event.proxy.ProxyShutdownEvent event) {
        logger.info("ForceXaeroFairPlay has been disabled!");
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        RegisteredServer targetServer = event.getResult().getServer().orElse(null);
        
        if (targetServer == null) {
            return;
        }

        String targetServerName = targetServer.getServerInfo().getName();
        String fromServerName = player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServer().getServerInfo().getName())
                .orElse(null);

        // Store the server the player is connecting to
        playerLastServer.put(player, targetServerName);
        
        if (configManager.isDebugEnabled()) {
            logger.info("Player {} preparing to connect from '{}' to '{}'", 
                player.getUsername(), fromServerName, targetServerName);
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();
        
        // Get the previous server name from our stored data
        final String fromServerName = playerLastServer.containsKey(player) 
            ? player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServer().getServerInfo().getName())
                .orElse(null)
            : null;
        
        // Schedule the mode change with a small delay to ensure player is fully connected
        server.getScheduler().buildTask(this, () -> {
            if (player.isActive()) {
                handlePlayerMode(player, serverName, fromServerName);
            }
        }).delay(1, java.util.concurrent.TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Clean up player data when they disconnect
        playerLastServer.remove(event.getPlayer());
    }

    private void handlePlayerMode(Player player, String toServerName, String fromServerName) {
        if (player.hasPermission("forcexaerofairplay.bypass")) {
            if (configManager.isDebugEnabled()) {
                logger.info("Player {} bypassed minimap mode change (has bypass permission)", player.getUsername());
            }
            return;
        }

        String toServerMode = configManager.getServerMode(toServerName);
        String fromServerMode = fromServerName != null
                ? configManager.getServerMode(fromServerName)
                : "none";

        if (configManager.isDebugEnabled()) {
            logger.info("Player {} switching from server '{}' (mode: {}) to server '{}' (mode: {})", 
                player.getUsername(), fromServerName, fromServerMode, toServerName, toServerMode);
        }

        StringBuilder messageBuilder = new StringBuilder();

        // Always add reset command at the beginning
        messageBuilder.append("§r§e§s§e§t§x§a§e§r§o ");

        // Add mode-specific command
        switch (toServerMode.toLowerCase()) {
            case "fairplay":
                // Disable cave mode everywhere
                messageBuilder.append("§f§a§i§r§x§a§e§r§o");
                if (configManager.isDebugEnabled()) {
                    logger.info("Setting fairplay mode for player {}", player.getUsername());
                }
                break;

            case "fairplay_nether":
                // Disable cave mode everywhere except nether (temporarily not working)
                messageBuilder.append("§f§a§i§r§x§a§e§r§o §x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r");
                if (configManager.isDebugEnabled()) {
                    logger.info("Setting fairplay_nether mode for player {} (NOTE: Nether mode temporarily not working)", player.getUsername());
                }
                break;

            case "disabled":
                // Only disable minimap
                messageBuilder.append("§n§o§m§i§n§i§m§a§p");
                if (configManager.isDebugEnabled()) {
                    logger.info("Disabling minimap for player {}", player.getUsername());
                }
                break;

            case "fairplay_disabled":
                // Fairplay + disabled minimap
                messageBuilder.append("§f§a§i§r§x§a§e§r§o §n§o§m§i§n§i§m§a§p");
                if (configManager.isDebugEnabled()) {
                    logger.info("Setting fairplay + disabled minimap for player {}", player.getUsername());
                }
                break;

            case "fairplay_nether_disabled":
                // Fairplay nether + disabled minimap (temporarily not working)
                messageBuilder.append("§f§a§i§r§x§a§e§r§o §x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r §n§o§m§i§n§i§m§a§p");
                if (configManager.isDebugEnabled()) {
                    logger.info("Setting fairplay_nether + disabled minimap for player {} (NOTE: Nether mode temporarily not working)", player.getUsername());
                }
                break;

            case "none":
            default:
                // Only reset, no additional commands
                if (configManager.isDebugEnabled()) {
                    logger.info("Resetting minimap for player {} (mode: none)", player.getUsername());
                }
                break;
        }

        // Send the complete message
        if (messageBuilder.length() > 0) {
            sendMessage(player, messageBuilder.toString().trim());
            if (configManager.isDebugEnabled()) {
                logger.info("Sent minimap command to player {}: {}", player.getUsername(), messageBuilder.toString().trim());
            }
        }
    }

    private void sendMessage(Player player, String message) {
        try {
            // Check if player is still connected
            if (!player.isActive()) {
                if (configManager.isDebugEnabled()) {
                    logger.debug("Player {} is not active, skipping message", player.getUsername());
                }
                return;
            }
            
            // Send the message as plain text
            player.sendMessage(net.kyori.adventure.text.Component.text(message));
            logger.debug("Sent message to player {}: {}", player.getUsername(), message);
        } catch (Exception e) {
            logger.warn("Failed to send message to player {}: {}", player.getUsername(), e.getMessage());
            if (configManager.isDebugEnabled()) {
                logger.warn("Full error details:", e);
            }
        }
    }

    private class ConfigManager {
        private String defaultMode = "fairplay";
        private boolean debugEnabled = false;
        private final Map<String, String> serverModes = new HashMap<>();

        public void loadConfig() {
            try {
                // Try to load from plugin data directory first
                Path configPath = Paths.get("plugins/forcexaerofairplay/config.yml");
                
                if (!Files.exists(configPath)) {
                    // Copy default config from plugin resources
                    copyDefaultConfig(configPath);
                }
                
                Yaml yaml = new Yaml();
                try (InputStream inputStream = Files.newInputStream(configPath)) {
                    Map<String, Object> data = yaml.load(inputStream);
                    
                    if (data != null) {
                        defaultMode = (String) data.getOrDefault("defaultMode", "fairplay");
                        debugEnabled = (Boolean) data.getOrDefault("debug", false);
                        
                        @SuppressWarnings("unchecked")
                        Map<String, String> serverModesData = (Map<String, String>) data.get("serverModes");
                        if (serverModesData != null) {
                            serverModes.clear();
                            serverModes.putAll(serverModesData);
                        }
                    }
                }
                
                logger.info("Configuration loaded successfully - Default mode: {}, Debug: {}", defaultMode, debugEnabled);
                
            } catch (IOException e) {
                logger.warn("Failed to load config, using defaults", e);
                loadDefaults();
            }
        }

        private void copyDefaultConfig(Path configPath) throws IOException {
            Files.createDirectories(configPath.getParent());
            
            // Copy config from plugin resources
            try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (resourceStream != null) {
                    Files.copy(resourceStream, configPath);
                    logger.info("Default config copied from plugin resources");
                } else {
                    logger.warn("Could not find config.yml in plugin resources, creating default");
                    createDefaultConfig(configPath);
                }
            }
        }

        private void createDefaultConfig(Path configPath) throws IOException {
            Files.createDirectories(configPath.getParent());
            
            String defaultConfig = "# Default mode for all players.\n" +
                    "# Options: none, fairplay, fairplay_nether, disabled, fairplay_disabled, fairplay_nether_disabled\n" +
                    "# none: Only reset (clear all modes)\n" +
                    "# fairplay: Disable cave mode everywhere\n" +
                    "# fairplay_nether: Disable cave mode everywhere except nether (TEMPORARILY NOT WORKING)\n" +
                    "# disabled: Only disable minimap\n" +
                    "# fairplay_disabled: Fairplay + disabled minimap\n" +
                    "# fairplay_nether_disabled: Fairplay nether + disabled minimap (TEMPORARILY NOT WORKING)\n" +
                    "defaultMode: fairplay\n\n" +
                    "# Enable debug logging\n" +
                    "debug: false\n\n" +
                    "# Server-specific modes\n" +
                    "# Only add servers if you want to override default setting.\n" +
                    "serverModes:\n" +
                    " # lobby: none\n" +
                    " # survival: fairplay\n" +
                    " # creative: disabled\n" +
                    " # pvp: fairplay_disabled\n";
            
            Files.write(configPath, defaultConfig.getBytes());
        }

        private void loadDefaults() {
            defaultMode = "fairplay";
            debugEnabled = false;
            serverModes.clear();
            serverModes.put("lobby", "none");
            serverModes.put("survival", "fairplay");
            serverModes.put("creative", "disabled");
        }

        public String getDefaultMode() {
            return defaultMode;
        }

        public String getServerMode(String serverName) {
            return serverModes.getOrDefault(serverName, defaultMode);
        }

        public Map<String, String> getServerModes() {
            return new HashMap<>(serverModes);
        }

        public boolean isDebugEnabled() {
            return debugEnabled;
        }
    }
}