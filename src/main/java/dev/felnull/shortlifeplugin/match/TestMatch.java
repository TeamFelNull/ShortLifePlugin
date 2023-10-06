package dev.felnull.shortlifeplugin.match;

import dev.felnull.fnjl.util.FNStringUtil;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

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
}
