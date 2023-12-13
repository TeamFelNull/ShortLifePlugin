package dev.felnull.shortlifeplugin.commands;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.MsgHandler;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroup;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import dev.felnull.shortlifeplugin.gui.EquipmentGroupModifyItemGui;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 装備グループサブコマンド
 *
 * @author MORIMORI0317, Quarri6343
 */
public enum EquipmentGroupSubCommands {

    /**
     * 追加
     */
    ADD(new CommandAPICommand("add")
            .withArguments(new StringArgument("id"))
            .withArguments(new TextArgument("name"))
            .executes(EquipmentGroupSubCommands::equipmentGroupAdd)),

    /**
     * IDを編集
     */
    MODIFY_ID(new CommandAPICommand("id")
            .withArguments(equipmentGroupArgument())
            .withArguments(new StringArgument("new id"))
            .executes(EquipmentGroupSubCommands::equipmentGroupModifyId)),

    /**
     * 名前を編集
     */
    MODIFY_NAME(new CommandAPICommand("name")
            .withArguments(equipmentGroupArgument())
            .withArguments(new TextArgument("new name"))
            .executes(EquipmentGroupSubCommands::equipmentGroupModifyName)),

    /**
     * アイテムを編集
     */
    MODIFY_ITEM(new CommandAPICommand("item")
            .withArguments(equipmentGroupArgument())
            .executes(EquipmentGroupSubCommands::equipmentGroupModifyItem)),

    /**
     * 所持制限を編集
     */
    MODIFY_RESTRICTION(new CommandAPICommand("restriction")
            .withArguments(equipmentGroupArgument())
            .withArguments(new IntegerArgument("new max hotbar exists count", -1, 9))
            .executes(EquipmentGroupSubCommands::equipmentGroupModifyRestriction)),

    /**
     * 編集サブコマンドの親
     */
    MODIFY(new CommandAPICommand("modify")
            .withSubcommands(MODIFY_ID.get(), MODIFY_NAME.get(), MODIFY_ITEM.get(), MODIFY_RESTRICTION.get())),

    /**
     * 削除
     */
    REMOVE(new CommandAPICommand("remove")
            .withArguments(equipmentGroupArgument())
            .executes(EquipmentGroupSubCommands::equipmentGroupRemove)),

    /**
     * 列挙
     */
    LIST(new CommandAPICommand("list")
            .executes((CommandExecutor) (sender, args) -> equipmentGroupList(sender))),

    /**
     * 情報
     */
    INFO(new CommandAPICommand("info")
            .withArguments(equipmentGroupArgument())
            .executes(EquipmentGroupSubCommands::equipmentGroupInfo));

    /**
     * 引数のノード名
     */
    private static final String ARG_NODENAME = "equipment group";
    
    /**
     * サブコマンド本体
     */
    private final CommandAPICommand subCommand;

    EquipmentGroupSubCommands(CommandAPICommand command) {
        this.subCommand = command;
    }

    /**
     * サブコマンドを構築する
     *
     * @return サブコマンド
     */
    public CommandAPICommand get() {
        return subCommand;
    }

    private static Argument<EquipmentGroup> equipmentGroupArgument() {
        return new CustomArgument<>(new StringArgument(ARG_NODENAME), info -> {
            EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
            return manager.getGroup(info.input()).orElseThrow(() -> CustomArgument.CustomArgumentException
                    .fromMessageBuilder(new CustomArgument.MessageBuilder(MsgHandler.get("cmd-equip-unknown")).appendArgInput()));
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> EquipmentGroupManager.getInstance().getAllGroup().keySet().toArray(String[]::new)));
    }

    private static void equipmentGroupAdd(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        String id = (String) Objects.requireNonNull(args.get("id"));
        String name = (String) Objects.requireNonNull(args.get("name"));

        if (manager.getGroup(id).isPresent()) {
            sender.sendRichMessage(MsgHandler.get("cmd-equip-id-already-exists"));
        } else {
            EquipmentGroup equipmentGroup = new EquipmentGroup(id, name, ImmutableList.of(), new EquipmentGroup.Restriction(-1));
            manager.addGroup(equipmentGroup);
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-equip-create", id));
        }
    }

    private static void equipmentGroupModifyId(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get(ARG_NODENAME));
        String newId = (String) Objects.requireNonNull(args.get("new id"));

        if (newId.equals(equipmentGroup.id())) {
            sender.sendRichMessage(MsgHandler.get("cmd-equip-id-same-with-new-id"));
        } else if (manager.getGroup(newId).isPresent()) {
            sender.sendRichMessage(MsgHandler.get("cmd-equip-new-id-already-exists"));
        } else {
            manager.removeGroup(equipmentGroup.id());
            EquipmentGroup newEquipmentGroup = new EquipmentGroup(newId, equipmentGroup.name(), equipmentGroup.itemStacks(), equipmentGroup.restriction());
            manager.addGroup(newEquipmentGroup);
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-equip-id-changed", equipmentGroup.id(), newId));
        }
    }

    private static void equipmentGroupModifyName(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get(ARG_NODENAME));
        String newName = (String) Objects.requireNonNull(args.get("new name"));

        if (newName.equals(equipmentGroup.name())) {
            sender.sendRichMessage(MsgHandler.get("cmd-equip-name-same-with-new-name"));
        } else {
            manager.removeGroup(equipmentGroup.id());
            EquipmentGroup newEquipmentGroup = new EquipmentGroup(equipmentGroup.id(), newName, equipmentGroup.itemStacks(), equipmentGroup.restriction());
            manager.addGroup(newEquipmentGroup);
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-equip-name-changed", equipmentGroup.id(), newName));
        }
    }

    private static void equipmentGroupModifyItem(CommandSender sender, CommandArguments args) {
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get(ARG_NODENAME));

        if (sender instanceof Player player) {
            EquipmentGroupModifyItemGui.tryOpenGui(player, equipmentGroup);
        } else {
            sender.sendRichMessage(MsgHandler.get("cmd-general-player-only"));
        }
    }

    private static void equipmentGroupModifyRestriction(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get(ARG_NODENAME));
        int newMaxHotbarExistsCount = (Integer) Objects.requireNonNull(args.get("new max hotbar exists count"));

        manager.removeGroup(equipmentGroup.id());
        EquipmentGroup.Restriction restriction = new EquipmentGroup.Restriction(newMaxHotbarExistsCount);
        EquipmentGroup newEquipmentGroup = new EquipmentGroup(equipmentGroup.id(), equipmentGroup.name(), equipmentGroup.itemStacks(), restriction);
        manager.addGroup(newEquipmentGroup);
        sender.sendRichMessage(MsgHandler.getFormatted("cmd-equip-restriction-changed", equipmentGroup.id()));
    }

    private static void equipmentGroupRemove(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get(ARG_NODENAME));

        manager.removeGroup(equipmentGroup.id());
        sender.sendRichMessage(MsgHandler.getFormatted("cmd-equip-delete", equipmentGroup.id()));
    }

    private static void equipmentGroupList(CommandSender sender) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        Map<String, EquipmentGroup> groups = manager.getAllGroup();

        if (groups.isEmpty()) {
            sender.sendRichMessage(MsgHandler.get("cmd-equip-empty"));
        } else {
            sender.sendRichMessage(MsgHandler.getFormatted("cmd-equip-count", groups.size()));

            groups.values().forEach(group -> sender.sendMessage(Component.text(String.format("- %s (", group.id()))
                    .append(Component.text(group.name()))
                    .append(Component.text(")"))));
        }
    }

    private static void equipmentGroupInfo(CommandSender sender, CommandArguments args) {
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get(ARG_NODENAME));

        sender.sendRichMessage(MsgHandler.getFormatted("cmd-equip-info", equipmentGroup.id()));
        List<Component> list = new LinkedList<>();
        equipmentGroup.appendInfoDesc(list);
        list.forEach(sender::sendMessage);
    }
}
