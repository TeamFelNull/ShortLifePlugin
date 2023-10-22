package dev.felnull.shortlifeplugin.match;

import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * サイドバー表示の管理
 *
 * @author MORIMORI0317
 */
public class SidebarDisplay {

    /**
     * 表示格納先オブジェクティブ
     */
    @NotNull
    private final Objective sidebarObjective;

    /**
     * 最後に表示されたテキスト
     */
    private final List<String> lastTexts = new LinkedList<>();

    /**
     * コンストラクタ
     *
     * @param sidebarObjective 表示格納先オブジェクティブ
     */
    public SidebarDisplay(@NotNull Objective sidebarObjective) {
        this.sidebarObjective = sidebarObjective;
    }

    /**
     * 表示内容を更新
     *
     * @param showTexts 表示するテキストのリスト
     */
    public void update(List<String> showTexts) {

        // スコアボードをリセット
        for (String lastText : this.lastTexts) {
            sidebarObjective.getScore(lastText).resetScore();
        }

        // 新しい情報表示用のスコアに点数を追加
        int ct = 0;
        for (String text : showTexts) {
            this.sidebarObjective.getScore(text).setScore(showTexts.size() - (ct++) - 1);
        }

        this.lastTexts.clear();
        this.lastTexts.addAll(showTexts);
    }
}
