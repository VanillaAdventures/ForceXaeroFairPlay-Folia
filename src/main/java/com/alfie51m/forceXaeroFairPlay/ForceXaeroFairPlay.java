package com.alfie51m.forceXaeroFairPlay;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ForceXaeroFairPlay extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ForceXaeroFairPlay has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ForceXaeroFairPlay has been disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        handlePlayerMode(player, player.getWorld().getName(), null);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        handlePlayerMode(player, toWorld, fromWorld);
    }

    private void handlePlayerMode(Player player, String toWorldName, String fromWorldName) {
        if (player.hasPermission("forcexaerofairplay.bypass")) {
            return;
        }

        FileConfiguration config = getConfig();
        String defaultMode = config.getString("defaultMode", "none").toLowerCase();
        String toWorldMode = config.getString("worldModes." + toWorldName, defaultMode).toLowerCase();
        String fromWorldMode = fromWorldName != null
                ? config.getString("worldModes." + fromWorldName, defaultMode).toLowerCase()
                : "none";

        StringBuilder messageBuilder = new StringBuilder();

        if (!fromWorldMode.equals(toWorldMode)) {
            messageBuilder.append("§r§e§s§e§t§x§a§e§r§o ");
        }

        switch (toWorldMode) {
            case "fairplay":
                messageBuilder.append("§f§a§i§r§x§a§e§r§o");
                break;

            case "disabled":
                messageBuilder.append("§n§o§m§i§n§i§m§a§p");
                break;

            case "none":
            default:
                break;
        }

        if (messageBuilder.length() > 0) {
            sendTellraw(player, messageBuilder.toString().trim());
        }
    }

    private void sendTellraw(Player player, String message) {
        String json = String.format("{\"text\":\"%s\"}", message.replace("\"", "\\\""));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "tellraw " + player.getName() + " " + json);
    }
}
