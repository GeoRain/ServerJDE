package com.example.serverjde.listeners;

import com.example.serverjde.ServerJDE;
import com.example.serverjde.config.ConfigManager;
import com.example.serverjde.utils.Logger;
import com.example.serverjde.utils.TextUtil;
import io.papermc.paper.event.player.AsyncPlayerClientBrandEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 连接风险检测监听器
 *
 * 检测内容：
 * 1. 同一 IP 多账号连接
 * 2. 连接洪水攻击（短时间内高频连接）
 * 3. 异常客户端品牌（作弊客户端）
 * 4. 黑名单 IP
 */
public class ConnectionListener implements Listener {

    private final ServerJDE plugin;
    private final ConfigManager config;
    private final Logger logger;

    // IP -> 连接时间戳列表（用于洪水检测）
    private final Map<String, List<Long>> ipConnectionTimes = new ConcurrentHashMap<>();
    // IP -> 客户端品牌（缓存）
    private final Map<String, String> ipClientBrand = new ConcurrentHashMap<>();

    public ConnectionListener(ServerJDE plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.logger = plugin.getPluginLogger();
    }

    /**
     * 预登录阶段：IP 黑名单检查、连接洪水检测
     * 注意：不在此阶段递增在线计数（玩家可能被踢出，避免计数泄漏）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!config.isConnectionEnabled()) return;

        InetAddress address = event.getAddress();
        String ip = address.getHostAddress();
        String name = event.getName();

        // 1. 黑名单 IP 检查
        if (config.getBlacklistedIps().stream().anyMatch(ip::equalsIgnoreCase)) {
            String reason = "黑名单 IP: " + ip;
            logger.log(reason, Logger.Level.WARNING);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    config.getKickMessage() + "\n§7原因: IP 在黑名单中");
            return;
        }

        // 2. 连接洪水检测
        if (isConnectionFlood(ip)) {
            String reason = "IP 连接洪水: " + ip + " 玩家: " + name;
            logger.log(reason, Logger.Level.WARNING);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    config.getKickMessage() + "\n§7原因: 连接频率过高");
            return;
        }

        // 3. 同一 IP 多账号检测（基于在线玩家计数）
        int onlineFromIp = countOnlineFromIp(ip);
        if (onlineFromIp >= config.getMaxConnectionsPerIp()) {
            String reason = String.format("同 IP 多账号 | IP: %s | 玩家: %s | 在线数: %d",
                    ip, name, onlineFromIp);
            logger.log(reason, Logger.Level.WARNING);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    config.getKickMessage() + "\n§7原因: 同 IP 账号数已达上限 (" +
                            config.getMaxConnectionsPerIp() + ")");
        }
    }

    /**
     * 登录阶段：再次检查 IP 多账号上限（防止竞态）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!config.isConnectionEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.connection")) return;

        String ip = event.getAddress().getHostAddress();
        int onlineFromIp = countOnlineFromIp(ip);

        if (onlineFromIp >= config.getMaxConnectionsPerIp()) {
            String reason = String.format("同 IP 多账号 | IP: %s | 玩家: %s | 在线数: %d",
                    ip, player.getName(), onlineFromIp);
            logger.log(reason, Logger.Level.WARNING);
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    config.getKickMessage() + "\n§7原因: 同 IP 账号数已达上限 (" +
                            config.getMaxConnectionsPerIp() + ")");
        }
    }

    /**
     * 玩家加入：记录日志（IP 计数已在登录阶段增加）
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.connection")) return;

        String ip = getPlayerIp(player);
        if (ip == null) return;

        logger.log("玩家加入 | 玩家: " + player.getName() + " | IP: " + ip);
    }

    /**
     * Paper 客户端品牌接收事件
     * 用于检测作弊客户端
     */
    @EventHandler
    public void onClientBrand(AsyncPlayerClientBrandEvent event) {
        if (!config.isConnectionEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.connection")) return;

        String brand = event.getBrand();
        String ip = getPlayerIp(player);

        logger.log("客户端品牌 | 玩家: " + player.getName() + " | 品牌: " + brand + " | IP: " + ip);
        if (ip != null) {
            ipClientBrand.put(ip, brand);
        }

        // 检查是否为被阻止的客户端品牌
        if (isBlockedBrand(brand)) {
            String reason = "异常客户端品牌: " + brand + " | 玩家: " + player.getName();
            logger.logRisk(player, reason);

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.kick(TextUtil.toComponent(
                        config.getKickMessage() + "\n§7原因: 异常客户端 " + brand));
            });
        }
    }

    /**
     * 玩家退出：清理 IP 品牌缓存
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String ip = getPlayerIp(player);
        if (ip != null) {
            ipClientBrand.remove(ip);
        }
    }

    // ============ 工具方法 ============

    private boolean isConnectionFlood(String ip) {
        long now = System.currentTimeMillis();
        long timeframe = config.getConnectionFloodTimeframeMs();
        int maxAttempts = config.getConnectionFloodMaxAttempts();

        List<Long> times = ipConnectionTimes.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>());

        // 移除过期时间戳
        times.removeIf(t -> now - t > timeframe);

        // 检查洪水
        if (times.size() >= maxAttempts) {
            return true;
        }

        times.add(now);
        return false;
    }

    /**
     * 统计来自指定 IP 的当前在线玩家数
     */
    private int countOnlineFromIp(String ip) {
        return (int) Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    String playerIp = getPlayerIp(p);
                    return ip.equalsIgnoreCase(playerIp);
                })
                .count();
    }

    private boolean isBlockedBrand(String brand) {
        if (brand == null || brand.isEmpty()) return false;
        List<String> blocked = config.getBlockedClientBrands();
        return blocked.stream().anyMatch(b -> brand.toLowerCase().contains(b.toLowerCase()));
    }

    private String getPlayerIp(Player player) {
        if (player.getAddress() == null) return null;
        return player.getAddress().getAddress().getHostAddress();
    }
}
