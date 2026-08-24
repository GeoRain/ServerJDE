package com.example.serverjde.listeners;

import com.example.serverjde.ServerJDE;
import com.example.serverjde.config.ConfigManager;
import com.example.serverjde.utils.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

/**
 * 出生点保护监听器
 *
 * 保护内容（以世界出生点为中心的 64x64x255 区域）：
 * 1. 阻止破坏方块
 * 2. 阻止放置方块
 * 3. 阻止爆炸（TNT/苦力怕/末影龙等）
 * 4. 阻止流体流动破坏
 * 5. 阻止活塞推动
 * 6. 阻止火焰蔓延和烧毁
 * 7. 阻止耕地踩踏
 */
public class SpawnProtectionListener implements Listener {

    private final ServerJDE plugin;
    private final ConfigManager config;
    private final Logger logger;

    public SpawnProtectionListener(ServerJDE plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.logger = plugin.getPluginLogger();
    }

    /**
     * 阻止破坏方块
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockBreak()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.spawn")) return;

        Block block = event.getBlock();
        if (!isInProtectedArea(block)) return;

        event.setCancelled(true);
        player.sendMessage(config.getSpawnDenyMessage());
        logger.log("阻止破坏出生点 | 玩家: " + player.getName() +
                " | 方块: " + block.getType() +
                " | 位置: " + formatLocation(block));
    }

    /**
     * 阻止放置方块
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockPlace()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.spawn")) return;

        Block block = event.getBlock();
        if (!isInProtectedArea(block)) return;

        event.setCancelled(true);
        player.sendMessage(config.getSpawnDenyMessage());
        logger.log("阻止放置出生点 | 玩家: " + player.getName() +
                " | 方块: " + block.getType() +
                " | 位置: " + formatLocation(block));
    }

    /**
     * 阻止倒空桶（水/熔岩/岩浆块等）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockPlace()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.spawn")) return;

        Block block = event.getBlock();
        if (!isInProtectedArea(block)) return;

        event.setCancelled(true);
        player.sendMessage(config.getSpawnDenyMessage());
        logger.log("阻止倒桶出生点 | 玩家: " + player.getName() +
                " | 桶: " + event.getBucket().name() +
                " | 位置: " + formatLocation(block));
    }

    /**
     * 阻止爆炸对出生点造成破坏（TNT/苦力怕/恶魂火球/末影龙等）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockExplosion()) return;

        // 过滤掉受保护区域内的方块
        event.blockList().removeIf(block -> {
            if (isInProtectedArea(block)) {
                logger.log("阻止爆炸破坏出生点 | 位置: " + formatLocation(block));
                return true;
            }
            return false;
        });
    }

    /**
     * 阻止点燃爆炸（防止 TNT/苦力怕在出生点被点燃）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockExplosion()) return;
        if (event.getEntity() == null) return;

        Location loc = event.getEntity().getLocation();
        if (isInProtectedArea(loc.getBlock())) {
            event.setCancelled(true);
            logger.log("阻止在出生点点燃爆炸 | 实体: " + event.getEntityType());
        }
    }

    /**
     * 阻止流体流动（水/熔岩）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockFluidFlow()) return;

        // 流入或流出生点都阻止
        if (isInProtectedArea(event.getToBlock()) || isInProtectedArea(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * 阻止活塞推动方块进入/离开出生点
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (isPistonAffectingSpawn(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
            logger.log("阻止活塞推出影响出生点 | 活塞位置: " + formatLocation(event.getBlock()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (isPistonAffectingSpawn(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
            logger.log("阻止活塞拉回影响出生点 | 活塞位置: " + formatLocation(event.getBlock()));
        }
    }

    /**
     * 检测活塞是否会影响出生点方块
     */
    private boolean isPistonAffectingSpawn(Block piston, List<Block> blocks, BlockFace direction) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockPiston()) return false;

        for (Block block : blocks) {
            Block target = block.getRelative(direction);
            if (isInProtectedArea(block) || isInProtectedArea(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 阻止火焰蔓延与方块烧毁
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockFire()) return;

        if (event.getNewState().getType() == Material.FIRE && isInProtectedArea(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockFire()) return;
        if (isInProtectedArea(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * 阻止耕地踩踏（玩家/实体跳跃破坏耕地）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFarmlandTrample(EntityChangeBlockEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockFarmlandTrample()) return;

        Block block = event.getBlock();
        if (block.getType() == Material.FARMLAND && isInProtectedArea(block)) {
            event.setCancelled(true);
            logger.log("阻止耕地踩踏 | 位置: " + formatLocation(block));
        }
    }

    /**
     * 阻止使用打火石/火焰弹在出生点点火
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.isSpawnProtectionEnabled() || !config.isSpawnBlockFire()) return;
        if (!event.hasItem()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.spawn")) return;

        Material itemType = event.getItem().getType();
        if (itemType != Material.FLINT_AND_STEEL && itemType != Material.FIRE_CHARGE) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        if (isInProtectedArea(clicked) || isInProtectedArea(clicked.getRelative(event.getBlockFace()))) {
            event.setCancelled(true);
            player.sendMessage(config.getSpawnDenyMessage());
        }
    }

    /**
     * 阻止末影珍珠/合唱果传送到出生点（防止刷物）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!config.isSpawnProtectionEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.spawn")) return;

        // 仅阻止珍珠/合唱果的传送，其他传送（如 spawn 命令）不阻止
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL &&
                event.getCause() != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }

        if (event.getTo() == null) return;

        if (isInProtectedArea(event.getTo().getBlock())) {
            event.setCancelled(true);
            player.sendMessage(config.getSpawnDenyMessage());
        }
    }

    // ============ 工具方法 ============

    /**
     * 检查方块是否在出生点保护区域内
     * 以世界出生点为中心：
     * - X/Z 方向：半径 = range / 2（覆盖整个 64x64 区域）
     * - Y 方向：保护全高度（覆盖 0~255，近似全世界高度）
     */
    private boolean isInProtectedArea(Block block) {
        // 先检查世界是否在保护列表
        if (!config.getProtectedWorlds().contains(block.getWorld().getName())) {
            return false;
        }

        Location spawn = block.getWorld().getSpawnLocation();
        int dx = Math.abs(block.getX() - spawn.getBlockX());
        int dz = Math.abs(block.getZ() - spawn.getBlockZ());

        // X/Z 半径 = range / 2
        if (dx > config.getSpawnRangeX() / 2) return false;
        if (dz > config.getSpawnRangeZ() / 2) return false;

        // Y 方向：当 range-y >= 256 时视为"全高度"，直接通过
        // 否则使用 ±range/2 半径
        if (config.getSpawnRangeY() < 256) {
            int dy = Math.abs(block.getY() - spawn.getBlockY());
            if (dy > config.getSpawnRangeY() / 2) return false;
        }

        return true;
    }

    private String formatLocation(Block block) {
        return String.format("世界=%s, x=%d, y=%d, z=%d",
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
