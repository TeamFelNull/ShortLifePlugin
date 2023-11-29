package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.SLConfig;
import dev.felnull.shortlifeplugin.SLPermissions;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.decoration.BloodExpression;
import dev.felnull.shortlifeplugin.match.MatchManager;
import dev.felnull.shortlifeplugin.match.TeamBaseMatch;
import dev.felnull.shortlifeplugin.utils.WeaponMechanicsUtils;
import me.deecaad.weaponmechanics.events.WeaponMechanicsEntityDamageByEntityEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponDamageEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;
import org.bukkit.util.BoundingBox;

import java.util.Optional;

import static org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
import static org.bukkit.Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR;

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

            BloodExpression.getDamageBox(livingEntity, e.getCause()).ifPresent(pointAndIsCritical -> {
                BoundingBox damageBox;
                Optional<BoundingBox> criticalDamageBox;

                if (pointAndIsCritical.getRight()) {
                    damageBox = livingEntity.getBoundingBox();
                    criticalDamageBox = Optional.of(pointAndIsCritical.getLeft());
                } else {
                    damageBox = pointAndIsCritical.getLeft();
                    criticalDamageBox = Optional.empty();
                }

                BloodExpression.spawnDamageParticle(livingEntity, damageBox, criticalDamageBox, e.getDamage());
            });
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
            Optional<BoundingBox> damagePointBox = WeaponMechanicsUtils.getDamagePointBox(livingEntity, e.getPoint());
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
     * @param e PlayerJoin event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerJoin(PlayerJoinEvent e) {
        Player joinp = e.getPlayer();
        joinp.sendMessage(Component.text("ShortLifeへようこそ!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

    }

    /**
     * プレイヤーがワールド間を移動した際の処理
     *
     * @param e プレイヤーワールド変更イベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        World fromWorld = e.getFrom();
        World toWorld = player.getWorld();

        // 試合中のワールドに、想定外の方法で参加した時の警告表示
        if (fromWorld != toWorld) {
            MatchManager matchManager = MatchManager.getInstance();
            // 移動先のワールドで試合が行われている場合
            matchManager.getMatchByWorld(toWorld).ifPresent(worldMatch -> {
                
                // プレイヤーが未参加または、参加している試合が世界の試合と一致しない場合
                if (matchManager.getJoinedMatch(player).stream().noneMatch(match -> match == worldMatch)) {
                    Component message = Component.text("試合中").color(NamedTextColor.RED)
                            .append(worldMatch.createDisplayComponent().color(NamedTextColor.GOLD))
                            .append(Component.text("のワールドに、想定外の方法で侵入したようです。"));

                    message = message.appendNewline().append(Component.text("試合に参加していないため、干渉することはできません。"));

                    // 権限がある場合は、参加用メッセージを追加
                    if (player.hasPermission(SLPermissions.COMMANDS_MATCH)) {
                        Component clickHere = Component.text("[ここをクリック]")
                                .style(Style.style().color(NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/match join " + worldMatch.getId())).build());

                        message = message.appendNewline().append(Component.text("参加する場合は")
                                .append(clickHere)
                                .append(Component.text("してください。")));
                    }

                    message = message.appendNewline().append(Component.text("故意で侵入していない場合は、不具合として報告してください！"));

                    player.sendMessage(message);
                }
            });
        }
    }
}
