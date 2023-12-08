package dev.felnull.shortlifeplugin.match;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.utils.SLFiles;
import dev.felnull.shortlifeplugin.utils.SLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static dev.felnull.shortlifeplugin.match.Match.SPAWN_INVINCIBILITY_TIME;
import static dev.felnull.shortlifeplugin.match.MatchStatus.STARTED;
import static org.bukkit.Sound.ENTITY_ENDER_DRAGON_AMBIENT;
import static org.bukkit.Sound.ENTITY_WITHER_DEATH;

/**
 * プレイヤーごとの情報
 *
 * @author MORIMORI0317
 */
public class PlayerInfo {

    /**
     * ランダム
     */
    private static final Random RANDOM = new Random();

    /**
     * ノーマル
     */
    private static final String NORMAL = "normal";

    /**
     * ボーナス
     */
    private static final String BONUS = "ボーナス";

    /**
     * 特殊
     */
    private static final String SPECIAL = "special";

    /**
     * 勝利
     */
    private static final String WINNER = "winner";

    /**
     * ストリーク
     */
    private static final String STREAK = "streak";

    /**
     * Gsonインスタンス
     */
    private static final Gson GSON = new Gson();

    /**
     * この情報のプレイヤー
     */
    @NotNull
    private final Player player;

    /**
     * プレイヤーごとの試合用スコアボード<br/>
     */
    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    /**
     * 情報表示用サイドバーのディスプレイ
     */
    @NotNull
    private final SidebarDisplay infoSidebarDisplay;

    /**
     * 試合用スコアボードにセットされる前のスコアボード
     */
    private Scoreboard preScoreboard;

    /**
     * 試合開始前のゲームモード
     */
    private GameMode preGameMode;

    /**
     * キル数
     */
    private int killCount;

    /**
     * 連続キル数
     */
    private int killStreakCount;

    /**
     * 最大連続キル数
     */
    private int maxKillStreakCount;

    /**
     * 死亡数
     */
    private int deathCount;

    /**
     * 情報表示の更新が必要であるフラグ
     */
    private boolean dirtyInfo;

    /**
     * 生存時間 (Tick)
     */
    private int lifeTime;

    /**
     * スポーン保護中の残り時間 (Tick)<br/>
     * -1以下で保護なし
     */
    private int spawnProtectTime = -1;

    /**
     * 報酬ボーナスフラグ
     */
    private boolean bonusFlag = false;

    /**
     * チャンスフラグ
     */
    private boolean chanceFlag = false;

    /**
     * チャンス時のキル数
     */
    private int killChanceCount = 0;

    /**
     * チャンス確率
     */
    private int luckyProbability = 5;

    /**
     * コンストラクタ
     *
     * @param player プレイヤー
     */
    protected PlayerInfo(@NotNull Player player) {
        this.player = player;
        Objective sidebarObjective = scoreboard.registerNewObjective("sidebar-info", Criteria.DUMMY,
                Component.text(MsgHandler.get("match-sidebar-title")).style(Style.style().color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD).build()));
        sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.infoSidebarDisplay = new SidebarDisplay(sidebarObjective);
    }

    /**
     * tick処理
     *
     * @param status マッチの状態
     * @param dirtyAllInfo 全てのサイドバー情報更新フラグ
     * @param sideBarMatchInfoAppender サイドバーの試合情報追加関数
     */
    protected void tick(MatchStatus status, boolean dirtyAllInfo, Consumer<List<Component>> sideBarMatchInfoAppender) {
        lifeTime++;

        // スポーン保護状態を更新
        if (status == STARTED) {
            if (spawnProtectTime >= 0) {
                spawnProtectTime--;

                float timePar = (float) spawnProtectTime / (float) SLUtils.toTick(TimeUnit.MILLISECONDS, SPAWN_INVINCIBILITY_TIME);

                player.getWorld()
                        .spawnParticle(Particle.COMPOSTER, player.getLocation().clone().add(0, 1, 0), (int) (15f * timePar), 0.3f, 0.3f, 0.3f);

            }
        } else {
            spawnProtectTime = -1;
        }

        if (dirtyAllInfo) {
            updateCheckAndInfo(sideBarMatchInfoAppender);
        }
    }

    /**
     * サイドバー情報の確認と更新
     *
     * @param sideBarMatchInfoAppender サイドバーの試合情報追加関数
     */
    public void updateCheckAndInfo(Consumer<List<Component>> sideBarMatchInfoAppender) {
        // 表示更新フラグの確認とリセット
        if (!(this.dirtyInfo)) {
            return;
        }
        this.dirtyInfo = false;

        updateInfo(sideBarMatchInfoAppender);
    }

    /**
     * 表示情報を更新
     *
     * @param sideBarMatchInfoAppender サイドバーの試合情報追加関数
     */
    protected void updateInfo(Consumer<List<Component>> sideBarMatchInfoAppender) {
        // サイドバーを更新
        List<Component> sidebarInfo = new LinkedList<>();
        sideBarMatchInfoAppender.accept(sidebarInfo);
        sidebarInfo.add(Component.text(""));
        appendSidebarPlayerInfo(sidebarInfo);
        appendSidebarAdditionalInfo(sidebarInfo);
        
        this.infoSidebarDisplay.update(sidebarInfo);
    }

    /**
     * 派生クラスがサイドバー情報を追加するためのオーバーライド用(よくない実装)
     *
     * @param sidebarInfos 情報追加用リスト
     */
    protected void appendSidebarAdditionalInfo(@NotNull List<Component> sidebarInfos) {
        
    }

    /**
     * プレイヤー関係の情報をサイドバーに追加
     *
     * @param sidebarInfos サイドバー情報のコンポーネントリスト
     */
    protected void appendSidebarPlayerInfo(@NotNull List<Component> sidebarInfos) {

        Style.Builder killCountStyle = Style.style();
        // キル数文字スタイル選定
        if (this.killCount >= 100) {
            killCountStyle.color(NamedTextColor.BLACK).decorate(TextDecoration.BOLD, TextDecoration.ITALIC);
        } else if (this.killCount >= 50) {
            killCountStyle.color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD);
        } else if (this.killCount >= 35) {
            killCountStyle.color(NamedTextColor.RED);
        } else if (this.killCount >= 15) {
            killCountStyle.color(NamedTextColor.GOLD);
        } else if (this.killCount >= 5) {
            killCountStyle.color(NamedTextColor.YELLOW);
        } else {
            killCountStyle.color(NamedTextColor.WHITE);
        }

        sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-kills")).color(NamedTextColor.GREEN)
                .append(Component.text(this.killCount).style(killCountStyle.build())));


        Style.Builder killStreakCountStyle = Style.style();
        // 連続キル数文字スタイル選定
        if (this.killStreakCount >= 50) {
            killStreakCountStyle.color(NamedTextColor.BLACK).decorate(TextDecoration.BOLD, TextDecoration.ITALIC);
        } else if (this.killStreakCount >= 30) {
            killStreakCountStyle.color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD);
        } else if (this.killStreakCount >= 15) {
            killStreakCountStyle.color(NamedTextColor.RED);
        } else if (this.killStreakCount >= 8) {
            killStreakCountStyle.color(NamedTextColor.GOLD);
        } else if (this.killStreakCount >= 3) {
            killStreakCountStyle.color(NamedTextColor.YELLOW);
        } else {
            killStreakCountStyle.color(NamedTextColor.WHITE);
        }

        Component killStreakCountComponent = Component.text(MsgHandler.get("match-sidebar-kill-streak")).color(NamedTextColor.GREEN)
                .append(Component.text(this.killStreakCount).style(killStreakCountStyle.build()));

        // 最大連続キル数が連続キル数を超えている場合のみ
        if (this.maxKillStreakCount > this.killStreakCount) {

            Style.Builder maxKillStreakCountStyle = Style.style();
            // 最大連続キル数文字スタイル選定
            if (this.maxKillStreakCount >= 50) {
                maxKillStreakCountStyle.color(NamedTextColor.BLACK).decorate(TextDecoration.BOLD, TextDecoration.ITALIC);
            } else if (this.maxKillStreakCount >= 30) {
                maxKillStreakCountStyle.color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD);
            } else if (this.maxKillStreakCount >= 15) {
                maxKillStreakCountStyle.color(NamedTextColor.RED);
            } else if (this.maxKillStreakCount >= 8) {
                maxKillStreakCountStyle.color(NamedTextColor.GOLD);
            } else if (this.maxKillStreakCount >= 3) {
                maxKillStreakCountStyle.color(NamedTextColor.YELLOW);
            } else {
                maxKillStreakCountStyle.color(NamedTextColor.WHITE);
            }

            killStreakCountComponent = killStreakCountComponent.append(Component.text(MsgHandler.get("match-sidebar-kill-streak-max-1")).color(NamedTextColor.GREEN)
                    .append(Component.text(this.maxKillStreakCount).style(maxKillStreakCountStyle.build()))
                    .append(Component.text(MsgHandler.get("match-sidebar-kill-streak-max-2")).color(NamedTextColor.GREEN)));
        }

        sidebarInfos.add(killStreakCountComponent);


        Style.Builder deathCountStyle = Style.style();
        // 死亡数文字スタイル選定
        if (this.deathCount >= 100) {
            deathCountStyle.color(NamedTextColor.BLACK).decorate(TextDecoration.BOLD, TextDecoration.ITALIC);
        } else if (this.deathCount >= 66) {
            deathCountStyle.color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD);
        } else if (this.deathCount >= 42) {
            deathCountStyle.color(NamedTextColor.RED);
        } else if (this.deathCount >= 13) {
            deathCountStyle.color(NamedTextColor.GOLD);
        } else if (this.deathCount >= 4) {
            deathCountStyle.color(NamedTextColor.YELLOW);
        } else {
            deathCountStyle.color(NamedTextColor.WHITE);
        }

        sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-deaths")).color(NamedTextColor.GREEN)
                .append(Component.text(this.deathCount).style(deathCountStyle.build())));

        if (bonusFlag) {
            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-separator-1")).color(NamedTextColor.AQUA));
            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-stats")).color(NamedTextColor.WHITE)
                    .append(Component.text(MsgHandler.get("match-sidebar-bonus")).color(NamedTextColor.AQUA)));
            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-separator-1")).color(NamedTextColor.AQUA));
        } else if (chanceFlag) {
            //空白を入れることで同じ文字をスコアボードで扱えます
            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-separator-1")).color(NamedTextColor.GOLD).decorate(TextDecoration.OBFUSCATED));
            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-stats")).color(NamedTextColor.WHITE)
                    .append(Component.text(MsgHandler.get("match-sidebar-chance")).color(NamedTextColor.GOLD)));
            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-separator-1")).color(NamedTextColor.GOLD).decorate(TextDecoration.OBFUSCATED));
        } else {
            sidebarInfos.add(Component.text(MsgHandler.get("match-sidebar-stats")).color(NamedTextColor.WHITE)
                    .append(Component.text(MsgHandler.get("match-sidebar-common"))).color(NamedTextColor.GRAY));
        }
    }

    public Scoreboard getPreScoreboard() {
        return preScoreboard;
    }

    public void setPreScoreboard(Scoreboard preScoreboard) {
        this.preScoreboard = preScoreboard;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public int getDeathCount() {
        return deathCount;
    }

    /**
     * 死亡数セットと表示更新フラグを立てる
     *
     * @param deathCount 死亡数
     */
    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
        dirtyInfo();
    }

    public int getKillCount() {
        return killCount;
    }

    /**
     * キル数セットと表示更新フラグを立てる
     *
     * @param killCount キル数
     */
    public void setKillCount(int killCount) {
        this.killCount = killCount;
        dirtyInfo();
    }

    public int getKillStreakCount() {
        return killStreakCount;
    }

    /**
     * 連続キル数セットと表示更新フラグを立てる
     *
     * @param killStreakCount 連続キル数
     */
    public void setKillStreakCount(int killStreakCount) {
        this.killStreakCount = killStreakCount;
        dirtyInfo();
    }

    public int getMaxKillStreakCount() {
        return maxKillStreakCount;
    }

    /**
     * 最大連続キル数セットと表示更新フラグを立てる
     *
     * @param maxKillStreakCount 最大連続キル数
     */
    public void setMaxKillStreakCount(int maxKillStreakCount) {
        this.maxKillStreakCount = maxKillStreakCount;
        dirtyInfo();
    }

    /**
     * 情報表示を更新するフラグを立てる
     */
    protected void dirtyInfo() {
        this.dirtyInfo = true;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    @SuppressWarnings("unused")
    public int getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    public GameMode getPreGameMode() {
        return preGameMode;
    }

    public void setPreGameMode(GameMode preGameMode) {
        this.preGameMode = preGameMode;
    }

    public void setSpawnProtectTime(int spawnProtectTime) {
        this.spawnProtectTime = spawnProtectTime;
    }

    public boolean isSpawnProtect() {
        return spawnProtectTime >= 0;
    }

    /**
     * 報酬を付与
     *
     * @author raindazo
     */
    public void giveReward() {
        PotionEffect luckEffect = player.getPotionEffect(PotionEffectType.LUCK);

        if (luckEffect != null) {
            luckyProbability += luckEffect.getAmplifier();
        }

        double streak = getKillStreakCount() % 5d;
        boolean bonus = RANDOM.nextInt(100) < luckyProbability;
        boolean chance = RANDOM.nextInt(100) < 1; //チャンス確率

        if (bonusFlag && !chanceFlag && chance) {
            chanceFlag = true;
            killChanceCount = getKillCount();
            player.sendMessage(Component.text(MsgHandler.get("match-sidebar-separator-2")).color(NamedTextColor.GOLD).decorate(TextDecoration.OBFUSCATED));
            player.sendMessage(Component.text(MsgHandler.get("match-sidebar-chance-phase-1")).color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text(MsgHandler.get("match-sidebar-chance-phase-2")).color(NamedTextColor.WHITE));
            player.sendMessage(Component.text(MsgHandler.get("match-sidebar-chance-phase-3")).color(NamedTextColor.WHITE));
            player.sendMessage(Component.text(MsgHandler.get("match-sidebar-chance-phase-4")).color(NamedTextColor.WHITE));
            player.sendMessage(Component.text(MsgHandler.get("match-sidebar-separator-2")).color(NamedTextColor.GOLD).decorate(TextDecoration.OBFUSCATED));
            player.playSound(player, ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.8f);
        }

        if (chanceFlag && getKillCount() == killChanceCount + 5) {
            runCommand(SPECIAL);
            player.playSound(player, ENTITY_WITHER_DEATH, 0.5f, 0.8f);
            chanceFlag = false;
        }

        if (streak == 0) {
            runCommand(STREAK);
        }

        if (bonusFlag || bonus) {
            runCommand(BONUS);
            bonusFlag = true;
        } else {
            runCommand(NORMAL);
        }
    }

    /**
     * 報酬情報を初期化
     *
     * @author raindazo
     */
    public void initRewardFlag() {
        bonusFlag = false;
        chanceFlag = false;
    }

    /**
     * 　報酬コマンドを取得する
     *
     * @param functionName 報酬名
     * @return JSON保存されているコマンド
     * @author raindazo
     **/
    protected String getRewardCommand(String functionName) throws IOException {
        File loadJsonFile = SLFiles.rewardCommandConfigJson();

        if (!loadJsonFile.exists() || loadJsonFile.isDirectory()) {
            return "";
        }

        JsonObject json = GSON.fromJson(Files.readString(loadJsonFile.toPath()), JsonObject.class);

        return switch (functionName) {
            case "normal" -> json.get("normalReward").getAsString();
            case "special" -> json.get("specialReward").getAsString();
            case "winner" -> json.get("winnerReward").getAsString();
            case "streak" -> json.get("streakReward").getAsString();
            default -> throw new IllegalStateException(MsgHandler.get("system-unexpected-value") + functionName);
        };
    }

    /**
     * JSONに保存されているコマンドを実行する
     *
     * @param functionName 報酬名
     * @author raindazo
     */
    protected void runCommand(String functionName) {
        try {
            switch (functionName) {
                case NORMAL -> {
                    List<String> normalCommandList = Arrays.asList(getRewardCommand("normal").split(","));
                    normalCommandList.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", player.getName())));
                }
                case SPECIAL -> {
                    List<String> specialCommandList = Arrays.asList(getRewardCommand("special").split(","));
                    specialCommandList.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", player.getName())));
                }
                case BONUS -> {
                    List<String> normalCommandList = Arrays.asList(getRewardCommand("normal").split(","));
                    normalCommandList.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", player.getName())));
                    normalCommandList.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", player.getName())));
                }
                case WINNER -> {
                    List<String> winnerCommandList = Arrays.asList(getRewardCommand("winner").split(","));
                    winnerCommandList.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", player.getName())));
                }
                case STREAK -> {
                    List<String> streakCommandList = Arrays.asList(getRewardCommand("streak").split(","));
                    streakCommandList.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", player.getName())));
                }
                default -> throw new IllegalStateException(MsgHandler.get("system-unexpected-value") + functionName);
            }

        } catch (IOException e) {
            SLUtils.getLogger().info(String.valueOf(e));
        } catch (NullPointerException e) {
            String errorFunctionName;
            switch (functionName) {
                case NORMAL -> errorFunctionName = "”通常報酬”";
                case BONUS -> errorFunctionName = "”ボーナス報酬”";
                case SPECIAL -> errorFunctionName = "”特殊報酬”";
                case STREAK -> errorFunctionName = "”ストリーク報酬”";
                case WINNER -> errorFunctionName = "”勝利報酬";
                default -> errorFunctionName = "";
            }
            SLUtils.getLogger().info(MsgHandler.getFormatted("system-command-unset", errorFunctionName));
        }
    }
}
