package com.example.serverjde.utils;

import com.example.serverjde.ServerJDE;
import com.example.serverjde.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志工具
 * 控制台日志、文件日志和管理员通知
 */
public class Logger {

    private final ServerJDE plugin;
    private final File logFile;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Logger(ServerJDE plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("无法创建插件数据目录！");
        }
        this.logFile = new File(dataFolder, "logs.log");
    }

    /**
     * 记录日志（同时输出到控制台和文件）
     */
    public void log(String message) {
        log(message, Level.INFO);
    }

    public void log(String message, Level level) {
        ConfigManager config = plugin.getConfigManager();
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formatted = String.format("[%s] [%s] %s", timestamp, level, message);

        // 控制台
        if (config != null && config.isConsoleLog()) {
            switch (level) {
                case WARNING -> plugin.getLogger().warning(message);
                case ERROR -> plugin.getLogger().severe(message);
                default -> plugin.getLogger().info(message);
            }
        }

        // 文件
        if (config != null && config.isFileLog()) {
            writeToFile(formatted);
        }
    }

    /**
     * 通知有权限的玩家
     */
    public void notifyAdmins(String message) {
        ConfigManager config = plugin.getConfigManager();
        if (config == null || !config.isNotifyAdmins()) return;

        String full = config.getPrefix() + message;
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("serverjde.admin"))
                .forEach(p -> p.sendMessage(full));
    }

    /**
     * 记录玩家风险事件（同时通知管理员）
     */
    public void logRisk(Player player, String reason) {
        String message = String.format("风险检测 | 玩家: %s | IP: %s | 原因: %s",
                player.getName(),
                player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "未知",
                reason);
        log(message, Level.WARNING);
        notifyAdmins("§e风险检测 §6" + player.getName() + " §7- " + reason);
    }

    private void writeToFile(String message) {
        synchronized (logFile) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(message);
            } catch (IOException e) {
                plugin.getLogger().severe("无法写入日志文件: " + e.getMessage());
            }
        }
    }

    public enum Level {
        INFO, WARNING, ERROR
    }
}
