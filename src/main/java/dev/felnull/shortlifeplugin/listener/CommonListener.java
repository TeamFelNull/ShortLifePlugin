package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.SLConfig;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.match.TeamBaseMatch;
import dev.felnull.shortlifeplugin.utils.WeaponMechanicsUtils;
import me.deecaad.weaponmechanics.events.WeaponMechanicsEntityDamageByEntityEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponDamageEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 一般的なイベントリスナー
 *
 * @author MORIMORI0317, miyabi0333, nin8995
 */
public class CommonListener implements Listener {

    /**
     * ダメージパーティクルのベースとなる体積
     */
    private static final double BASE_DAMAGE_PARTICLE_VOLUME = 0.648000034332275d;

    /**
     * 揃えるドット数。MCIDのドット数の最大は95。
     */
    private static final int DOT_LENGTH = 47;


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
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent e) {
        // プレイヤーのみ
        if (e.getEntity() instanceof Player player) {
            // WeaponMechanicsのダメージ以外
            if (!(e instanceof WeaponMechanicsEntityDamageByEntityEvent)) {
                spawnDamageParticle(player.getWorld(), player.getBoundingBox(), null, e.getDamage());
            }
        }
    }

    /**
     * WeaponMechanicsの武器ダメージを受けた際の処理
     *
     * @param e 武器ダメージイベント
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onWeaponDamage(WeaponDamageEntityEvent e) {
        if (e.getVictim() instanceof Player player) {
            spawnDamageParticle(player.getWorld(), player.getBoundingBox(), WeaponMechanicsUtils.getDamagePointBox(player, e.getPoint()), e.getFinalDamage());
        }
    }

    private void spawnDamageParticle(@NotNull World world, @NotNull BoundingBox damageBox, @Nullable BoundingBox subDamageBox, double damage) {
        if (damage > 0) {
            double countPar = Math.min(damage / 10d, 20d);

            int count = (int) (((subDamageBox != null) ? 3.5d : 10d) * countPar * (damageBox.getVolume() / BASE_DAMAGE_PARTICLE_VOLUME));

            world.spawnParticle(Particle.BLOCK_CRACK, damageBox.getCenterX(), damageBox.getCenterY(), damageBox.getCenterZ(),
                    count, damageBox.getWidthX() / 4d, damageBox.getHeight() / 4d, damageBox.getWidthZ() / 4d,
                    Material.REDSTONE_BLOCK.createBlockData());

            if (subDamageBox != null) {
                int subCount = (int) (15d * countPar * (damageBox.getVolume() / BASE_DAMAGE_PARTICLE_VOLUME));

                world.spawnParticle(Particle.BLOCK_DUST, subDamageBox.getCenterX(), subDamageBox.getCenterY(), subDamageBox.getCenterZ(),
                        subCount, subDamageBox.getWidthX() / 4d, subDamageBox.getHeight() / 4d, subDamageBox.getWidthZ() / 4d,
                        Material.REDSTONE_BLOCK.createBlockData());
            }
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
}
