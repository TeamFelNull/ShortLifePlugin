package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * 試合用イベントリスナー
 *
 * @author MORIMORI0317
 */
public final class MatchListener implements Listener {

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public static void init(ShortLifePlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new MatchListener(), plugin);
    }

    /**
     * プレイヤーがリスポーンした際の処理
     *
     * @param e プレイヤーリスポーンイベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        MatchManager matchManager = SLUtils.getMatchManager();
        Match match = matchManager.getJointedMach(e.getPlayer());

        // 試合に参加済みで試合中であればリスポーン地点を変更
        if (match != null && match.getStatus() == Match.Status.STARTED) {
            Location location = match.lotteryRespawnLocation(e.getPlayer());
            if (location != null) {
                e.setRespawnLocation(location);
            }
        }
    }

    /**
     * エンティティがダメージを受けた際の処理
     *
     * @param e エンティティダメージイベント
     */
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            MatchManager matchManager = SLUtils.getMatchManager();
            Match match = matchManager.getJointedMach(player);

            if (match != null && match.isInvinciblePlayer(player)) {
                e.setCancelled(true);
            }
        }
    }
}
