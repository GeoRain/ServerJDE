package com.example.serverjde;

import com.example.serverjde.config.ConfigManager;
import com.example.serverjde.listeners.ConnectionListener;
import com.example.serverjde.listeners.SpawnProtectionListener;
import com.example.serverjde.listeners.TNTListener;
import com.example.serverjde.utils.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * ServerJDE 主插件类
 * Minecraft 1.20.1 Paper 服务器安全插件
 *
 * 功能：
 * 1. 检测 Minecraft 客户端连接风险
 * 2. 禁止 TNT 使用
 * 3. 保护出生点 64x64x255 区域
 */
public class ServerJDE extends JavaPlugin {

    private static ServerJDE instance;
    private ConfigManager configManager;
    private Logger logger;

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();

        // 初始化配置管理器
        this.configManager = new ConfigManager(this);
        configManager.load();

        // 初始化日志工具
        this.logger = new Logger(this);

        // 注册事件监听器
        registerListeners();

        // 启动消息
        getLogger().info("============================================");
        getLogger().info("  ServerJDE 安全插件已启用 (v" + getDescription().getVersion() + ")");
        getLogger().info("  - 连接风险检测: " + (configManager.isConnectionEnabled() ? "启用" : "禁用"));
        getLogger().info("  - TNT 禁用: " + (configManager.isTntEnabled() ? "启用" : "禁用"));
        getLogger().info("  - 出生点保护: " + (configManager.isSpawnProtectionEnabled() ? "启用" : "禁用"));
        getLogger().info("  出生点保护范围: " + configManager.getSpawnRangeX() + "x" +
                configManager.getSpawnRangeY() + "x" + configManager.getSpawnRangeZ());
        getLogger().info("============================================");

        logger.log("插件启动完成");
    }

    @Override
    public void onDisable() {
        getLogger().info("ServerJDE 安全插件已禁用");
        if (logger != null) {
            logger.log("插件关闭");
        }
    }

    /**
     * 注册所有事件监听器
     */
    private void registerListeners() {
        ConnectionListener connectionListener = new ConnectionListener(this);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(new TNTListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnProtectionListener(this), this);

        // 注册客户端品牌消息通道（用于检测作弊客户端）
        getServer().getMessenger().registerIncomingPluginChannel(this, "minecraft:brand", connectionListener);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("serverjde")) {
            return false;
        }

        if (!sender.hasPermission("serverjde.admin")) {
            sender.sendMessage(configManager.getPrefix() + "§c你没有权限执行此命令！");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                configManager.load();
                sender.sendMessage(configManager.getPrefix() + "§a配置已重新加载！");
                logger.log(sender.getName() + " 重新加载了配置");
            }
            case "status" -> sendStatus(sender);
            case "info" -> sendInfo(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(configManager.getPrefix() + "§e===== ServerJDE 帮助 =====");
        sender.sendMessage("§7/serverjde reload §f- 重新加载配置");
        sender.sendMessage("§7/serverjde status §f- 查看运行状态");
        sender.sendMessage("§7/serverjde info §f- 查看插件信息");
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(configManager.getPrefix() + "§e===== ServerJDE 状态 =====");
        sender.sendMessage("§7连接风险检测: " + (configManager.isConnectionEnabled() ? "§a启用" : "§c禁用"));
        sender.sendMessage("§7TNT 禁用: " + (configManager.isTntEnabled() ? "§a启用" : "§c禁用"));
        sender.sendMessage("§7出生点保护: " + (configManager.isSpawnProtectionEnabled() ? "§a启用" : "§c禁用"));
        sender.sendMessage("§7出生点范围: §f" + configManager.getSpawnRangeX() + "x" +
                configManager.getSpawnRangeY() + "x" + configManager.getSpawnRangeZ());
        sender.sendMessage("§7受保护世界: §f" + String.join(", ", configManager.getProtectedWorlds()));
        sender.sendMessage("§7IP 最大连接数: §f" + configManager.getMaxConnectionsPerIp());
        sender.sendMessage("§7阻止客户端品牌: §f" + configManager.getBlockedClientBrands().size() + " 个");
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(configManager.getPrefix() + "§e===== ServerJDE 信息 =====");
        sender.sendMessage("§7版本: §f" + getDescription().getVersion());
        sender.sendMessage("§7作者: §f" + getDescription().getAuthors());
        sender.sendMessage("§7描述: §f" + getDescription().getDescription());
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("serverjde.admin")) {
            return List.of("reload", "status", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    // ============ Getter ============
    public static ServerJDE getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Logger getPluginLogger() {
        return logger;
    }
}
