package dev.felnull.shortlifeplugin;

/**
 * このプラグインで使用するパーミッション<br/>
 * パーミッションの定義はplugin.ymlで行ってください。
 *
 * @author MORIMORI0317, Quarri6343
 */
public enum SLPermissions {
    /**
     * 試合コマンド用パーミッション
     */
    COMMANDS_MATCH("shortlifeplugin.commands.match"),

    /**
     * GUIコマンド用パーミッション
     */
    COMMANDS_GUI("shortlifeplugin.commands.gui"),

    /**
     * 装備グループコマンド用パーミッション
     */
    COMMANDS_EQUIPMENT_GROUP("shortlifeplugin.commands.equipmentgroup"),

    /**
     * 試合部屋コマンド用パーミッション<br/>
     */
    COMMANDS_ROOM("shortlifeplugin.commands.room"),

    /**
     * 報酬コマンド用パーミッション<br/>
     */
    COMMANDS_REWARD("shortlifeplugin.commands.reward"),

    /**
     * テクスチャコマンド用パーミッション<br/>
     */
    COMMANDS_TEXTURE("shortlifeplugin.commands.sltexture");

    /**
     * パーミッション名
     */
    private final String name;

    SLPermissions(String name) {
        this.name = name;
    }

    /**
     * パーミッション名を取得
     *
     * @return パーミッション名
     */
    public String get() {
        return name;
    }
}
