package dev.felnull.shortlifeplugin.match;

import dev.felnull.fnjl.util.FNStringUtil;
import dev.felnull.shortlifeplugin.match.map.MapMarker;
import dev.felnull.shortlifeplugin.match.map.MapMarkerPoints;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * テスト用試合
 *
 * @author MORIMORI0317
 */
public class TestMatch extends Match {

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected TestMatch(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        super(id, matchMode, matchMap);
    }

    @Override
    protected void baseTick() {
        super.baseTick();

        Component timeComponent = Component.text(FNStringUtil.getTimeFormat(this.statusTick * 50L));
        Audience.audience(players).sendActionBar(timeComponent);
    }

    @Override
    protected @Nullable MapMarker getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player) {
        return matchMapWorld.getMakerRandom(MapMarkerPoints.SPAWN);
    }
}
