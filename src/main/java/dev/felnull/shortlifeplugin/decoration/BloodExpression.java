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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 血液の表現
 *
 * @author MORIMORI0317
 */
public class BloodExpression {

    /**
     * ダメージパーティクルのベースとなる体積
     */
    private static final double BASE_DAMAGE_PARTICLE_VOLUME = 0.648000034332275d;

    private BloodExpression() {
    }

    /**
     * ダメージパーティクルパーティクルを生成
     *
     * @param livingEntity      リビングエンティティ
     * @param damageBox         ダメージ箇所の範囲
     * @param criticalDamageBox 主なダメージ箇所の範囲
     * @param damage            ダメージ量
     */
    public static void spawnDamageParticle(@NotNull LivingEntity livingEntity, @NotNull BoundingBox damageBox, @Nullable BoundingBox criticalDamageBox, double damage) {
        if (damage > 0) {
            int range = 100;
            List<Player> receivers;

            if (livingEntity instanceof Player player) {
                /* 対象がプレイヤーであれば、本人以外を指定 */
                receivers = player.getWorld().getNearbyPlayers(livingEntity.getLocation(), range, range, range)
                        .stream()
                        .filter(it -> it != player)
                        .toList();
            } else {
                /* それ以外であれば未指定(全員) */
                receivers = null;
            }

            double countPar = Math.min(damage / 10d, 20d);
            int count = (int) (((criticalDamageBox != null) ? 3.5d : 10d) * countPar * (damageBox.getVolume() / BASE_DAMAGE_PARTICLE_VOLUME));

            // 全体的なパーティクルを表示
            ParticleBuilder particleBuilder = createBloodParticleBuilder(livingEntity, damageBox, false);
            particleBuilder.count(count);
            particleBuilder.receivers(receivers);
            if (livingEntity instanceof Player player) {
                particleBuilder.source(player);
            }
            particleBuilder.spawn();

            if (criticalDamageBox != null) {
                int criticalCount = (int) (15d * countPar * (damageBox.getVolume() / BASE_DAMAGE_PARTICLE_VOLUME));

                // 致命的な箇所のパーティクルを表示
                ParticleBuilder criticalParticleBuilder = createBloodParticleBuilder(livingEntity, criticalDamageBox, true);
                criticalParticleBuilder.count(criticalCount);
                criticalParticleBuilder.receivers(receivers);
                if (livingEntity instanceof Player player) {
                    criticalParticleBuilder.source(player);
                }
                criticalParticleBuilder.spawn();
            }
        }
    }

    /**
     * ダメージ箇所を取得
     *
     * @param livingEntity リビングエンティティ
     * @param cause        ダメージケース
     * @return 範囲と限定的なダメージ箇所かどうか
     */
    @Nullable
    public static Pair<BoundingBox, Boolean> getDamageBox(@NotNull LivingEntity livingEntity, @NotNull EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case KILL, CONTACT, ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, BLOCK_EXPLOSION,
                    ENTITY_EXPLOSION, FALLING_BLOCK, THORNS, FLY_INTO_WALL, CRAMMING, SONIC_BOOM ->
                    Pair.of(livingEntity.getBoundingBox(), false);
            case FALL -> {
                BoundingBox entityBox = livingEntity.getBoundingBox();
                Vector max = entityBox.getMax();
                yield Pair.of(BoundingBox.of(max.clone().setY(max.getY() - (entityBox.getHeight() / 15d)), entityBox.getMin()), true);
            }
            default -> null;
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
