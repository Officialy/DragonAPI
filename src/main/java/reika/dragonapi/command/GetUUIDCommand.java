package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;


public class GetUUIDCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("getuuid").executes((context) -> {
            context.getSource().sendSuccess(
                    Component.translatable("dragonapi.uuidcommand").withStyle(ChatFormatting.AQUA).append(context.getSource().getEntityOrException().getStringUUID()),
                    false);
            return 1;
        }));
    }

}
