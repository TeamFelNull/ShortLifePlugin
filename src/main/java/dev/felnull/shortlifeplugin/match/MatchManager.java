package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.felnull.shortlifeplugin.ShortLifePlugin;
import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapLoader;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * 試合管理システム
 *
 * @author MORIMORI0317
 */
public final class MatchManager {

    /**
     * 試合用マップの読み込み管理
     */
    private final MatchMapLoader mapLoader = new MatchMapLoader();

    /**
     * 管理されている試合のIDとインスタンスのマップ
     */
    private final Map<String, Match> matches = new HashMap<>();


    /**
     * 初期化処理
     *
     * @param plugin プラグイン
     */
    public void init(ShortLifePlugin plugin) {
        this.mapLoader.init(plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, task -> this.tick(), 1, 1);
    }

    /**
     * 1Tickごとの処理
     */
    private void tick() {
        Set<Map.Entry<String, Match>> allMatch = this.matches.entrySet();
        Set<String> removeMatches = new HashSet<>();

        for (final Map.Entry<String, Match> matchEntry : allMatch) {
            boolean removeFlag = false;
            Match match = matchEntry.getValue();

            try {
                match.tick();

                // 試合の破棄フラグがtrueであれば試合を削除
                if (match.isDestroyed()) {
                    removeFlag = true;
                }

            } catch (Exception ex) {
                SLUtils.reportError(ex, "試合Tick処理中に想定外のエラーが発生");

                try {
                    match.unexpectedError();
                    match.destroy();
                } catch (Exception e) {
                    SLUtils.reportError(e, "試合Tickの想定外エラー処理に失敗");
                }

                // 試合の処理中にエラーがあれば試合を削除 (無限エラーログ編を回避)
                removeFlag = true;
            }

            if (removeFlag) {
                removeMatches.add(matchEntry.getKey());
            }
        }

        // 破棄されるべき試合を削除
        removeMatches.forEach(this::removeMatch);
    }

    /**
     * 試合を追加
     *
     * @param matchId   試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合マップ
     * @return 追加された試合
     */
    @Nullable
    public Match addMatch(@NotNull String matchId, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        if (this.matches.containsKey(matchId)) {
            return null;
        }

        Match match = matchMode.matchProvider().provide(matchId, matchMode, matchMap);
        this.matches.put(matchId, match);
        match.init();
        return match;
    }

    /**
     * 試合を破棄して削除
     *
     * @param matchId 試合ID
     */
    public void removeMatch(String matchId) {
        Match match = this.matches.get(matchId);

        if (match != null) {

            try {
                match.dispose();
            } catch (Exception ex) {
                SLUtils.reportError(ex, "試合の破棄に失敗");
            }

            this.matches.remove(matchId);
        }
    }

    /**
     * 破棄処理
     */
    public void dispose() {

        // 全試合を破棄
        List<String> allMatchId = ImmutableList.copyOf(this.matches.keySet());

        for (String matchId : allMatchId) {
            removeMatch(matchId);
        }

        this.matches.clear();

        this.mapLoader.dispose();
    }

    /**
     * 全ての試合を取得する
     *
     * @return IDと試合インスタンスの不変マップ
     */
    @Unmodifiable
    public Map<String, Match> getAllMatch() {
        return ImmutableMap.copyOf(matches);
    }


    /**
     * 試合を取得
     *
     * @param matchId 試合ID
     * @return 試合
     */
    @Nullable
    public Match getMatch(String matchId) {
        return matches.get(matchId);
    }

    /**
     * 指定したプレイヤーが参加する試合を取得する
     *
     * @param player 対象のプレイヤー
     * @return 対象のプレイヤーが参加する試合 (未参加の場合はnull)
     */
    @Nullable
    public Match getJointedMach(@NotNull Player player) {
        return matches.values().stream()
                .filter(match -> match.getAllJoinPlayers().contains(player))
                .limit(1)
                .findFirst()
                .orElse(null);
    }

    /**
     * 指定したワールドで開催されている試合を取得
     *
     * @param world ワールド
     * @return 試合
     */
    @Nullable
    public Match getMachByWorld(@NotNull World world) {
        return matches.values().stream()
                .filter(match -> match.getMatchMapInstance().isStrictWorldMatch(world))
                .limit(1)
                .findFirst()
                .orElse(null);
    }

    public MatchMapLoader getMapLoader() {
        return mapLoader;
    }
}
