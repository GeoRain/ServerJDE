package com.example.serverjde.config;

import com.example.serverjde.ServerJDE;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理器
 * 集中管理所有配置项的读取和缓存
 */
public class ConfigManager {

    private final ServerJDE plugin;
    private FileConfiguration config;

    // 连接风险检测配置
    private boolean connectionEnabled;
    private int maxConnectionsPerIp;
    private int connectionFloodTimeframeMs;
    private int connectionFloodMaxAttempts;
    private boolean blockVpn;
    private List<String> blockedClientBrands;
    private List<String> blacklistedIps;
    private boolean kickOnRisk;
    private String kickMessage;

    // TNT 配置
    private boolean tntEnabled;
    private boolean tntBlockPlace;
    private boolean tntBlockIgnite;
    private boolean tntBlockExplosion;
    private boolean tntBlockCraft;
    private boolean tntRemoveExisting;
    private String tntDenyMessage;

    // 出生点保护配置
    private boolean spawnProtectionEnabled;
    private int spawnRangeX;
    private int spawnRangeY;
    private int spawnRangeZ;
    private List<String> protectedWorlds;
    private boolean spawnBlockBreak;
    private boolean spawnBlockPlace;
    private boolean spawnBlockExplosion;
    private boolean spawnBlockFluidFlow;
    private boolean spawnBlockPiston;
    private boolean spawnBlockFire;
    private boolean spawnBlockFarmlandTrample;
    private String spawnDenyMessage;

    // 日志配置
    private boolean consoleLog;
    private boolean fileLog;
    private boolean notifyAdmins;

    // 通用配置
    private String prefix;

    public ConfigManager(ServerJDE plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.config = plugin.getConfig();

        // 连接风险检测
        this.connectionEnabled = config.getBoolean("connection.enabled", true);
        this.maxConnectionsPerIp = config.getInt("connection.max-connections-per-ip", 3);
        this.connectionFloodTimeframeMs = config.getInt("connection.connection-flood-timeframe-ms", 1000);
        this.connectionFloodMaxAttempts = config.getInt("connection.connection-flood-max-attempts", 5);
        this.blockVpn = config.getBoolean("connection.block-vpn", false);
        this.blockedClientBrands = config.getStringList("connection.blocked-client-brands");
        this.blacklistedIps = config.getStringList("connection.blacklisted-ips");
        this.kickOnRisk = config.getBoolean("connection.kick-on-risk", true);
        this.kickMessage = config.getString("connection.kick-message",
                "§c[ServerJDE] §f检测到异常客户端连接，已阻止！");

        // TNT
        this.tntEnabled = config.getBoolean("tnt.enabled", true);
        this.tntBlockPlace = config.getBoolean("tnt.block-place", true);
        this.tntBlockIgnite = config.getBoolean("tnt.block-ignite", true);
        this.tntBlockExplosion = config.getBoolean("tnt.block-explosion", true);
        this.tntBlockCraft = config.getBoolean("tnt.block-craft", true);
        this.tntRemoveExisting = config.getBoolean("tnt.remove-existing-on-load", false);
        this.tntDenyMessage = config.getString("tnt.deny-message", "§c[ServerJDE] §fTNT 已被禁止使用！");

        // 出生点保护
        this.spawnProtectionEnabled = config.getBoolean("spawn-protection.enabled", true);
        this.spawnRangeX = config.getInt("spawn-protection.range-x", 64);
        this.spawnRangeY = config.getInt("spawn-protection.range-y", 255);
        this.spawnRangeZ = config.getInt("spawn-protection.range-z", 64);
        this.protectedWorlds = config.getStringList("spawn-protection.protected-worlds");
        if (protectedWorlds.isEmpty()) {
            protectedWorlds = new ArrayList<>(List.of("world"));
        }
        this.spawnBlockBreak = config.getBoolean("spawn-protection.block-break", true);
        this.spawnBlockPlace = config.getBoolean("spawn-protection.block-place", true);
        this.spawnBlockExplosion = config.getBoolean("spawn-protection.block-explosion", true);
        this.spawnBlockFluidFlow = config.getBoolean("spawn-protection.block-fluid-flow", true);
        this.spawnBlockPiston = config.getBoolean("spawn-protection.block-piston", true);
        this.spawnBlockFire = config.getBoolean("spawn-protection.block-fire", true);
        this.spawnBlockFarmlandTrample = config.getBoolean("spawn-protection.block-farmland-trample", true);
        this.spawnDenyMessage = config.getString("spawn-protection.deny-message",
                "§c[ServerJDE] §f出生点区域受保护，禁止破坏！");

        // 日志
        this.consoleLog = config.getBoolean("logging.console-log", true);
        this.fileLog = config.getBoolean("logging.file-log", true);
        this.notifyAdmins = config.getBoolean("logging.notify-admins", true);

        // 通用
        this.prefix = config.getString("prefix", "§8[§bServerJDE§8] §7");
    }

    // ============ 连接风险 Getter ============
    public boolean isConnectionEnabled() { return connectionEnabled; }
    public int getMaxConnectionsPerIp() { return maxConnectionsPerIp; }
    public int getConnectionFloodTimeframeMs() { return connectionFloodTimeframeMs; }
    public int getConnectionFloodMaxAttempts() { return connectionFloodMaxAttempts; }
    public boolean isBlockVpn() { return blockVpn; }
    public List<String> getBlockedClientBrands() { return blockedClientBrands; }
    public List<String> getBlacklistedIps() { return blacklistedIps; }
    public boolean isKickOnRisk() { return kickOnRisk; }
    public String getKickMessage() { return kickMessage; }

    // ============ TNT Getter ============
    public boolean isTntEnabled() { return tntEnabled; }
    public boolean isTntBlockPlace() { return tntBlockPlace; }
    public boolean isTntBlockIgnite() { return tntBlockIgnite; }
    public boolean isTntBlockExplosion() { return tntBlockExplosion; }
    public boolean isTntBlockCraft() { return tntBlockCraft; }
    public boolean isTntRemoveExisting() { return tntRemoveExisting; }
    public String getTntDenyMessage() { return tntDenyMessage; }

    // ============ 出生点保护 Getter ============
    public boolean isSpawnProtectionEnabled() { return spawnProtectionEnabled; }
    public int getSpawnRangeX() { return spawnRangeX; }
    public int getSpawnRangeY() { return spawnRangeY; }
    public int getSpawnRangeZ() { return spawnRangeZ; }
    public List<String> getProtectedWorlds() { return protectedWorlds; }
    public boolean isSpawnBlockBreak() { return spawnBlockBreak; }
    public boolean isSpawnBlockPlace() { return spawnBlockPlace; }
    public boolean isSpawnBlockExplosion() { return spawnBlockExplosion; }
    public boolean isSpawnBlockFluidFlow() { return spawnBlockFluidFlow; }
    public boolean isSpawnBlockPiston() { return spawnBlockPiston; }
    public boolean isSpawnBlockFire() { return spawnBlockFire; }
    public boolean isSpawnBlockFarmlandTrample() { return spawnBlockFarmlandTrample; }
    public String getSpawnDenyMessage() { return spawnDenyMessage; }

    // ============ 日志 Getter ============
    public boolean isConsoleLog() { return consoleLog; }
    public boolean isFileLog() { return fileLog; }
    public boolean isNotifyAdmins() { return notifyAdmins; }

    // ============ 通用 Getter ============
    public String getPrefix() { return prefix; }
}
