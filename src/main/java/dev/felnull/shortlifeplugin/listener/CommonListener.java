package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.SLConfig;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.decoration.BloodExpression;
import dev.felnull.shortlifeplugin.match.TeamBaseMatch;
import dev.felnull.shortlifeplugin.utils.WeaponMechanicsUtils;
import me.deecaad.weaponmechanics.events.WeaponMechanicsEntityDamageByEntityEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponDamageEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;
import org.bukkit.util.BoundingBox;

import static org.bukkit.Sound.*;

/**
 * 一般的なイベントリスナー
 *
 * @author MORIMORI0317, miyabi0333, nin8995
 */
public class CommonListener implements Listener {

    /**
     * 揃えるドット数。MCIDのドット数の最大は95。
     */
    private static final int DOT_LENGTH = 0;

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public static void init(ShortLifePlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new CommonListener(), plugin);
    }

    /**
     * プレイヤーが参加した際のイベント
     *
     * @param e プレイヤー参加イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {

        // テストモードの場合、参加時にメッセージを送信
        if (SLConfig.isTestMode()) {
            Component message = Component.text("ShortLifePluginはテストモードです。");
            message = message.appendNewline().append(Component.text("このメッセージを見た場合は運営に報告してください！"));
            message = message.color(NamedTextColor.GREEN);
            e.getPlayer().sendMessage(message);
        }
    }

    /**
     * エンティティがダメージを受けた際の処理
     *
     * @param e エンティティダメージイベント
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        // WeaponMechanicsのダメージ以外
        if (!(e instanceof WeaponMechanicsEntityDamageByEntityEvent) && e.getEntity() instanceof LivingEntity livingEntity
                && BloodExpression.isSpawnDamageParticle(livingEntity, e.getDamage())) {

            Pair<BoundingBox, Boolean> pointAndIsCritical = BloodExpression.getDamageBox(livingEntity, e.getCause());

            if (pointAndIsCritical != null) {
                BoundingBox damageBox;
                BoundingBox criticalDamageBox;

                if (pointAndIsCritical.getRight()) {
                    damageBox = livingEntity.getBoundingBox();
                    criticalDamageBox = pointAndIsCritical.getLeft();
                } else {
                    damageBox = pointAndIsCritical.getLeft();
                    criticalDamageBox = null;
                }

                BloodExpression.spawnDamageParticle(livingEntity, damageBox, criticalDamageBox, e.getDamage());
            }
        }
    }

    /**
     * WeaponMechanicsの武器ダメージを受けた際の処理
     *
     * @param e 武器ダメージイベント
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponDamage(WeaponDamageEntityEvent e) {
        LivingEntity livingEntity = e.getVictim();
        double damage = e.getFinalDamage();
        if (BloodExpression.isSpawnDamageParticle(livingEntity, damage)) {
            BoundingBox damagePointBox = WeaponMechanicsUtils.getDamagePointBox(livingEntity, e.getPoint());
            BloodExpression.spawnDamageParticle(livingEntity, livingEntity.getBoundingBox(), damagePointBox, damage);
        }
    }

    /**
     * プレイヤーが死亡した際のイベント
     *
     * @param e プレイヤー死亡イベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player killed = e.getEntity();
        Player killer = e.getEntity().getKiller();
        if (killer != null) {
            ItemStack stack = killer.getEquipment().getItemInMainHand();
            Component weapon = !stack.isEmpty() ? stack.displayName() : Component.text("[素手]").color(NamedTextColor.RED).decorate(TextDecoration.BOLD);
            NamedTextColor killedColor = TeamBaseMatch.getTeamColor(killed);
            NamedTextColor killerColor = TeamBaseMatch.getTeamColor(killer);

            e.deathMessage(null);
            e.getPlayer().getWorld().sendMessage(Component
                    .text(alignName(killed.getName())).color(killedColor)
                    .append(Component.text(" <-Killed-- ").color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD))
                    .append(Component.text(killer.getName() + " ").color(killerColor))
                    .append(weapon));
        }
    }

    /**
     * 名前に左からスペースを追加して {@link CommonListener#DOT_LENGTH 指定ドット数}に揃える
     *
     * @param name 揃える名前
     * @return 揃えられた名前
     */
    private String alignName(String name) {
        int widthToAdd = Math.max(0, DOT_LENGTH - MinecraftFont.Font.getWidth(name));
        String spaceToAdd = " ".repeat(widthToAdd / 4 + (widthToAdd % 4 != 0 ? 1 : 0));
        return spaceToAdd + name;
    }


    /**
     * 何らかのエンティティーを殺害した際に発生するイベント
     *
     * @param e エンティティー死亡イベント
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer != null) {
            killer.playSound(killer, ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.6f, 0.5f);
            killer.playSound(killer, ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.5f);
        }
    }

    /**
     *
     * @param e PlayerJoin event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerJoin(PlayerJoinEvent e) {
        Player joinp = e.getPlayer();
        joinp.sendMessage(Component.text("Shortlifeへようこそ!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));

    }


}
