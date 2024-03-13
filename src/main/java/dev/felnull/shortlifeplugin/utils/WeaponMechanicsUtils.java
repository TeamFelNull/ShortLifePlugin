package dev.felnull.shortlifeplugin.utils;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.weapon.damage.DamagePoint;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * WeaponMechanics関係のユーティリティクライアント
 *
 * @author MORIMORI0317, nin8995
 */
public class WeaponMechanicsUtils {

    /**
     * 銃ダメージイベントの間だけ付与されるメタデータ名
     */
    private static final String WEAPON_DAMAGE_EVENT_METADATA = "doing-weapon-damage";

    private WeaponMechanicsUtils() {
        throw new AssertionError();
    }

    /**
     * エンティティの指定されたダメージポイント範囲を取得する
     *
     * @param livingEntity エンティティ
     * @param damagePoint  ダメージポイント
     * @return 範囲
     * @see <a href="https://github.com/WeaponMechanics/MechanicsMain/blob/HEAD/WeaponMechanics/src/main/java/me/deecaad/weaponmechanics/weapon/HitHandler.java">参考</a>
     */
    public static Optional<BoundingBox> getDamagePointBox(@NotNull LivingEntity livingEntity, @Nullable DamagePoint damagePoint) {

        if (damagePoint == null) {
            return Optional.empty();
        }

        BoundingBox ret = null;

        Configuration basicConfiguration = WeaponMechanics.getBasicConfigurations();
        EntityType type = livingEntity.getType();
        double entityHeight = livingEntity.getHeight(); // CompatibilityAPI.getEntityCompatibility().getHeight(livingEntity);
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

        return Optional.ofNullable(ret);
    }

    /**
     * 銃のダメージイベントかどうか確認<br/>
     * 銃ダメージでのイベント中のみ特定のメタデータが付与される仕様<br/>
     * もし今後この仕様が変更された場合は、{@link me.deecaad.weaponmechanics.weapon.damage.DamageUtil}を参照
     *
     * @param e ダメージイベント
     * @return 銃ダメージでのイベントかどうか
     */
    public static boolean isDoingWeaponDamageEvent(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        return entity.hasMetadata(WEAPON_DAMAGE_EVENT_METADATA);
    }

    /**
     * プレイヤーのインベントリ内の銃を全てリロードする
     *
     * @param player リロードされるプレイヤー
     */
    public static void reloadAllWeapons(Player player) {
        Inventory inv = player.getInventory();
        //inv.forEach(item -> {});だと１番のアイテムと同じアイテムしか出てこない謎
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                String title = CustomTag.WEAPON_TITLE.getString(item);
                if (title != null && !CustomTag.AMMO_TITLE.hasString(item)) {
                    Configuration config = WeaponMechanics.getConfigurations();
                    WeaponReloadEvent reloadEvent = new WeaponReloadEvent(title, item, player, EquipmentSlot.HAND, 0, 0, config.getInt(title + ".Reload.Magazine_Size"), 0, 0, config.getObject(title + ".Reload.Start_Mechanics", Mechanics.class));
                    Bukkit.getPluginManager().callEvent(reloadEvent);
                    CustomTag.AMMO_LEFT.setInteger(item, reloadEvent.getMagazineSize());
                }
            }
        }
    }
}
