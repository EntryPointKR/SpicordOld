package kr.entree.spicord.bukkit.bootstrap;

import kr.entree.spicord.Spicord;
import kr.entree.spicord.bukkit.discord.BukkitMessage;
import kr.entree.spicord.config.Parameter;
import kr.entree.spicord.config.SpicordConfig;
import kr.entree.spicord.discord.Discord;
import kr.entree.spicord.discord.WebhookManager;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import static kr.entree.spicord.config.SpicordConfig.featureKey;

/**
 * Created by JunHyung Lim on 2019-11-17
 */
public class DiscordChatToBukkit implements Listener {
    private final Plugin plugin;
    private final Discord discord;
    private final SpicordConfig config;
    private final WebhookManager webhookManager;
    private final StringBuilder builder = new StringBuilder();
    private Player last = null;
    private BukkitTask task = null;
    private long lastFlushTime = 0;

    public DiscordChatToBukkit(Plugin plugin, Discord discord, SpicordConfig config, WebhookManager webhookManager) {
        this.plugin = plugin;
        this.discord = discord;
        this.config = config;
        this.webhookManager = webhookManager;
    }

    private void queueNow(Player player, String message) {
        if (config.isFakeProfilePlayerChat()) {
            val sendMessage = new BukkitMessage(webhookManager, player, message);
            discord.addTask(config.getSendMessage("player-chat", sendMessage));
        } else {
            val parameter = new Parameter().put(player)
                    .put("%message%", message);
            discord.addTask(config.getSendMessage("player-chat", parameter));
        }
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void flushQueue() {
        queueNow(last, builder.toString());
        last = null;
        builder.setLength(0);
        cancelTask();
        lastFlushTime = System.currentTimeMillis();
    }

    private void queueSlowly(Player player, String message) {
        if (player.equals(last)) {
            builder.append('\n');
        } else {
            if (last != null) {
                flushQueue();
            }
            last = player;
        }
        val timeDiff = System.currentTimeMillis() - lastFlushTime;
        builder.append(message);
        if (timeDiff >= 3000) {
            flushQueue();
        } else {
            cancelTask();
            task = Bukkit.getScheduler().runTaskLater(Spicord.get(), this::flushQueue, 3L * 20L);
        }
    }

    private void chat(Player player, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String uncolored = ChatColor.stripColor(message);
            if (config.isSlowModePlayerChat()) {
                queueSlowly(player, uncolored);
            } else {
                queueNow(player, uncolored);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        chat(e.getPlayer(), e.getMessage());
    }

    private boolean isJoinQuitEnabled() {
        return config.getBoolean(featureKey("player-chat.join-quit"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (isJoinQuitEnabled()) {
            webhookManager.remove(e.getPlayer());
            chat(e.getPlayer(), e.getQuitMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (isJoinQuitEnabled()) {
            chat(e.getPlayer(), e.getJoinMessage());
        }
    }
}