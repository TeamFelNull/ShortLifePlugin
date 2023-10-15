package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector3;
import dev.felnull.shortlifeplugin.match.map.*;
import dev.felnull.shortlifeplugin.utils.MatchUtils;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 試合インスタンスクラス
 *
 * @author MORIMORI0317
 */
public abstract class Match {

    /**
     * 試合が開始するまでの時間(ms)
     */
    private static final long START_WAIT_TIME = 1000 * 30;

    /**
     * 試合終了後にテレポートするまでの時間
     */
    private static final long FINISH_WAIT_FOR_TELEPORT = 1000 * 10;

    /**
     * 試合終了から破棄されるまでの時間(ms)
     */
    private static final long DISPOSE_WAIT_TIME = FINISH_WAIT_FOR_TELEPORT + (1000 * 5);

    /**
     * 試合参加時のメッセージ
     */
    private static final Component MATCH_JOIN_MESSAGE = Component.text("試合に参加しました").color(NamedTextColor.WHITE);

    /**
     * 試合退出時のメッセージ
     */
    private static final Component MATCH_LEAVE_MESSAGE = Component.text("試合から退出しました").color(NamedTextColor.WHITE);

    /**
     * 試合参加時、参加者以外に送信するメッセージ
     */
    private static final Function<Player, Component> MATCH_JOIN_BROADCAST_MESSAGE =
            player -> Component.text(String.format("%sが試合に参加しました", player.getName())).color(NamedTextColor.WHITE);

    /**
     * 試合退出時、参加者以外に送信するメッセージ
     */
    private static final Function<Player, Component> MATCH_LEAVE_BROADCAST_MESSAGE =
            player -> Component.text(String.format("%sが試合から退出しました", player.getName())).color(NamedTextColor.WHITE);

    /**
     * 試合開始時のメッセージ
     */
    private static final Component MATCH_START_MESSAGE = Component.text("試合開始...").color(NamedTextColor.GREEN);

    /**
     * 試合終了時のメッセージ
     */
    private static final Component MATCH_FINISH_MESSAGE = Component.text("試合終了...").color(NamedTextColor.GREEN);

    /**
     * 開始時のワールド読み込み待ちメッセージ
     */
    private static final Component MATCH_WAIT_LOAD_WORLD_MESSAGE = Component.text("マップの準備が終わるまでお待ちください").color(NamedTextColor.BLUE);

    /**
     * ワールド読み込み失敗で試合を中止する際のメッセージ
     */
    private static final Component MATCH_CANCEL_FAILED_LOAD_WORLD_MESSAGE = Component.text("マップの読み込みに失敗したため試合を中止します").color(NamedTextColor.DARK_RED);

    /**
     * 想定外のエラーが原因で試合を中止する際のメッセージ
     */
    private static final Component MATCH_CANCEL_UNEXPECTED_ERROR_MESSAGE = Component.text("想定外のエラーが発生したため試合を中止します").color(NamedTextColor.DARK_RED).appendNewline()
            .append(Component.text("この事を運営に報告してください！").color(NamedTextColor.GOLD));


    /**
     * 人不足で試合を終了する際のメッセージ
     */
    private static final Component MATCH_FINISH_INSUFFICIENT_PLAYER_MESSAGE = Component.text("参加人数が不足したため試合を終了します").color(NamedTextColor.RED);


    /**
     * 参加しているプレイヤー
     */
    protected final List<Player> players = new ArrayList<>();

    /**
     * 同じ状態の経過Tick
     */
    protected int statusTick;

    /**
     * 試合のID
     */
    @NotNull
    private final String id;

    /**
     * 試合モード
     */
    @NotNull
    private final MatchMode matchMode;

    /**
     * 試合用マップ
     */
    @NotNull
    private final MatchMap matchMap;

    /**
     * 状態
     */
    @NotNull
    private Status status = Status.NONE;

    /**
     * 破棄するためのフラグ
     */
    private boolean destroyed = false;

    /**
     * 試合用マップ
     */
    private MatchMapInstance matchMapInstance;

    /**
     * マップの読み込まれ待ちを通知したかどうか
     */
    private boolean waitLoadMapNotified;


    /**
     * 試合終了後のテレポートを行ったかどうか
     */
    private boolean finishTeleport;

    /**
     * コンストラクタ
     *
     * @param id        試合ID
     * @param matchMode 試合モード
     * @param matchMap  試合用マップ
     */
    protected Match(@NotNull String id, @NotNull MatchMode matchMode, @NotNull MatchMap matchMap) {
        this.id = id;
        this.matchMode = matchMode;
        this.matchMap = matchMap;
    }

    /**
     * 初期化処理
     */
    protected void init() {
        this.matchMapInstance = MatchUtils.getMatchManager().getMapLoader().createMapInstance(this.id, this.matchMap);

        SLUtils.getLogger().info(String.format("試合(%s)が作成されました", getId()));
    }

    /**
     * 1Tickごとの処理
     */
    protected final void tick() {

        // 状態が破棄ならばTick処理を行わない
        if (getStatus() == Status.DISCARDED) {
            return;
        }

        // 参加を維持できないプレイヤーを退出させる
        List<Player> leavePlayers = players.stream()
                .filter(pl -> !canMaintainJoinPlayer(pl))
                .toList();
        leavePlayers.forEach(player -> leave(player, true));


        // 試合参加者0人になれば試合破棄
        if (this.players.isEmpty()) {
            destroy();
            return;
        }

        baseTick();

        this.statusTick++;

        // 状態ごとの処理
        switch (getStatus()) {
            case NONE -> beforeStartTick();
            case STARTED -> duringMatchTick();
            case FINISHED -> afterFinishTick();
            default -> {
            }
        }
    }

    /**
     * 試合開始前のTick処理
     */
    protected void beforeStartTick() {

        // マップの読み込みに失敗した場合は試合破棄
        if (this.matchMapInstance.isLoadFailed()) {
            broadcast(MATCH_CANCEL_FAILED_LOAD_WORLD_MESSAGE);

            Optional<Throwable> mapWorldError = this.matchMapInstance.getMapWordLoadError();

            if (mapWorldError.isPresent()) {
                SLUtils.reportError(mapWorldError.get(), "試合用マップの読み込みに失敗");
            } else {
                SLUtils.reportError(new RuntimeException("試合用マップの読み込みに失敗"));
            }

            destroy();
            return;
        }

        boolean start = false;

        if (this.statusTick >= SLUtils.toTick(TimeUnit.MILLISECONDS, START_WAIT_TIME)) {
            // 開始待機時間が過ぎた場合の処理
            if (matchMapInstance.isReady()) {
                // マップの準備が終わっていれば試合開始
                start = true;
            } else if (!waitLoadMapNotified) {
                // マップの準備が終わっていなければ、1度だけ通知
                waitLoadMapNotified = true;
                broadcast(MATCH_WAIT_LOAD_WORLD_MESSAGE);
            }
        }

        if (start && !start()) {
            SLUtils.reportError(new RuntimeException("試合を開始できませんでした。"));
            destroy();
        }
    }

    /**
     * 試合中のTick処理
     */
    protected void duringMatchTick() {

        boolean finish = false;

        if (players.size() < getMatchMode().minPlayerCount()) {
            // 参加プレイヤーの人数が、最低参加人数より少なければ試合を終了
            broadcast(MATCH_FINISH_INSUFFICIENT_PLAYER_MESSAGE);
            finish = true;
        } else if (this.statusTick >= SLUtils.toTick(TimeUnit.MILLISECONDS, getMatchMode().limitTime())) {
            // 試合制限時間を過ぎた場合、試合を終了
            finish = true;
        }

        if (finish && !finish()) {
            SLUtils.reportError(new RuntimeException("試合を終了できませんでした。"));
            destroy();
        }
    }

    /**
     * 試合終了後のTick処理
     */
    protected void afterFinishTick() {

        // 終了後のテレポート
        if (!finishTeleport && this.statusTick >= SLUtils.toTick(TimeUnit.MILLISECONDS, FINISH_WAIT_FOR_TELEPORT)) {
            finishTeleport = true;

            for (final Player player : players) {
                MatchUtils.teleportToLeave(player, this.matchMapInstance.getMapWorld()
                        .map(MatchMapWorld::getWorld)
                        .orElse(null));
            }
        }

        // 破棄されるまでしばらく待機
        if (this.statusTick >= SLUtils.toTick(TimeUnit.MILLISECONDS, DISPOSE_WAIT_TIME)) {
            destroy();
        }
    }

    /**
     * 試合ごとのTick処理
     */
    protected void baseTick() {
    }

    /**
     * 試合への参加を維持できるかどうか
     *
     * @param player プレイヤー
     * @return 維持できるかどうか
     */
    protected boolean canMaintainJoinPlayer(@NotNull Player player) {
        Objects.requireNonNull(player);

        // このプレイヤーインスタンスが有効ではない場合は維持不可
        if (!player.isConnected() || !player.isOnline()) {
            return false;
        }

        if (this.status != Status.NONE) {
            Optional<MatchMapWorld> matchMapWorld = this.matchMapInstance.getMapWorld();

            if (matchMapWorld.isPresent() && matchMapWorld.get().getWorld() != player.getWorld()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 指定のプレイヤーを試合へ参加させる
     *
     * @param player      プレイヤー
     * @param sendMessage 参加メッセージを本人に送信するかどうか
     * @return 参加できたかどうか
     */
    public boolean join(@NotNull Player player, boolean sendMessage) {
        Objects.requireNonNull(player);

        // 最大参加人数を超えるか確認
        if (players.size() >= matchMode.maxPlayerCount()) {
            return false;
        }

        // すでにどれかしらの試合に参加しているプレイヤーは参加不可
        if (MatchUtils.getMatchManager().getJointedMach(player) != null) {
            return false;
        }

        // 開始前、試合中以外は参加不可
        if (status != Status.NONE && status != Status.STARTED) {
            return false;
        }


        // 既に試合が開始されていれば、試合用マップへ移動
        if (status == Status.STARTED && matchMapInstance.isReady()) {
            Location respawnLocation = lotterySpawnLocation(player);
            if (respawnLocation != null) {
                player.teleport(respawnLocation);
            } else {
                SLUtils.reportError(new RuntimeException("リスポーン地点を取得できませんでした"));
                return false;
            }
        }


        this.players.add(player);

        if (sendMessage) {
            player.sendMessage(MATCH_JOIN_MESSAGE);
        }

        broadcast(MATCH_JOIN_BROADCAST_MESSAGE.apply(player), pl -> pl != player);

        SLUtils.getLogger().info(String.format("%sが試合(%s)に参加しました", player.getName(), getId()));

        return true;
    }

    /**
     * 指定のプレイヤーを試合から退出させる
     *
     * @param player      プレイヤー
     * @param sendMessage 退出メッセージを送信するかどうか
     * @return 退出できたかどうか
     */
    public boolean leave(@NotNull Player player, boolean sendMessage) {
        Objects.requireNonNull(player);

        // 参加していないプレイヤーは退出不可
        if (!this.players.contains(player)) {
            return false;
        }

        this.players.remove(player);

        // 試合用ワールドにいる場合、ワールド外にテレポート
        Optional<MatchMapWorld> matchMapWorld = this.matchMapInstance.getMapWorld();
        if (matchMapWorld.isPresent() && matchMapWorld.get().getWorld() == player.getWorld()) {
            MatchUtils.teleportToLeave(player, matchMapWorld
                    .map(MatchMapWorld::getWorld)
                    .orElse(null));
        }

        if (sendMessage) {
            player.sendMessage(MATCH_LEAVE_MESSAGE);
            broadcast(MATCH_LEAVE_BROADCAST_MESSAGE.apply(player), pl -> pl != player);
        }

        SLUtils.getLogger().info(String.format("%sが試合(%s)から退出しました", player.getName(), getId()));
        return true;
    }

    /**
     * 現在参加している全てのプレイヤー
     *
     * @return 参加しているプレイヤーの不変リスト
     */
    @Unmodifiable
    @NotNull
    public List<Player> getAllJoinPlayers() {
        return ImmutableList.copyOf(players);
    }

    /**
     * 試合を開始
     *
     * @return 開始できたかどうか
     */
    public boolean start() {

        // 試合開始前状態以外は開始不可
        if (getStatus() != Status.NONE) {
            return false;
        }

        // マップの準備中、もしくは読み込み失敗の場合は開始不可
        if (!this.matchMapInstance.isReady() || this.matchMapInstance.isLoadFailed()) {
            return false;
        }

        // 参加中のプレイヤー全員を試合用マップへテレポート
        if (this.matchMapInstance.getMapWorld().isPresent()) {
            for (Player player : this.players) {
                Location respawnPoint = lotterySpawnLocation(player);
                if (respawnPoint != null) {
                    player.teleport(respawnPoint);
                }
            }
        } else {
            SLUtils.reportError(new RuntimeException("ワールドの取得に失敗"));
            return false;
        }

        changeStatus(Status.STARTED);

        broadcast(MATCH_START_MESSAGE);

        SLUtils.getLogger().info(String.format("試合(%s)が開始しました", getId()));
        return true;
    }

    /**
     * 試合を終了
     *
     * @return 終了できたかどうか
     */
    public boolean finish() {

        // 試合中以外は終了不可
        if (getStatus() != Status.STARTED) {
            return false;
        }

        changeStatus(Status.FINISHED);

        broadcast(MATCH_FINISH_MESSAGE);

        SLUtils.getLogger().info(String.format("試合(%s)が終了しました", getId()));
        return true;
    }

    /**
     * 試合を破棄するフラグを立てる
     */
    public void destroy() {
        // お兄さん許して、試合が壊れるなんて嫌よ
        // こわれちゃ＾～う

        this.destroyed = true;
    }

    /**
     * 破棄時の処理
     */
    protected void dispose() {
        changeStatus(Status.DISCARDED);

        List<Player> leavePlayers = ImmutableList.copyOf(this.players);
        leavePlayers.forEach(player -> leave(player, false));

        if (this.matchMapInstance != null) {
            this.matchMapInstance.dispose();
        }

        SLUtils.getLogger().info(String.format("試合(%s)が破棄されました", getId()));
    }

    /**
     * 予期せぬエラーが起きた時の処理
     */
    protected void unexpectedError() {
        broadcast(MATCH_CANCEL_UNEXPECTED_ERROR_MESSAGE);
    }

    /**
     * このフラグを確認して、試合マネージャーが試合の破棄を行う
     *
     * @return 破棄されるべきかどうか
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public MatchMode getMatchMode() {
        return matchMode;
    }

    @NotNull
    public Status getStatus() {
        return status;
    }

    @NotNull
    public MatchMap getMatchMap() {
        return matchMap;
    }

    private void changeStatus(@NotNull Status status) {

        // 現在のステータスと変わらないなら変更なし
        if (this.status == status) {
            return;
        }

        this.status = status;
        this.statusTick = 0;
    }

    /**
     * リストに情報説明の追加を行う
     *
     * @param componentList 情報説明コンポーネントのリスト
     */
    public void appendInfoDesc(@NotNull List<Component> componentList) {
        componentList.add(Component.text("モード: ").append(matchMode.name()));
        componentList.add(Component.text("参加人数: ").append(Component.text(String.format("%d/%d", players.size(), matchMode.maxPlayerCount()))));
        componentList.add(Component.text("状態: ").append(getStatus().getName()));
    }

    /**
     * 参加中のプレイヤー全てにメッセージを送信
     *
     * @param component メッセージ
     */
    public void broadcast(@NotNull Component component) {
        allPlayerAudience().sendMessage(component);
    }

    /**
     * 一部のプレイヤー以外にメッセージを送信
     *
     * @param component                メッセージ
     * @param exclusionPlayerPredicate プレイヤーフィルター
     */
    public void broadcast(@NotNull Component component, @NotNull Predicate<Player> exclusionPlayerPredicate) {
        Audience.audience(players.stream()
                .filter(exclusionPlayerPredicate)
                .toList()).sendMessage(component);
    }


    /**
     * この試合に参加している全てのプレイヤーに向けたオーディエンスを取得
     *
     * @return オーディエンス
     */
    public Audience allPlayerAudience() {
        return Audience.audience(players);
    }

    /**
     * プレイヤーのスポーン地点を抽選<br/>
     * スポーン地点を定義していない場合はnullを返す
     *
     * @param player プレイヤー
     * @return 場所
     */
    @Nullable
    public Location lotterySpawnLocation(@NotNull Player player) {

        // マップが読み込み済みならばスポーン地点を抽選する
        if (this.matchMapInstance.getMapWorld().isPresent()) {
            MatchMapWorld matchMapWorld = this.matchMapInstance.getMapWorld().get();
            World world = matchMapWorld.getWorld();
            MapMarker spawnMarker = matchMapWorld.getMakerRandom(MapMarkerPoints.SPAWN);

            // スポーン地点のマーカーを取得できない場合はnullを返す
            if (spawnMarker == null) {
                return null;
            }

            BlockVector3 spawnPos = matchMapWorld.offsetCorrection(spawnMarker.getPosition());

            Location location = new Location(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
            location.setDirection(spawnMarker.getDirection().getDirection());

            return location;
        }

        return null;
    }

    /**
     * 指定されたプレイヤーが無敵かどうか
     *
     * @param player プレイヤー
     * @return 無敵かどうか
     */
    public boolean isInvinciblePlayer(@NotNull Player player) {
        if (status == Status.FINISHED) {
            Optional<MatchMapWorld> matchMapWorld = matchMapInstance.getMapWorld();
            return matchMapWorld.isPresent() && matchMapWorld.get().getWorld() == player.getWorld();
        }
        return false;
    }

    public MatchMapInstance getMatchMapInstance() {
        return matchMapInstance;
    }

    /**
     * 試合の状態
     *
     * @author MORIMORI0317
     */
    public enum Status {
        /**
         * 開始前
         */
        NONE(Component.text("無し")),
        /**
         * 開始
         */
        STARTED(Component.text("開始")),
        /**
         * 終了
         */
        FINISHED(Component.text("終了")),

        /**
         * 破棄済み
         */
        DISCARDED(Component.text("破棄"));

        /**
         * 状態名
         */
        private final Component name;

        /**
         * コンストラクタ
         *
         * @param name 状態名
         */
        Status(Component name) {
            this.name = name;
        }

        public Component getName() {
            return name;
        }
    }
}
