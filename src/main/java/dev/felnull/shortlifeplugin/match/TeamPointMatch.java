package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.match.map.MatchMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * ポイント制チーム試合
 *
 * @author MORIMORI0317
 */
public class TeamPointMatch extends TeamBaseMatch {

    /**
     * 勝利時のテキスト
     */
    private static final MatchEndTextProvider[] WIN_TEXTS = {
            (myTeam, winners, losers) -> "おめでとう！",
            (myTeam, winners, losers) -> String.format("%sチームに勝ち目はありません", losers.stream()
                    .min(Comparator.comparing(PointMatchTeam::getPoint))
                    .map(MatchTeam::getName)
                    .orElse("負けた"))
    };

    /**
     * 敗北時のテキスト
     */
    private static final MatchEndTextProvider[] LOSE_TEXTS = {
            (myTeam, winners, losers) -> "残念！",
            (myTeam, winners, losers) -> "次は頑張ろう",
            (myTeam, winners, losers) -> "何で負けたか、次の試合まで考えといてください",
            (myTeam, winners, losers) -> "なかなか難しいねんな..."
    };

    /**
     * 引き分け時のテキスト
     */
    private static final MatchEndTextProvider[] DRAW_TEXTS = {
            (myTeam, winners, losers) -> "こんなんじゃ勝負になんないよ"
    };

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

    @Override
    protected MatchTeam createMatchTeam(@NotNull String name, @NotNull NamedTextColor color, @NotNull NamespacedKey respawnPointMaker) {
        return new PointMatchTeam(name, color, respawnPointMaker);
    }

    @Override
    protected PlayerData createPlayerData(@NotNull Player player) {
        return new PointTeamPlayerData(player);
    }

    /**
     * ポイント制チーム試合のプレイヤーデータを取得
     *
     * @param player プレイヤー
     * @return プレイヤーデータ
     */
    @Nullable
    public PointTeamPlayerData getPointTeamPlayerData(@NotNull Player player) {
        return (PointTeamPlayerData) getTeamPlayerData(player);
    }

    @Override
    protected void onPlayerKill(@NotNull Player target, @NotNull Player attacker) {
        super.onPlayerKill(target, attacker);

        PointMatchTeam targetMatchTeam = (PointMatchTeam) getTeamByPlayer(target);
        PointMatchTeam attackerMatchTeam = (PointMatchTeam) getTeamByPlayer(attacker);

        // ポイントを付与
        if (targetMatchTeam != null && attackerMatchTeam != null && targetMatchTeam != attackerMatchTeam) {
            attackerMatchTeam.setPoint(attackerMatchTeam.getPoint() + 1);
        }
    }

    @Override
    protected void matchEnd() {
        // 最も点数が多いチームを取得
        List<PointMatchTeam> mostPointTeams = new LinkedList<>();
        int maxPoint = -1;
        for (MatchTeam team : teams) {
            int point = ((PointMatchTeam) team).getPoint();
            if (maxPoint == -1 || point > maxPoint) {
                mostPointTeams.clear();
                maxPoint = point;
                mostPointTeams.add((PointMatchTeam) team);
            } else if (point == maxPoint) {
                mostPointTeams.add((PointMatchTeam) team);
            }
        }


        boolean draw = true;

        // 引き分け確認
        for (MatchTeam team : teams) {
            if (!mostPointTeams.contains((PointMatchTeam) team)) {
                draw = false;
                break;
            }
        }

        // 負けチーム、引き分け時は空
        List<PointMatchTeam> losers = new LinkedList<>();

        if (!draw) {
            // 負けチームのみ抽出
            losers.addAll(teams.stream()
                    .filter(it -> it instanceof PointMatchTeam)
                    .map(it -> (PointMatchTeam) it)
                    .filter(team -> !mostPointTeams.contains(team))
                    .limit(teams.size() - mostPointTeams.size())
                    .toList());
        }

        // 勝敗処理
        for (MatchTeam team : teams) {
            Component retText;
            MatchEndTextProvider subtitleProvider;

            if (draw) {
                retText = Component.text("引き分け").color(NamedTextColor.GRAY);
                subtitleProvider = DRAW_TEXTS[RANDOM.nextInt(DRAW_TEXTS.length)];
            } else if (mostPointTeams.contains((PointMatchTeam) team)) {
                retText = Component.text("勝利").color(NamedTextColor.RED);
                subtitleProvider = WIN_TEXTS[RANDOM.nextInt(WIN_TEXTS.length)];
            } else {
                retText = Component.text("敗北").color(NamedTextColor.BLUE);
                subtitleProvider = LOSE_TEXTS[RANDOM.nextInt(LOSE_TEXTS.length)];
            }

            String subtitle = subtitleProvider.provide((PointMatchTeam) team, mostPointTeams, losers);
            Title.Times times = Title.Times.times(Ticks.duration(10), Duration.ofMillis(FINISH_WAIT_FOR_TELEPORT - (FINISH_WAIT_FOR_TELEPORT / 4)), Ticks.duration(20));
            Title title = Title.title(retText, Component.text(subtitle), times);
            team.audience().showTitle(title);
        }
    }

    /**
     * ポイント制試合チーム
     *
     * @author MORIMORI0317
     */
    public class PointMatchTeam extends MatchTeam {

        /**
         * 獲得済みポイント
         */
        private int point;

        /**
         * コンストラクタ
         *
         * @param name              チーム名
         * @param color             チームカラー
         * @param respawnPointMaker リスポーン地点
         */
        public PointMatchTeam(String name, NamedTextColor color, NamespacedKey respawnPointMaker) {
            super(name, color, respawnPointMaker);
        }

        public int getPoint() {
            return point;
        }

        /**
         * ポイントセットと表示更新フラグを立てる
         *
         * @param point ポイント
         */
        public void setPoint(int point) {
            this.point = point;
            TeamPointMatch.this.dirtyAllInfo();
        }
    }

    /**
     * ポイント制チーム試合のプレイヤーデータ
     *
     * @author MORIMORI0317
     */
    public class PointTeamPlayerData extends TeamPlayerData {

        /**
         * コンストラクタ
         *
         * @param player プレイヤー
         */
        public PointTeamPlayerData(@NotNull Player player) {
            super(player);
        }


        @Override
        protected void appendSidebarMatchInfo(@NotNull List<String> sidebarInfos) {
            super.appendSidebarMatchInfo(sidebarInfos);

            // 得点表示
            StringBuilder pointSb = new StringBuilder();
            pointSb.append("ポイント ");

            List<String> teamPointTexts = new LinkedList<>();
            for (MatchTeam team : TeamPointMatch.this.teams) {
                String text = team.getName() + ": " + ((PointMatchTeam) team).getPoint();
                teamPointTexts.add(text);
            }

            pointSb.append(String.join(", ", teamPointTexts));

            sidebarInfos.add(pointSb.toString());
        }
    }

    /**
     * 試合終了時のテキスト取得用
     *
     * @author MORIMORI0317
     */
    private interface MatchEndTextProvider {

        /**
         * 試合終了時のテキスト取得
         *
         * @param myTeam  自分のチーム
         * @param winners 勝ったチーム
         * @param losers  負けチーム、引き分け時は空
         * @return 文字列
         */
        @NotNull
        String provide(@NotNull PointMatchTeam myTeam, @NotNull @Unmodifiable List<PointMatchTeam> winners, @NotNull @Unmodifiable List<PointMatchTeam> losers);
    }
}
