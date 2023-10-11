package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import org.jetbrains.annotations.NotNull;

/**
 * チーム試合
 *
 * @author MORIMORI0317
 */
public class TeamMatch extends Match {

    /**
     * チームIDの接頭辞 (slm-試合ID-チーム番号)
     */
    private static final String TEAM_NAME_FORMAT = "slm-%s-%d";

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected TeamMatch(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        super(id, matchMode, matchMap);
    }

    @Override
    public boolean start() {

        /*ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        scoreboard.registerNewTeam(String.format(TEAM_NAME_FORMAT, getId(), 0));*/

        return super.start();
    }

    @Override
    protected void baseTick() {
        super.baseTick();
    }
}
