package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;

import reika.dragonapi.auxiliary.trackers.DonatorController;
import reika.dragonapi.trackers.PatreonController;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DonatorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dragondonators").executes((context) -> {
            context.getSource().sendSuccess(
                    Component.translatable("dragonapi.donatorcommand").withStyle(ChatFormatting.AQUA)
                            .append(DonatorController.instance.getDisplayList())
                            .append("\n--------------------------------------\n").withStyle(ChatFormatting.WHITE)
                            .append(PatreonController.instance.getDisplayList()),
                    false);
            return 1;
        }));
    }
}
