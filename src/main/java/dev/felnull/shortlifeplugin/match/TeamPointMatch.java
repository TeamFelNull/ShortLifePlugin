package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import org.jetbrains.annotations.NotNull;

/**
 * ポイント制チーム試合
 *
 * @author MORIMORI0317
 */
public class TeamPointMatch extends TeamBaseMatch {
    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected TeamPointMatch(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        super(id, matchMode, matchMap);
    }
}
