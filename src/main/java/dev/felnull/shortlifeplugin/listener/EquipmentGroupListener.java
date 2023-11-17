package dev.felnull.shortlifeplugin.listener;

import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroup;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import me.deecaad.weaponmechanics.WeaponMechanicsAPI;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponPreReloadEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponPreShootEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponScopeEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * 装備グループ用イベントリスナー
 *
 * @author MORIMORI0317
 */
public class EquipmentGroupListener implements Listener {

    /**
     * 初期化
     *
     * @param plugin プラグイン
     */
    public static void init(ShortLifePlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new EquipmentGroupListener(), plugin);
    }

    /**
     * プレイヤーが作用させた場合のイベント
     *
     * @param e プレイヤーインタラクトイベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.HAND && (e.getAction().isLeftClick() || e.getAction().isRightClick())) {
            if (hotbarRestriction(e.getPlayer(), e.getItem(), true)) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * 武器発射時のイベント
     *
     * @param e 武器発射前イベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponPreShoot(WeaponPreShootEvent e) {
        if (e.getEntity() instanceof Player player && e.getHand() == EquipmentSlot.HAND && hotbarRestriction(player, e.getWeaponStack(), false)) {
            e.setCancelled(true);
        }
    }

    /**
     * 武器再装填時のイベント
     *
     * @param e 武器再装填前イベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponPreReload(WeaponPreReloadEvent e) {
        if (e.getEntity() instanceof Player player && e.getHand() == EquipmentSlot.HAND && hotbarRestriction(player, e.getWeaponStack(), false)) {
            e.setCancelled(true);
        }
    }

    /**
     * 武器を構えた時のイベント
     *
     * @param e 武器スコープイベント
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponScope(WeaponScopeEvent e) {
        if (e.getEntity() instanceof Player player && e.getHand() == EquipmentSlot.HAND && hotbarRestriction(player, e.getWeaponStack(), false)) {
            e.setCancelled(true);
        }
    }

    private boolean hotbarRestriction(@NotNull Player player, @Nullable ItemStack itemStack, boolean skipWeaponMessage) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        List<EquipmentGroup> equipmentGroups = manager.getBelongsGroups(itemStack);
        Inventory inventory = player.getInventory();

        List<ItemStack> hotbarStacks = new LinkedList<>();
        // 0~8のスロットがホットバー
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarStack = inventory.getItem(i);
            if (hotbarStack != null && !hotbarStack.isEmpty()) {
                hotbarStacks.add(hotbarStack);
            }
        }

        // 制限されるグループリスト
        List<EquipmentGroup> restrictedEquipmentGroups = equipmentGroups.stream()
                .filter(equipmentGroup -> equipmentGroup.isRestricted(hotbarStacks))
                .toList();

        // 制限されるグループが存在する場合
        if (!restrictedEquipmentGroups.isEmpty()) {

            if (!(itemStack != null && WeaponMechanicsAPI.getWeaponTitle(itemStack) != null && !skipWeaponMessage)) {
                JoinConfiguration.Builder restrictedGroupComponentBuilder = JoinConfiguration.builder();
                restrictedGroupComponentBuilder.separator(Component.text(","));

                Component[] groupComponents = restrictedEquipmentGroups.stream()
                        .map(equipmentGroup -> Component.text(String.format("%sは%d個", equipmentGroup.name(), equipmentGroup.restriction().maxHotbarExistsCount())))
                        .toArray(Component[]::new);

                Component restrictedGroupComponent = Component.join(restrictedGroupComponentBuilder, groupComponents)
                        .append(Component.text("より多くホットバーに存在できません"))
                        .color(NamedTextColor.RED);


                Audience audience = Audience.audience(player);
                audience.sendMessage(restrictedGroupComponent);
                audience.playSound((Sound.sound(org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE.key(), Sound.Source.MASTER, 1, 1.5f)));

                player.spawnParticle(Particle.REDSTONE, player.getLocation().clone().add(0, 1, 0),
                        25, 0.3f, 0.3f, 0.3f, new Particle.DustOptions(Color.fromRGB(50, 50, 50), 1));
            }

            return true;
        }

        return false;
    }
}
