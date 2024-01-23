package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import dev.felnull.shortlifeplugin.match.map.MatchMapHandler;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 試合マップ選択処理用クラス
 *
 * @author MORIMORI0317
 */
public class MapSelector {

    /**
     * マップ決定までの時間 (ms)
     */
    protected static final long SELECTED_DEADLINE_TIME = 1000 * 15;

    /**
     * ランダム
     */
    private static final Random RANDOM = new Random();

    /**
     * このマップ選択が対象の試合
     */
    @NotNull
    private final Match match;

    /**
     * 選定された試合マップ<br/>
     * 未選択時はnull
     */
    @Nullable
    private MatchMap selectedMatchMap;

    /**
     * マップ決定までの残り時間<br/>
     * カウントダウンを開始していない時は-1
     */
    private int deadlineTime = -1;

    /**
     * コンストラクタ
     *
     * @param match 試合
     */
    protected MapSelector(@NotNull Match match) {
        this.match = match;
    }

    /**
     * Tick処理
     */
    protected void tick() {

        // マップ選定済みであれば、処理を行わない
        if (this.selectedMatchMap != null) {
            return;
        }

        int preDeadlineTime = this.deadlineTime;

        // 投票カウントダウン処理
        if (this.match.players.size() >= this.match.getMatchMode().minPlayerCount()) {
            /* 最低参加人数を満たしていれば */

            if (this.deadlineTime == -1) {
                this.deadlineTime = SLUtils.toTick(TimeUnit.MILLISECONDS, SELECTED_DEADLINE_TIME);
            } else {
                this.deadlineTime--;
            }
        } else {
            /* 最低参加人数を下回っているならば */
            this.deadlineTime = -1;
        }

        // マップ決定処理
        if (this.deadlineTime == 0) {
            MatchMap lottedMatchMap = lotteryMatchMap();

            if (lottedMatchMap == null) {
                throw new RuntimeException("マップを決定できませんでした");
            }

            // マップ決定
            this.selectedMatchMap = lottedMatchMap;
        }

        // スコアボード更新処理
        boolean sidebarDirty = false;

        if (this.deadlineTime != preDeadlineTime) {
            if (this.deadlineTime <= -1) {
                sidebarDirty = preDeadlineTime > -1;
            } else if (preDeadlineTime <= -1) {
                sidebarDirty = true;
            } else if (this.deadlineTime / 20 != preDeadlineTime / 20) {
                sidebarDirty = true;
            }
        }

        if (sidebarDirty) {
            this.match.dirtyAllInfo();
        }
    }

    /**
     * マップ抽選処理
     *
     * @return 選ばれたマップ
     */
    private MatchMap lotteryMatchMap() {
        MatchManager matchManager = MatchManager.getInstance();
        MatchMapHandler mapHandler = matchManager.getMapHandler();
        List<MatchMap> maps = mapHandler.getAvailableMaps(this.match.getMatchMode());

        Map<Player, PlayerInfo> players = this.match.players;

        // 投票されたマップリスト
        Map<MatchMap, Long> votedMaps = players.values().stream()
                .map(PlayerInfo::getMapSelectorInfo)
                .filter(mapSelectorInfo -> mapSelectorInfo.votedMatchMap != null)
                .map(mapSelectorInfo -> mapSelectorInfo.votedMatchMap)
                .filter(maps::contains)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        if (votedMaps.isEmpty()) {
            /* 全員未投票の場合 */
            return getRandomMap(maps);
        } else {
            long maxVoteCount = Collections.max(votedMaps.values());
            List<MatchMap> maxVoteMaps = votedMaps.entrySet().stream()
                    .filter(it -> it.getValue() == maxVoteCount)
                    .map(Map.Entry::getKey)
                    .toList();

            if (maxVoteMaps.size() == 1) {
                /* 同じ投票数のマップが存在しない場合 */
                return maxVoteMaps.get(0);
            } else {
                /* 同じ投票数のマップが存在する場合 */
                return getRandomMap(maxVoteMaps);
            }
        }
    }

    private MatchMap getRandomMap(List<MatchMap> maps) {
        if (maps.isEmpty()) {
            return null;
        } else {
            return maps.get(RANDOM.nextInt(maps.size()));
        }
    }

    public int getDeadlineTime() {
        return deadlineTime;
    }

    @Nullable
    public MatchMap getSelectedMatchMap() {
        return selectedMatchMap;
    }

    /**
     * プレイヤーが保持するマップ選択情報
     *
     * @author MORIMORI0317
     */
    public static class MapSelectorInfo {

        /**
         * 投票した試合マップ
         */
        @Nullable
        private MatchMap votedMatchMap;

        @Nullable
        public MatchMap getVotedMatchMap() {
            return votedMatchMap;
        }

        public void setVotedMatchMap(@NotNull MatchMap votedMatchMap) {
            this.votedMatchMap = votedMatchMap;
        }
    }
}
