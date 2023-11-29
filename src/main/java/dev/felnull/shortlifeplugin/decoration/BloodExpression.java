package dev.felnull.shortlifeplugin.decoration;

import com.destroystokyo.paper.ParticleBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * 血液の表現
 *
 * @author MORIMORI0317
 * @see <a href="https://www.spigotmc.org/threads/comprehensive-particle-spawning-guide-1-13-1-19.343001/">参考</a>
 */
public class BloodExpression {

    /**
     * ダメージパーティクルのベースとなる体積
     */
    private static final double BASE_DAMAGE_PARTICLE_VOLUME = 0.648000034332275d;

    /**
     * 最大パーティクル表示数
     */
    private static final int MAX_PARTICLE_COUNT = 300;

    private BloodExpression() {
    }

    /**
     * ダメージパーティクルを生成
     *
     * @param livingEntity      リビングエンティティ
     * @param damageBox         ダメージ箇所の範囲
     * @param criticalDamageBox 主なダメージ箇所の範囲
     * @param damage            ダメージ量
     */
    public static void spawnDamageParticle(@NotNull LivingEntity livingEntity, @NotNull BoundingBox damageBox, Optional<BoundingBox> criticalDamageBox, double damage) {
        if (damage <= 0) {
            return;
        }

        int range = 100;
        Optional<List<Player>> receivers = getReceivers(livingEntity, range);

        double countPar = getCountPar(damage);
        int count = getCount((criticalDamageBox.isPresent() ? 3.5d : 10d), countPar, damageBox);

        // 全体的なパーティクルを表示
        showParticle(livingEntity, damageBox, false, count, receivers);

        criticalDamageBox.ifPresent(boundingBox -> {
            int criticalCount = getCount(15d, countPar, damageBox);

            // 致命的な箇所のパーティクルを表示
            showParticle(livingEntity, boundingBox, true, criticalCount, receivers);
        });
    }

    /**
     * パーティクルの表示対象者を取得
     *
     * @param livingEntity ダメージを負った対象
     * @param range パーティクルの視認範囲
     * @return 表示対象者(全員ならempty)
     */
    private static Optional<List<Player>> getReceivers(@NotNull LivingEntity livingEntity, int range) {
        Optional<List<Player>> receivers;

        if (livingEntity instanceof Player player) {
            /* 対象がプレイヤーであれば、本人以外を指定 */
            receivers = Optional.of(player.getWorld().getNearbyPlayers(livingEntity.getLocation(), range, range, range)
                    .stream()
                    .filter(it -> it != player)
                    .toList());
        } else {
            /* それ以外であれば未指定(全員) */
            receivers = Optional.empty();
        }
        return receivers;
    }

    /**
     * パーティクルの数のダメージ係数を取得
     *
     * @param damage 食らったダメージ
     * @return ダメージ係数
     */
    private static double getCountPar(double damage) {
        return Math.min(damage / 10d, 20d);
    }

    /**
     * 表示すべきパーティクルの数を取得
     *
     * @param multiplier 単純係数
     * @param countPar ダメージ係数
     * @param damageBox ダメージ箇所の範囲
     * @return パーティクルの数
     */
    private static int getCount(double multiplier, double countPar, @NotNull BoundingBox damageBox) {
        return Math.min((int) (multiplier * countPar * (damageBox.getVolume() / BASE_DAMAGE_PARTICLE_VOLUME)),
                MAX_PARTICLE_COUNT);
    }

    /**
     * パーティクルを表示する
     *
     * @param livingEntity ダメージを負った対象
     * @param damageBox ダメージ箇所の範囲
     * @param critical クリティカルダメージかどうか
     * @param count 表示数
     * @param receivers 表示対象
     */
    private static void showParticle(@NotNull LivingEntity livingEntity, @NotNull BoundingBox damageBox, boolean critical, int count, Optional<List<Player>> receivers) {
        ParticleBuilder particleBuilder = createBloodParticleBuilder(livingEntity, damageBox, critical);
        particleBuilder.count(count);
        particleBuilder.receivers(receivers.orElse(null));
        if (livingEntity instanceof Player player) {
            particleBuilder.source(player);
        }
        particleBuilder.spawn();
    }
    
    /**
     * ダメージ箇所を取得
     *
     * @param livingEntity リビングエンティティ
     * @param cause        ダメージケース
     * @return 範囲と限定的なダメージ箇所かどうか
     */
    public static Optional<Pair<BoundingBox, Boolean>> getDamageBox(@NotNull LivingEntity livingEntity, @NotNull EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case KILL, CONTACT, ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, BLOCK_EXPLOSION,
                    ENTITY_EXPLOSION, FALLING_BLOCK, THORNS, FLY_INTO_WALL, CRAMMING, SONIC_BOOM ->
                    Optional.of(Pair.of(livingEntity.getBoundingBox(), false));
            case FALL -> {
                BoundingBox entityBox = livingEntity.getBoundingBox();
                Vector max = entityBox.getMax();
                yield Optional.of(Pair.of(BoundingBox.of(max.clone().setY(max.getY() - (entityBox.getHeight() / 15d)), entityBox.getMin()), true));
            }
            default -> Optional.empty();
        };
    }

    private static ParticleBuilder createBloodParticleBuilder(@NotNull LivingEntity livingEntity, @NotNull BoundingBox damageBox, boolean critical) {
        ParticleBuilder particleBuilder;

        if (livingEntity instanceof Enderman || livingEntity instanceof EnderDragon || livingEntity instanceof Endermite || livingEntity instanceof Shulker) {
            particleBuilder = new ParticleBuilder(Particle.PORTAL);
        } else if (livingEntity instanceof MagmaCube || livingEntity instanceof Blaze) {
            particleBuilder = new ParticleBuilder(Particle.FLAME);
        } else if (livingEntity instanceof Creeper) {
            particleBuilder = new ParticleBuilder(Particle.ITEM_CRACK);
            particleBuilder.data(new ItemStack(Material.GUNPOWDER));
        } else {
            particleBuilder = new ParticleBuilder(critical ? Particle.BLOCK_DUST : Particle.BLOCK_CRACK);

            if (livingEntity instanceof Allay) {
                particleBuilder.data(Material.AMETHYST_BLOCK.createBlockData());
            } else if (livingEntity instanceof Vex) {
                particleBuilder.data(Material.SOUL_FIRE.createBlockData());
            } else if (livingEntity instanceof Warden) {
                particleBuilder.data(Material.SCULK.createBlockData());
            } else if (livingEntity instanceof WitherSkeleton || livingEntity instanceof Wither) {
                particleBuilder.data(Material.OBSIDIAN.createBlockData());
            } else if (livingEntity instanceof Skeleton || livingEntity instanceof SkeletonHorse) {
                particleBuilder.data(Material.BONE_BLOCK.createBlockData());
            } else if (livingEntity instanceof Slime) {
                particleBuilder.data(Material.SLIME_BLOCK.createBlockData());
            } else if (livingEntity instanceof IronGolem) {
                particleBuilder.data(Material.IRON_BLOCK.createBlockData());
            } else {
                particleBuilder.data(Material.REDSTONE_BLOCK.createBlockData());
            }
        }

        particleBuilder.location(livingEntity.getWorld(), damageBox.getCenterX(), damageBox.getCenterY(), damageBox.getCenterZ());
        particleBuilder.offset(damageBox.getWidthX() / 4d, damageBox.getHeight() / 4d, damageBox.getWidthZ() / 4d);

        return particleBuilder;
    }

    /**
     * ダメージパーティクルを出すかどうか
     *
     * @param livingEntity リビングエンティティ
     * @param damage       ダメージ量
     * @return 出すならtrue、出さないならばfalse
     */
    public static boolean isSpawnDamageParticle(@NotNull LivingEntity livingEntity, double damage) {
        return !livingEntity.isInvulnerable() && damage > 0;
    }
}
