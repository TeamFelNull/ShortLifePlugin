package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;


/**
 * どのような試合にするかの定義
 *
 * @param id             試合モードID
 * @param name           試合モードID
 * @param matchType      試合の種類
 * @param limitTime      制限時間(ms)
 * @param numberOfTeam   チームの最大数(1チームで個人)
 * @param minPlayerCount 最低参加プレイヤー数
 * @param matchProvider  試合インスタンス生成ファクトリー
 * @author MORIMORI0317
 */
public record MatchMode(@NotNull String id, @NotNull Component name, @NotNull MatchType matchType, long limitTime,
                        int numberOfTeam, int minPlayerCount, @NotNull MatchMode.MatchProvider matchProvider) {

    /**
     * 試合プロバイダー
     *
     * @author MORIMORI031
     */
    public interface MatchProvider {

        /**
         * 試合を作成
         *
         * @param id        試合ID
         * @param matchMode 試合モード
         * @param matchMap  試合マップ
         * @return 試合
         */
        Match provide(String id, MatchMode matchMode, MatchMap matchMap);
    }
}
