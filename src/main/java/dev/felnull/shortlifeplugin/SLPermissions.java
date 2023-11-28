package dev.felnull.shortlifeplugin;

/**
 * このプラグインで使用するパーミッション<br/>
 * パーミッションの定義はplugin.ymlで行ってください。
 *
 * @author MORIMORI0317
 */
public final class SLPermissions {

    /**
     * 試合コマンド用パーミッション
     */
    public static final String COMMANDS_MATCH = "shortlifeplugin.commands.match";

    /**
     * GUIコマンド用パーミッション
     */
    public static final String COMMANDS_GUI = "shortlifeplugin.commands.gui";

    /**
     * 装備グループコマンド用パーミッション
     */
    public static final String COMMANDS_EQUIPMENT_GROUP = "shortlifeplugin.commands.equipmentgroup";

    /**
     * 試合部屋コマンド用パーミッション<br/>
     */
    public static final String COMMANDS_ROOM = "shortlifeplugin.commands.room";

    /**
     * 報酬コマンド用パーミッション<br/>
     */
    public static final String COMMANDS_REWARD = "hortlifeplugin.commands.reward";

    private SLPermissions() {
        throw new AssertionError();
    }
}
