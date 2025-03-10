package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.integration.TABIntegration;
import dev.felnull.shortlifeplugin.match.map.MapMarker;
import dev.felnull.shortlifeplugin.match.map.MapMarkerPoints;
import dev.felnull.shortlifeplugin.match.map.MatchMapWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;

import static dev.felnull.shortlifeplugin.match.MatchMessageComponents.MATCH_FINISH_TEAM_NO_PARTICIPANTS_MESSAGE;

/**
 * チーム試合
 *
 * @author MORIMORI0317, nin8995
 */
public abstract class TeamBaseMatch extends PVPBaseMatch {

    /**
     * チームのリスト
     */
    protected final List<MatchTeam> teams = new ArrayList<>();

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     */
    protected TeamBaseMatch(@NotNull String id, @NotNull MatchMode matchMode) {
        super(id, matchMode);
    }

    @Override
    protected PlayerInfo createPlayerInfo(@NotNull Player player) {
        return new TeamPlayerInfo(player);
    }

    /**
     * チームのプレイヤー情報を取得
     *
     * @param player プレイヤー
     * @return プレイヤー情報
     */
    public Optional<TeamBaseMatch.TeamPlayerInfo> getTeamPlayerInfo(@NotNull Player player) {
        return getPlayerInfo(player).map(playerInfo -> (TeamBaseMatch.TeamPlayerInfo) playerInfo);
    }


    @Override
    protected void init() {
        super.init();

        // チーム初期化
        teams.add(createMatchTeam(MsgHandler.get("match-team-red"), NamedTextColor.RED, MapMarkerPoints.SPAWN_TEAM1.get()));
        teams.add(createMatchTeam(MsgHandler.get("match-team-blue"), NamedTextColor.BLUE, MapMarkerPoints.SPAWN_TEAM2.get()));
    }

    /**
     * チーム作成
     *
     * @param name              チーム名
     * @param color             チームカラー
     * @param respawnPointMaker リスポーン地点マーカー
     * @return チーム
     */
    protected MatchTeam createMatchTeam(@NotNull String name, @NotNull NamedTextColor color, @NotNull NamespacedKey respawnPointMaker) {
        return new MatchTeam(name, color, respawnPointMaker);
    }


    @Override
    protected void tick() {

        if (getStatus() != MatchStatus.NONE) {
            boolean noParticipantsFlag = teams.stream()
                    .anyMatch(team -> team.getParticipationPlayers().isEmpty());

            // チームに参加者が居なければ試合終了
            if (noParticipantsFlag) {
                broadcast(MATCH_FINISH_TEAM_NO_PARTICIPANTS_MESSAGE.get());
                destroy();
                return;
            }
        }

        super.tick();
    }

    @Override
    protected void playerStart(@NotNull Player player) {
        joinTeam(player);
        super.playerStart(player);
    }

    /**
     * チームにプレイヤーを参加させる
     *
     * @param player プレイヤー
     */
    private void joinTeam(Player player) {

        // 人数が最も少ないチームを取得
        List<MatchTeam> minTeams = new LinkedList<>();

        int minPlayerCount = -1;
        for (MatchTeam team : teams) {
            int playerCount = team.getParticipationPlayers().size();

            if (minPlayerCount == -1 || minPlayerCount > playerCount) {
                minPlayerCount = playerCount;
                minTeams.clear();

                minTeams.add(team);
            } else if (minPlayerCount == playerCount) {
                minTeams.add(team);
            }
        }


        MatchTeam team;
        if (minTeams.isEmpty()) {
            // 人数が最も少ないチームが無い場合はランダムに参加
            team = teams.get(RANDOM.nextInt(teams.size()));
        } else {
            // 人数が最も少ないチームにランダム参加
            team = minTeams.get(RANDOM.nextInt(minTeams.size()));
        }

        team.addPlayer(player);

        // プレイヤーリスト表記色更新
        TABIntegration.setPlayerTabListColor(player, team.getColor());

        dirtyAllInfo();
    }

    @Override
    public boolean leave(@NotNull Player player, boolean sendMessage) {

        // チームから退出
        teams.forEach(matchTeam -> matchTeam.removePlayer(player));

        // プレイヤーリスト表記色リセット
        TABIntegration.setPlayerTabListColor(player, null);

        dirtyAllInfo();

        return super.leave(player, sendMessage);
    }

    /**
     * 指定したプレイヤーが存在するチームを取得
     *
     * @param player プレイヤー
     * @return チーム
     */
    public Optional<MatchTeam> getTeamByPlayer(@NotNull Player player) {
        return this.teams.stream()
                .filter(team -> team.hasParticipation(player))
                .limit(1)
                .findFirst();
    }


    /**
     * 指定したプレイヤーのチーム色を取得する。
     * チームが存在しない場合は白を返す。
     *
     * @param player プレイヤー
     * @return 色
     */
    public static NamedTextColor getTeamColor(@NotNull Player player) {
        return MatchManager.getInstance().getJoinedMatch(player)
                .stream().filter(match -> match instanceof TeamBaseMatch).findFirst()
                .map(match ->
                        ((TeamBaseMatch) match).getTeamByPlayer(player).map(MatchTeam::getColor).orElse(NamedTextColor.WHITE))
                .orElse(NamedTextColor.WHITE);
    }

    @Override
    protected Optional<MapMarker> getSpawnMaker(@NotNull MatchMapWorld matchMapWorld, @NotNull Player player) {
        return getTeamByPlayer(player).flatMap(matchTeam -> matchMapWorld.getMakerRandom(matchTeam.getRespawnPoint()));
    }

    @Override
    protected boolean canMaintainJoinPlayer(@NotNull Player player) {
        if (!super.canMaintainJoinPlayer(player)) {
            return false;
        }

        // 試合開始後、どれかしらのチームに所属していない場合は維持不可
        return getStatus() == MatchStatus.NONE || teams.stream()
                .anyMatch(team -> team.hasParticipation(player));
    }

    /**
     * 試合チーム
     *
     * @author MORIMORI0317
     */
    public class MatchTeam {

        /**
         * チーム名
         */
        @NotNull
        private final String name;

        /**
         * チームカラー
         */
        @NotNull
        private final NamedTextColor color;

        /**
         * リスポーン地点のマーカー
         */
        @NotNull
        private final NamespacedKey respawnPoint;

        /**
         * 参加しているプレイヤー
         */
        private final List<Player> participationPlayers = new ArrayList<>();

        /**
         * コンストラクタ
         *
         * @param name              チーム名
         * @param color             チームカラー
         * @param respawnPointMaker リスポーン地点
         */
        public MatchTeam(@NotNull String name, @NotNull NamedTextColor color, @NotNull NamespacedKey respawnPointMaker) {
            this.name = name;
            this.color = color;
            this.respawnPoint = respawnPointMaker;
        }

        @NotNull
        public NamedTextColor getColor() {
            return color;
        }

        @NotNull
        public NamespacedKey getRespawnPoint() {
            return respawnPoint;
        }

        @Unmodifiable
        public List<Player> getParticipationPlayers() {
            return ImmutableList.copyOf(participationPlayers);
        }

        /**
         * 指定しているプレイヤーがチームに参加しているかどうか
         *
         * @param player プレイヤー
         * @return 参加していればtrue、していなければfalse
         */
        public boolean hasParticipation(@NotNull Player player) {
            return this.participationPlayers.contains(player);
        }

        /**
         * 指定したプレイヤーをチームに追加する
         *
         * @param player プレイヤー
         */
        public void addPlayer(@NotNull Player player) {
            Objects.requireNonNull(player);

            if (!this.participationPlayers.contains(player)) {
                this.participationPlayers.add(player);
            }

            // 情報表示を更新
            getTeamPlayerInfo(player).ifPresent(PlayerInfo::dirtyInfo);
        }

        /**
         * 指定したプレイヤーをチームから削除する
         *
         * @param player プレイヤー
         */
        public void removePlayer(@NotNull Player player) {
            this.participationPlayers.remove(player);
        }

        @NotNull
        public String getName() {
            return name;
        }


        /**
         * チームのメンバーオーディエンスを取得
         *
         * @return オーディエンス
         */
        public Audience audience() {
            return Audience.audience(this.participationPlayers);
        }
    }

    /**
     * チーム試合のプレイヤー情報
     *
     * @author MORIMORI0317
     */
    public class TeamPlayerInfo extends PlayerInfo {
        /**
         * コンストラクタ
         *
         * @param player プレイヤー
         */
        public TeamPlayerInfo(@NotNull Player player) {
            super(player);

            // スコアボード表示用チームを登録
            for (int i = 0; i < TeamBaseMatch.this.teams.size(); i++) {
                MatchTeam matchTeam = TeamBaseMatch.this.teams.get(i);
                Team sbTeam = getScoreboard().registerNewTeam("team-" + i);
                sbTeam.setAllowFriendlyFire(false);
                sbTeam.setCanSeeFriendlyInvisibles(true);
                sbTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
                sbTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                sbTeam.color(matchTeam.getColor());
            }
        }

        @Override
        protected void updateInfo(Consumer<List<Component>> sideBarMatchInfoAppender) {
            super.updateInfo(sideBarMatchInfoAppender);

            Set<Team> scTeams = getScoreboard().getTeams();
            scTeams.forEach(team -> team.removeEntries(team.getEntries()));

            for (int i = 0; i < TeamBaseMatch.this.teams.size(); i++) {
                MatchTeam matchTeam = TeamBaseMatch.this.teams.get(i);
                Team sbTeam = getScoreboard().getTeam("team-" + i);

                if (sbTeam != null) {
                    List<Player> teamPlayers = matchTeam.getParticipationPlayers();
                    teamPlayers.forEach(sbTeam::addPlayer);
                }
            }

        }

        @Override
        protected void appendSidebarPlayerInfo(@NotNull List<Component> sidebarInfos) {
            appendSidebarTeamInfo(sidebarInfos);
            super.appendSidebarPlayerInfo(sidebarInfos);
        }

        /**
         * チーム関係の情報をサイドバーに追加
         *
         * @param sidebarInfos サイドバー情報の文字列リスト
         */
        protected void appendSidebarTeamInfo(@NotNull List<Component> sidebarInfos) {
            getTeamByPlayer(getPlayer()).ifPresent(team -> {
                Component teamComponent = Component.text(team.getName()).color(team.getColor());

                sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-team")).color(NamedTextColor.GREEN)
                        .append(teamComponent));
            });
        }
    }
}
