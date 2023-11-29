package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.match.Match;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.utils.MatchUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.IOException;

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
        MatchManager matchManager = MatchManager.getInstance();
        matchManager.getJoinedMatch(e.getPlayer()).ifPresent(match -> {
            if (match.getStatus() == Match.Status.STARTED) {
                /* 試合に参加済みで試合中の場合 */

                // リスポーン処理
                match.onRespawn(e.getPlayer());

                // リスポーン地点を変更
                match.lotterySpawnLocation(e.getPlayer()).ifPresent(e::setRespawnLocation);
            }
        });
    }

    /**
     * エンティティがダメージを受けた際の処理
     *
     * @param e エンティティダメージイベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {

        if (e.getEntity() instanceof Player target) {
            MatchManager matchManager = MatchManager.getInstance();
            matchManager.getJoinedMatch(target).ifPresent(match -> {
                Player attacker;

                // プレイヤーからのダメージかどうか
                if (e instanceof EntityDamageByEntityEvent entityDamageByEntityEvent
                        && entityDamageByEntityEvent.getDamager() instanceof Player attackerPlayer) {
                    attacker = attackerPlayer;
                } else {
                    attacker = null;
                }

                // 試合ごとのダメージ処理を呼び出し
                if (!match.onPlayerDamage(target, attacker, e.getDamage(), e.getCause())) {
                    e.setCancelled(true);
                }
            });
        }
    }

    /**
     * プレイヤーが参加した際のイベント
     *
     * @param e プレイヤー参加イベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        MatchManager matchManager = MatchManager.getInstance();
        // 試合に参加してないプレイヤーが、試合ワールドに参加した場合に強制退出
        matchManager.getMatchByWorld(e.getPlayer().getWorld()).ifPresent(worldMatch -> {
            MatchUtils.teleportToLeave(e.getPlayer(), worldMatch.getMatchMapInstance().getStrictWorld());
        });
    }

    /**
     * プレイヤーが死亡した際のイベント
     *
     * @param e プレイヤー死亡イベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player target = e.getPlayer();
        MatchManager matchManager = MatchManager.getInstance();
        // 参加者が死亡した場合、試合の死亡処理を呼ぶ
        matchManager.getJoinedMatch(target).ifPresent(match -> {
            try {
                match.onPlayerDeath(target);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
