package dev.felnull.shortlifeplugin.decoration;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

/**
 * エンティティと出血パーティクルの対応
 *
 * @author MORIMORI0317, Quarri6343
 */
public enum EntityBloodParticleType {
    /**
     * ポータル
     */
    PORTAL(new ParticleBuilder(Particle.PORTAL), ImmutableList.of(Enderman.class, EnderDragon.class, Endermite.class, Shulker.class)),

    /**
     * 炎
     */
    FLAME(new ParticleBuilder(Particle.FLAME), ImmutableList.of(MagmaCube.class, Blaze.class)),

    /**
     * クリーパー
     */
    CREEPER(new ParticleBuilder(Particle.ITEM_CRACK).data(new ItemStack(Material.GUNPOWDER)), ImmutableList.of(Creeper.class));
    
    /**
     * パーティクル
     */
    private ParticleBuilder particle;

    /**
     * パーティクルが適用されるエンティティ
     */
    private ImmutableList<Class<? extends LivingEntity>> entityList;
    
    EntityBloodParticleType(ParticleBuilder particle, ImmutableList<Class<? extends LivingEntity>> entityList) {
        this.particle = particle;
        this.entityList = entityList;
    }

    public ParticleBuilder getParticle() {
        return particle;
    }
    
    public ImmutableList<Class<? extends LivingEntity>> getEntityList() {
        return entityList;
    }
}
