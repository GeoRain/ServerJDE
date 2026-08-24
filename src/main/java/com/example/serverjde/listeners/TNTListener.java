package com.example.serverjde.listeners;

import com.example.serverjde.ServerJDE;
import com.example.serverjde.config.ConfigManager;
import com.example.serverjde.utils.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * TNT 禁用监听器
 *
 * 阻止内容：
 * 1. 放置 TNT
 * 2. 点燃 TNT（打火石/火焰弹/箭矢）
 * 3. TNT 爆炸
 * 4. 合成 TNT
 * 5. 发射器发放 TNT
 */
public class TNTListener implements Listener {

    private final ServerJDE plugin;
    private final ConfigManager config;
    private final Logger logger;

    public TNTListener(ServerJDE plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.logger = plugin.getPluginLogger();

        // 启用时清除已存在的 TNT 实体
        if (config.isTntEnabled() && config.isTntRemoveExisting()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    plugin.getServer().getWorlds().forEach(world ->
                            world.getEntitiesByClass(TNTPrimed.class)
                                    .forEach(tnt -> {
                                        tnt.remove();
                                        logger.log("已清除 TNT 实体 | 世界: " + world.getName());
                                    })));
        }
    }

    /**
     * 阻止放置 TNT
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isTntEnabled() || !config.isTntBlockPlace()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.tnt")) return;

        if (event.getBlockPlaced().getType() == Material.TNT) {
            event.setCancelled(true);
            player.sendMessage(config.getTntDenyMessage());
            logger.log("阻止 TNT 放置 | 玩家: " + player.getName() +
                    " | 位置: " + formatLocation(event.getBlock()));
        }
    }

    /**
     * 阻止点燃 TNT（使用打火石或火焰弹）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.isTntEnabled() || !config.isTntBlockIgnite()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("serverjde.bypass.tnt")) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        // 只关注打火石和火焰弹
        if (item.getType() != Material.FLINT_AND_STEEL && item.getType() != Material.FIRE_CHARGE) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.TNT) return;

        event.setCancelled(true);
        player.sendMessage(config.getTntDenyMessage());
        logger.log("阻止点燃 TNT | 玩家: " + player.getName() +
                " | 位置: " + formatLocation(clickedBlock));
    }

    /**
     * 阻止 TNT 爆炸（包括点燃的 TNT、TNT 矿车、苦力怕等若配置启用）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!config.isTntEnabled() || !config.isTntBlockExplosion()) return;

        EntityType type = event.getEntityType();
        if (type == EntityType.TNT || type == EntityType.TNT_MINECART) {
            event.setCancelled(true);
            if (event.getEntity() != null) {
                event.getEntity().remove();
            }
            logger.log("阻止 TNT 爆炸 | 位置: " + formatLocation(event.getLocation().getBlock()) +
                    " | 类型: " + type);
        }
    }

    /**
     * 阻止合成 TNT（实时预览，移除结果）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!config.isTntEnabled() || !config.isTntBlockCraft()) return;

        if (event.getInventory().getResult() != null &&
                event.getInventory().getResult().getType() == Material.TNT) {
            event.getInventory().setResult(null);
        }
    }

    /**
     * 阻止点击合成 TNT（保险检查）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!config.isTntEnabled() || !config.isTntBlockCraft()) return;

        if (event.getRecipe() == null) return;
        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() != Material.TNT) return;

        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage(config.getTntDenyMessage());
            logger.log("阻止合成 TNT | 玩家: " + player.getName());
        }
    }

    /**
     * 阻止发射器发放 TNT
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (!config.isTntEnabled() || !config.isTntBlockPlace()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TNT) return;

        event.setCancelled(true);
        logger.log("阻止发射器发放 TNT | 位置: " + formatLocation(event.getBlock()));
    }

    /**
     * 阻止 TNT 因火焰蔓延而被点燃（点火源保护）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!config.isTntEnabled() || !config.isTntBlockIgnite()) return;

        if (event.getBlock().getType() == Material.TNT) {
            event.setCancelled(true);
        }
    }

    private String formatLocation(Block block) {
        return String.format("世界=%s, x=%d, y=%d, z=%d",
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
