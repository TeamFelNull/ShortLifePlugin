package dev.felnull.shortlifeplugin.decoration;

import com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.entity.*;

/**
 * エンティティと出血マテリアルの対応
 *
 * @author Quarri6343
 */
public enum EntityBloodMaterialType {
    /**
     * アメジスト
     */
    AMETHYST(Material.AMETHYST_BLOCK, ImmutableList.of(Creeper.class)),

    /**
     * ソウルファイア
     */
    SOUL_FIRE(Material.SOUL_FIRE, ImmutableList.of(Vex.class)),

    /**
     * スカルク
     */
    SCULK(Material.SCULK, ImmutableList.of(Warden.class)),

    /**
     * 黒曜石
     */
    OBSIDIAN(Material.OBSIDIAN, ImmutableList.of(WitherSkeleton.class, Wither.class)),

    /**
     * 骨
     */
    BONE(Material.BONE_BLOCK, ImmutableList.of(Skeleton.class, SkeletonHorse.class)),

    /**
     * スライム
     */
    SLIME(Material.SLIME_BLOCK, ImmutableList.of(Slime.class)),

    /**
     * 鉄
     */
    IRON(Material.IRON_BLOCK, ImmutableList.of(IronGolem.class));
    
    /**
     * マテリアル
     */
    private Material material;

    /**
     * パーティクルが適用されるエンティティ
     */
    private ImmutableList<Class<? extends LivingEntity>> entityList;

    EntityBloodMaterialType(Material material, ImmutableList<Class<? extends LivingEntity>> entityList) {
        this.material = material;
        this.entityList = entityList;
    }

    public Material getMaterial() {
        return material;
    }

    public ImmutableList<Class<? extends LivingEntity>> getEntityList() {
        return entityList;
    }
}
