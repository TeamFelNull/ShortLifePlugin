package dev.felnull.shortlifeplugin.commands;

import com.google.common.collect.ImmutableList;
import dev.felnull.shortlifeplugin.SLPermissions;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroup;
import dev.felnull.shortlifeplugin.equipmentgroup.EquipmentGroupManager;
import dev.felnull.shortlifeplugin.gui.EquipmentGroupModifyItemGui;
import dev.jorel.commandapi.CommandAPI;
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
 * 装備グループコマンド
 *
 * @author MORIMORI0317
 */
public class EquipmentGroupCommand implements SLCommand {

    @Override
    public CommandAPICommand create() {
        CommandAPICommand add = new CommandAPICommand("add")
                .withArguments(new StringArgument("id"))
                .withArguments(new TextArgument("name"))
                .executes(this::equipmentGroupAdd);

        CommandAPICommand modifyId = new CommandAPICommand("id")
                .withArguments(equipmentGroupArgument("equipment group"))
                .withArguments(new StringArgument("new id"))
                .executes(this::equipmentGroupModifyId);

        CommandAPICommand modifyName = new CommandAPICommand("name")
                .withArguments(equipmentGroupArgument("equipment group"))
                .withArguments(new TextArgument("new name"))
                .executes(this::equipmentGroupModifyName);

        CommandAPICommand modifyItem = new CommandAPICommand("item")
                .withArguments(equipmentGroupArgument("equipment group"))
                .executes(this::equipmentGroupModifyItem);

        CommandAPICommand modifyRestriction = new CommandAPICommand("restriction")
                .withArguments(equipmentGroupArgument("equipment group"))
                .withArguments(new IntegerArgument("new max hotbar exists count", -1, 9))
                .executes(this::equipmentGroupModifyRestriction);

        CommandAPICommand modify = new CommandAPICommand("modify")
                .withSubcommands(modifyId, modifyName, modifyItem, modifyRestriction);


        CommandAPICommand remove = new CommandAPICommand("remove")
                .withArguments(equipmentGroupArgument("equipment group"))
                .executes(this::equipmentGroupRemove);


        CommandAPICommand list = new CommandAPICommand("list")
                .executes((CommandExecutor) (sender, args) -> equipmentGroupList(sender));


        CommandAPICommand info = new CommandAPICommand("info")
                .withArguments(equipmentGroupArgument("equipment group"))
                .executes(this::equipmentGroupInfo);


        return new CommandAPICommand("equipmentgroup")
                .withAliases("sleg")
                .withPermission(SLPermissions.COMMANDS_EQUIPMENT_GROUP)
                .withSubcommands(add, modify, remove, list, info);
    }

    @Override
    public void unregister() {
        CommandAPI.unregister("equipmentgroup");
    }

    private Argument<EquipmentGroup> equipmentGroupArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
            return manager.getGroup(info.input()).orElseThrow(() -> CustomArgument.CustomArgumentException
                    .fromMessageBuilder(new CustomArgument.MessageBuilder("Unknown equipment group: ").appendArgInput()));
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> EquipmentGroupManager.getInstance().getAllGroup().keySet().toArray(String[]::new)));
    }

    private void equipmentGroupAdd(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        String id = (String) Objects.requireNonNull(args.get("id"));
        String name = (String) Objects.requireNonNull(args.get("name"));

        if (manager.getGroup(id) != null) {
            sender.sendRichMessage("指定されたIDの装備グループは既に存在します");
        } else {
            EquipmentGroup equipmentGroup = new EquipmentGroup(id, name, ImmutableList.of(), new EquipmentGroup.Restriction(-1));
            manager.addGroup(equipmentGroup);
            sender.sendRichMessage(String.format("新しい装備グループ(%s)を作成しました", id));
        }
    }

    private void equipmentGroupModifyId(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get("equipment group"));
        String newId = (String) Objects.requireNonNull(args.get("new id"));

        if (newId.equals(equipmentGroup.id())) {
            sender.sendRichMessage("指定された装備グループのIDと新しいIDが同じです");
        } else if (manager.getGroup(newId) != null) {
            sender.sendRichMessage("新しいIDの装備グループが既に存在しています");
        } else {
            manager.removeGroup(equipmentGroup.id());
            EquipmentGroup newEquipmentGroup = new EquipmentGroup(newId, equipmentGroup.name(), equipmentGroup.itemStacks(), equipmentGroup.restriction());
            manager.addGroup(newEquipmentGroup);
            sender.sendRichMessage(String.format("装備グループ(%s)のIDを%sに変更しました", equipmentGroup.id(), newId));
        }
    }

    private void equipmentGroupModifyName(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get("equipment group"));
        String newName = (String) Objects.requireNonNull(args.get("new name"));

        if (newName.equals(equipmentGroup.name())) {
            sender.sendRichMessage("指定された装備グループの名前と新しい名前が同じです");
        } else {
            manager.removeGroup(equipmentGroup.id());
            EquipmentGroup newEquipmentGroup = new EquipmentGroup(equipmentGroup.id(), newName, equipmentGroup.itemStacks(), equipmentGroup.restriction());
            manager.addGroup(newEquipmentGroup);
            sender.sendRichMessage(String.format("装備グループ(%s)の名前を%sに変更しました", equipmentGroup.id(), newName));
        }
    }

    private void equipmentGroupModifyItem(CommandSender sender, CommandArguments args) {
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get("equipment group"));

        if (sender instanceof Player player) {
            EquipmentGroupModifyItemGui.tryOpenGui(player, equipmentGroup);
        } else {
            sender.sendRichMessage("プレイヤーのみ実行可能です");
        }
    }

    private void equipmentGroupModifyRestriction(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get("equipment group"));
        int newMaxHotbarExistsCount = (Integer) Objects.requireNonNull(args.get("new max hotbar exists count"));

        manager.removeGroup(equipmentGroup.id());
        EquipmentGroup.Restriction restriction = new EquipmentGroup.Restriction(newMaxHotbarExistsCount);
        EquipmentGroup newEquipmentGroup = new EquipmentGroup(equipmentGroup.id(), equipmentGroup.name(), equipmentGroup.itemStacks(), restriction);
        manager.addGroup(newEquipmentGroup);
        sender.sendRichMessage(String.format("装備グループ(%s)の装備制限を変更しました", equipmentGroup.id()));
    }

    private void equipmentGroupRemove(CommandSender sender, CommandArguments args) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get("equipment group"));

        manager.removeGroup(equipmentGroup.id());
        sender.sendRichMessage(String.format("指定された装備グループ(%s)を削除しました", equipmentGroup.id()));
    }

    private void equipmentGroupList(CommandSender sender) {
        EquipmentGroupManager manager = EquipmentGroupManager.getInstance();
        Map<String, EquipmentGroup> groups = manager.getAllGroup();

        if (groups.isEmpty()) {
            sender.sendRichMessage("装備グループは存在しません");
        } else {
            sender.sendRichMessage(String.format("%d個の装備グループが存在します", groups.size()));

            groups.values().forEach(group -> sender.sendMessage(Component.text(String.format("- %s (", group.id()))
                    .append(Component.text(group.name()))
                    .append(Component.text(")"))));
        }
    }

    private void equipmentGroupInfo(CommandSender sender, CommandArguments args) {
        EquipmentGroup equipmentGroup = (EquipmentGroup) Objects.requireNonNull(args.get("equipment group"));

        sender.sendRichMessage(String.format("%sの装備グループ情報:", equipmentGroup.id()));
        List<Component> list = new LinkedList<>();
        equipmentGroup.appendInfoDesc(list);
        list.forEach(sender::sendMessage);
    }
}
