package dev.felnull.shortlifeplugin.utils;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.file.Configuration;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.damage.DamagePoint;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WeaponMechanics関係のユーティリティクライアント
 *
 * @author MORIMORI0317
 */
public class WeaponMechanicsUtils {

    private WeaponMechanicsUtils() {
    }

    /**
     * エンティティの指定されたダメージポイント範囲を取得する
     *
     * @param livingEntity エンティティ
     * @param damagePoint  ダメージポイント
     * @return 範囲
     * @see <a href="https://github.com/WeaponMechanics/MechanicsMain/blob/HEAD/WeaponMechanics/src/main/java/me/deecaad/weaponmechanics/weapon/HitHandler.java">参考</a>
     */
    @Nullable
    public static BoundingBox getDamagePointBox(@NotNull LivingEntity livingEntity, @NotNull DamagePoint damagePoint) {

        BoundingBox ret = null;

        Configuration basicConfiguration = WeaponMechanics.getBasicConfigurations();
        EntityType type = livingEntity.getType();
        double entityHeight = CompatibilityAPI.getEntityCompatibility().getHeight(livingEntity);
        BoundingBox boundingBox = livingEntity.getBoundingBox();
        Vector boxMin = boundingBox.getMin();
        Vector boxMax = boundingBox.getMax();

        double head = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.HEAD.name());
        double body = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.BODY.name());
        double legs = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.LEGS.name());
        double feet = basicConfiguration.getDouble("Entity_Hitboxes." + type.name() + "." + DamagePoint.FEET.name());

        switch (damagePoint) {
            case HEAD -> {
                if (head > 0) {
                    ret = BoundingBox.of(
                            boxMin.clone().setY(boxMax.getY() - (entityHeight * head)),
                            boxMax);
                }
            }
            case BODY -> {
                if (body > 0) {
                    ret = BoundingBox.of(
                            boxMin.clone().setY(boxMax.getY() - (entityHeight * (head + body))),
                            boxMax.clone().setY(boxMax.getY() - (entityHeight * head)));
                } else if (feet <= 0 && legs <= 0) {
                    ret = livingEntity.getBoundingBox();
                }
            }
            case LEGS -> {
                if (legs > 0) {
                    ret = BoundingBox.of(
                            boxMin.clone().setY(boxMax.getY() - (entityHeight * (head + body + legs))),
                            boxMax.clone().setY(boxMax.getY() - (entityHeight * (head + body))));
                }
            }
            case FEET -> {
                if (feet > 0) {
                    ret = BoundingBox.of(
                            boxMin,
                            boxMax.clone().setY(boxMax.getY() - (entityHeight * (head + body + legs))));
                }
            }
            default -> {
            }
        }

        return ret;
    }
}
