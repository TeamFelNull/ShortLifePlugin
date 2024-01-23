package dev.felnull.shortlifeplugin.match;

import com.google.common.collect.Streams;
import dev.felnull.fnjl.util.FNMath;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 試合のカウントダウンのボスバー
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchCountDownBossBar {

    /**
     * ボスバー本体
     */
    private final BossBar countDownBossbar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    /**
     * ボスバーをプレイヤーに表示させる
     *
     * @param player プレイヤー
     */
    public void show(Player player) {
        player.showBossBar(countDownBossbar);
    }

    /**
     * ボスバーをプレイヤーから隠す
     *
     * @param player プレイヤー
     */
    public void hide(Player player) {
        player.hideBossBar(countDownBossbar);
    }

    /**
     * ボスバーの表示を全プレイヤーから消す
     */
    public void hideAll() {
        List<Player> bossbarViewers = Streams.stream(countDownBossbar.viewers())
                .filter(viewer -> viewer instanceof Player)
                .map(viewer -> (Player) viewer)
                .toList();
        Audience.audience(bossbarViewers).hideBossBar(countDownBossbar);
    }

    /**
     * ボスバーを試合状況に基づいて更新
     *
     * @param status 試合状況
     */
    public void updateCountDownStatus(MatchStatus status) {
        updateTextAndColor(status.getCountDownBossbarColor(), status.getShowName());
    }

    /**
     * ボスバーの色と表示テキストを更新
     *
     * @param color 色
     * @param text  テキスト
     */
    public void updateTextAndColor(BossBar.Color color, String text) {
        this.countDownBossbar.color(color);
        this.countDownBossbar.name(Component.text(text));
    }

    /**
     * ボスバーを進行させる
     *
     * @param compTime  経過時間
     * @param totalTime 合計時間
     */
    public void progress(int compTime, int totalTime) {
        float progress = FNMath.clamp((float) compTime / (float) totalTime, 0, 1);
        this.countDownBossbar.progress(1f - progress);
    }
}
