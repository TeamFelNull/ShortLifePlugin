package dev.felnull.shortlifeplugin.match;

/**
 * 試合のカウントダウンの時間
 *
 * @author MORIMORI0317, Quarri6343
 */
public class MatchCountDownTime {

    /**
     * 前回更新された時の時間
     */
    private long lastTime;

    /**
     * 時間(1/1000秒単位)
     */
    private long time;

    /**
     * 時間を更新
     *
     * @param remnantTick 試合の残りTick数
     */
    public void update(int remnantTick) {
        lastTime = time;
        time = Math.max(remnantTick, 0) * 50L;
    }

    /**
     * 今画面に表示される秒数を更新すべきかどうかを取得<br>
     * updateで1秒を跨ぐごとに更新フラグが立つ
     *
     * @return 表示秒数を更新すべきかどうか
     */
    public boolean shouldDirtyDisplay() {
        return lastTime / 1000L != time / 1000L;
    }

    /**
     * 時間を秒単位で取得
     *
     * @return 時間
     */
    public int getSecond() {
        return (int) (time / 1000L);
    }
}
