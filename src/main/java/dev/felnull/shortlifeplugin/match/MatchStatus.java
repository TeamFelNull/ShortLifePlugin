package dev.felnull.shortlifeplugin.match;

import dev.felnull.shortlifeplugin.MsgHandler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * 試合の状態
 *
 * @author MORIMORI0317
 */
public enum MatchStatus {

    /**
     * 開始前
     */
    NONE(Component.text(MsgHandler.get("match-status-none")), MsgHandler.get("match-status-display-none"), NamedTextColor.YELLOW, BossBar.Color.YELLOW),

    /**
     * 開始
     */
    STARTED(Component.text(MsgHandler.get("match-status-started")), MsgHandler.get("match-status-display-started"), NamedTextColor.GREEN, BossBar.Color.GREEN),

    /**
     * 終了
     */
    FINISHED(Component.text(MsgHandler.get("match-status-finished")), MsgHandler.get("match-status-display-finished"), NamedTextColor.BLUE, BossBar.Color.BLUE),

    /**
     * 破棄済み
     */
    DISCARDED(Component.text(MsgHandler.get("match-status-discarded")), MsgHandler.get("match-status-display-discarded"), NamedTextColor.RED, BossBar.Color.RED);

    /**
     * 状態名
     */
    private final Component name;

    /**
     * 表示名
     */
    private final String showName;

    /**
     * 色
     */
    private final TextColor color;

    /**
     * カウントダウン用ボスバーの色
     */
    private final BossBar.Color countDownBossbarColor;

    /**
     * コンストラクタ
     *
     * @param name                  状態名
     * @param showName              カウントダウン用ボスバーの表示名
     * @param color                 色
     * @param countDownBossbarColor カウントダウン用ボスバーの色
     */
    MatchStatus(Component name, String showName, TextColor color, BossBar.Color countDownBossbarColor) {
        this.name = name;
        this.showName = showName;
        this.color = color;
        this.countDownBossbarColor = countDownBossbarColor;
    }

    public Component getName() {
        return name;
    }

    public TextColor getColor() {
        return color;
    }

    public BossBar.Color getCountDownBossbarColor() {
        return countDownBossbarColor;
    }

    public String getShowName() {
        return showName;
    }

}
